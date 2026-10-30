package com.arslan.customanimator.service

import android.hardware.input.InputManager
import android.os.Build
import android.os.Process
import android.os.RemoteException
import android.os.SystemClock
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import android.system.StructPollfd
import android.util.Log
import android.view.InputDevice
import android.view.InputEvent
import android.view.KeyCharacterMap
import android.view.KeyEvent
import com.arslan.customanimator.utils.ButtonTrigger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.concurrent.thread

class PowerButtonUserService : IPowerButtonUserService.Stub() {

    private val lock = Any()

    @Volatile
    private var listener: IPowerButtonListener? = null

    private var devices: List<EvdevDevice> = emptyList()
    private var detector: PowerPressDetector? = null
    private val volumeDetector = VolumeKeyDetector(object : VolumeKeyDetector.Output {
        override fun pressKey(code: Int) {
            val keyCode = EvdevDevice.forwardedKeys[code] ?: return
            val now = SystemClock.uptimeMillis()
            val down = KeyEvent(
                now, now, KeyEvent.ACTION_DOWN, keyCode, 0, 0,
                KeyCharacterMap.VIRTUAL_KEYBOARD, 0, 0, InputDevice.SOURCE_KEYBOARD
            )
            forwardedDowns[code] = down
            inject(down)
        }

        override fun releaseKey(code: Int) {
            val down = forwardedDowns.remove(code) ?: return
            inject(KeyEvent.changeTimeRepeat(KeyEvent.changeAction(down, KeyEvent.ACTION_UP), SystemClock.uptimeMillis(), 0))
        }

        override fun repeatKey(code: Int) {
            val down = forwardedDowns[code] ?: return
            val repeat = KeyEvent.changeTimeRepeat(down, SystemClock.uptimeMillis(), down.repeatCount + 1)
            forwardedDowns[code] = repeat
            inject(repeat)
        }

        override fun trigger(trigger: ButtonTrigger) {
            notifyListener { it.onVolumeTrigger(trigger.name) }
        }
    })
    private val grabbedDevices = HashSet<EvdevDevice>()
    private var interactive = false
    private var volumeScreenOff = false
    private var powerDoublePress = false
    private var grabPending = false
    private var holdDownTime = 0L
    private val forwardedDowns = HashMap<Int, KeyEvent>()

    private val inputManager: Any by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Class.forName("android.hardware.input.InputManagerGlobal").getMethod("getInstance").invoke(null)
        } else {
            InputManager::class.java.getMethod("getInstance").invoke(null)
        } ?: error("input manager unavailable")
    }

    private val injectMethod by lazy {
        inputManager.javaClass.getMethod(
            "injectInputEvent",
            InputEvent::class.java,
            Int::class.javaPrimitiveType
        )
    }

    private val output = object : PowerPressDetector.Output {
        override fun replayPresses(count: Int) {
            repeat(count) { index ->
                if (index > 0) SystemClock.sleep(REPLAY_GAP_MS)
                val downTime = SystemClock.uptimeMillis()
                inject(powerEvent(KeyEvent.ACTION_DOWN, downTime))
                inject(powerEvent(KeyEvent.ACTION_UP, downTime))
            }
        }

        override fun holdDown() {
            holdDownTime = SystemClock.uptimeMillis()
            inject(powerEvent(KeyEvent.ACTION_DOWN, holdDownTime))
        }

        override fun holdUp() {
            inject(powerEvent(KeyEvent.ACTION_UP, holdDownTime))
        }

        override fun doublePress() {
            notifyListener { it.onDoublePress() }
        }
    }

    private fun notifyListener(call: (IPowerButtonListener) -> Unit) {
        val target = listener ?: return
        try {
            call(target)
        } catch (e: RemoteException) {
            Log.d(TAG, "Listener is gone", e)
            listener = null
        }
    }

    override fun destroy() {
        synchronized(lock) { releaseAll() }
        System.exit(0)
    }

    override fun start(
        listener: IPowerButtonListener?,
        doublePressWindowMs: Int,
        powerDoublePress: Boolean,
        volumeTriggers: Array<String>?,
        volumeScreenOff: Boolean
    ): Boolean {
        val multiPressTimeoutMs = if (powerDoublePress) doublePressWindowMs.toLong() else PowerPressDetector.PASS_THROUGH
        synchronized(lock) {
            this.listener = listener
            this.volumeScreenOff = volumeScreenOff
            this.powerDoublePress = powerDoublePress
            volumeDetector.triggers = volumeTriggers.orEmpty().mapNotNull(ButtonTrigger::fromName).toSet()
            volumeDetector.doublePressWindowMs = doublePressWindowMs.toLong()
            detector?.let {
                it.multiPressTimeoutMs = multiPressTimeoutMs
                applyGrabs()
                return true
            }
            devices = EvdevDevice.openButtonDevices()
            if (devices.isEmpty()) return false
            detector = PowerPressDetector(multiPressTimeoutMs, output)
        }
        thread(name = "power-button-reader") { readLoop() }
        return true
    }

    override fun setGrabbed(interactive: Boolean): Boolean = synchronized(lock) {
        this.interactive = interactive
        grabPending = interactive && devices.any {
            it.hasPower && it !in grabbedDevices && it.isKeyDown(EvdevDevice.KEY_POWER)
        }
        applyGrabs()
    }

    private fun wantsGrab(device: EvdevDevice): Boolean = when {
        device.hasPower -> (interactive || powerDoublePress) && !grabPending
        volumeDetector.triggers.isEmpty() -> false
        else -> interactive || volumeScreenOff
    }

    private fun applyGrabs(): Boolean = devices.map { applyGrab(it, wantsGrab(it)) }.all { it }

    private fun applyGrab(device: EvdevDevice, grab: Boolean): Boolean {
        if ((device in grabbedDevices) == grab) return true
        releaseHeldKeys()
        if (!grab) {
            grabbedDevices -= device
            return device.grab(false)
        }
        val applied = device.grab(true)
        if (applied) grabbedDevices += device
        return applied
    }

    private fun releaseAll() {
        devices.forEach { applyGrab(it, false) }
    }

    private fun releaseHeldKeys() {
        detector?.reset()
        volumeDetector.reset()
        forwardedDowns.values.forEach { inject(KeyEvent.changeAction(it, KeyEvent.ACTION_UP)) }
        forwardedDowns.clear()
    }

    private fun readLoop() {
        val pollFds = devices.map { device ->
            StructPollfd().apply {
                fd = device.fileDescriptor
                events = OsConstants.POLLIN.toShort()
            }
        }.toTypedArray()
        val buffer = ByteArray(EVENT_SIZE * EVENTS_PER_READ)
        try {
            while (true) pollOnce(pollFds, buffer)
        } catch (e: ErrnoException) {
            Log.e(TAG, "Reading the power button failed", e)
        } finally {
            synchronized(lock) {
                releaseAll()
                devices.forEach { it.close() }
                devices = emptyList()
            }
        }
    }

    private fun pollOnce(pollFds: Array<StructPollfd>, buffer: ByteArray) {
        try {
            Os.poll(pollFds, pollTimeoutMs())
        } catch (e: ErrnoException) {
            if (e.errno != OsConstants.EINTR) throw e
        }
        synchronized(lock) {
            pollFds.forEachIndexed { index, pollFd ->
                if (pollFd.revents.toInt() and OsConstants.POLLIN != 0) drain(devices[index], buffer)
            }
            val now = SystemClock.elapsedRealtime()
            detector?.onTimeout(now)
            volumeDetector.onTimeout(now)
        }
    }

    private fun pollTimeoutMs(): Int = synchronized(lock) {
        val deadline = listOfNotNull(detector?.deadline, volumeDetector.deadline)
            .filter { it != PowerPressDetector.NO_DEADLINE }
            .minOrNull()
        if (deadline == null) {
            -1
        } else {
            (deadline - SystemClock.elapsedRealtime()).coerceAtLeast(0).toInt()
        }
    }

    private fun drain(device: EvdevDevice, buffer: ByteArray) {
        val read = Os.read(device.fileDescriptor, buffer, 0, buffer.size)
        val events = ByteBuffer.wrap(buffer, 0, read).order(ByteOrder.nativeOrder())
        for (offset in 0 until read - EVENT_SIZE + 1 step EVENT_SIZE) {
            val type = events.getShort(offset + TIME_SIZE).toInt()
            val code = events.getShort(offset + TIME_SIZE + 2).toInt()
            val value = events.getInt(offset + TIME_SIZE + 4)
            if (type == EvdevDevice.EV_KEY) handleKey(device, code, value)
        }
    }

    private fun handleKey(device: EvdevDevice, code: Int, value: Int) {
        if (device in grabbedDevices) {
            onKey(code, value)
        } else if (grabPending && code == EvdevDevice.KEY_POWER && value == KEY_UP) {
            grabPending = false
            applyGrabs()
        }
    }

    private fun onKey(code: Int, value: Int) {
        val detector = detector ?: return
        val now = SystemClock.elapsedRealtime()
        if (code == EvdevDevice.KEY_POWER) notifyListener { it.onPowerKey() }
        when {
            code == EvdevDevice.KEY_POWER && value == KEY_DOWN -> {
                volumeDetector.onPowerDown(now)
                detector.onPowerDown(now)
            }
            code == EvdevDevice.KEY_POWER && value == KEY_UP -> {
                volumeDetector.onPowerUp()
                detector.onPowerUp(now)
            }
            code != EvdevDevice.KEY_POWER -> forward(code, value, now)
        }
    }

    private fun forward(code: Int, value: Int, now: Long) {
        if (code !in EvdevDevice.forwardedKeys) return
        when (value) {
            KEY_DOWN -> {
                detector?.onOtherKeyDown()
                volumeDetector.onDown(code, now)
            }
            KEY_UP -> {
                detector?.onOtherKeyUp()
                volumeDetector.onUp(code, now)
            }
        }
    }

    private fun powerEvent(action: Int, downTime: Long): KeyEvent =
        KeyEvent(
            downTime, SystemClock.uptimeMillis(), action, KeyEvent.KEYCODE_POWER, 0, 0,
            KeyCharacterMap.VIRTUAL_KEYBOARD, 0, 0, InputDevice.SOURCE_KEYBOARD
        )

    private fun inject(event: KeyEvent) {
        try {
            injectMethod.invoke(inputManager, event, INJECT_ASYNC)
        } catch (e: Throwable) {
            Log.e(TAG, "Injecting ${KeyEvent.keyCodeToString(event.keyCode)} failed", e)
        }
    }

    private companion object {
        const val TAG = "PowerButtonUserService"
        const val INJECT_ASYNC = 0
        const val KEY_UP = 0
        const val KEY_DOWN = 1
        const val EVENTS_PER_READ = 64
        const val REPLAY_GAP_MS = 60L
        val TIME_SIZE = if (Process.is64Bit()) 16 else 8
        val EVENT_SIZE = TIME_SIZE + 8
    }
}
