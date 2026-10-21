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
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.concurrent.thread

class PowerButtonUserService : IPowerButtonUserService.Stub() {

    private val lock = Any()

    @Volatile
    private var listener: IPowerButtonListener? = null

    private var devices: List<EvdevDevice> = emptyList()
    private var detector: PowerPressDetector? = null
    private var grabbed = false
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
            val target = listener ?: return
            try {
                target.onDoublePress()
            } catch (e: RemoteException) {
                Log.d(TAG, "Listener is gone", e)
                listener = null
            }
        }
    }

    override fun destroy() {
        setGrabbed(false)
        System.exit(0)
    }

    override fun start(listener: IPowerButtonListener?, multiPressTimeoutMs: Int): Boolean {
        synchronized(lock) {
            this.listener = listener
            detector?.let {
                it.multiPressTimeoutMs = multiPressTimeoutMs.toLong()
                return true
            }
            devices = EvdevDevice.openPowerKeyDevices()
            if (devices.isEmpty()) return false
            detector = PowerPressDetector(multiPressTimeoutMs.toLong(), output)
        }
        thread(name = "power-button-reader") { readLoop() }
        return true
    }

    override fun setGrabbed(grabbed: Boolean): Boolean = synchronized(lock) {
        grabPending = grabbed && !this.grabbed && devices.any { it.isKeyDown(EvdevDevice.KEY_POWER) }
        if (grabPending) return true
        applyGrab(grabbed)
    }

    private fun applyGrab(grabbed: Boolean): Boolean {
        if (this.grabbed == grabbed) return true
        releaseHeldKeys()
        val applied = devices.isNotEmpty() && devices.all { it.grab(grabbed) }
        if (grabbed && !applied) devices.forEach { it.grab(false) }
        this.grabbed = grabbed && applied
        return applied
    }

    private fun releaseHeldKeys() {
        detector?.reset()
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
                setGrabbed(false)
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
            detector?.onTimeout(SystemClock.uptimeMillis())
        }
    }

    private fun pollTimeoutMs(): Int = synchronized(lock) {
        val deadline = detector?.deadline ?: PowerPressDetector.NO_DEADLINE
        if (deadline == PowerPressDetector.NO_DEADLINE) {
            -1
        } else {
            (deadline - SystemClock.uptimeMillis()).coerceAtLeast(0).toInt()
        }
    }

    private fun drain(device: EvdevDevice, buffer: ByteArray) {
        val read = Os.read(device.fileDescriptor, buffer, 0, buffer.size)
        val events = ByteBuffer.wrap(buffer, 0, read).order(ByteOrder.nativeOrder())
        for (offset in 0 until read - EVENT_SIZE + 1 step EVENT_SIZE) {
            val type = events.getShort(offset + TIME_SIZE).toInt()
            val code = events.getShort(offset + TIME_SIZE + 2).toInt()
            val value = events.getInt(offset + TIME_SIZE + 4)
            if (type == EvdevDevice.EV_KEY) handleKey(code, value)
        }
    }

    private fun handleKey(code: Int, value: Int) {
        if (grabbed) {
            onKey(code, value)
        } else if (grabPending && code == EvdevDevice.KEY_POWER && value == KEY_UP) {
            grabPending = false
            applyGrab(true)
        }
    }

    private fun onKey(code: Int, value: Int) {
        val detector = detector ?: return
        val now = SystemClock.uptimeMillis()
        when {
            code == EvdevDevice.KEY_POWER && value == KEY_DOWN -> detector.onPowerDown(now)
            code == EvdevDevice.KEY_POWER && value == KEY_UP -> detector.onPowerUp(now)
            code != EvdevDevice.KEY_POWER -> forward(code, value, now)
        }
    }

    private fun forward(code: Int, value: Int, now: Long) {
        val keyCode = EvdevDevice.forwardedKeys[code] ?: return
        val previous = forwardedDowns[code]
        when (value) {
            KEY_DOWN -> {
                detector?.onOtherKeyDown()
                val down = KeyEvent(
                    now, now, KeyEvent.ACTION_DOWN, keyCode, 0, 0,
                    KeyCharacterMap.VIRTUAL_KEYBOARD, 0, 0, InputDevice.SOURCE_KEYBOARD
                )
                forwardedDowns[code] = down
                inject(down)
            }
            KEY_REPEAT -> previous?.let {
                val repeat = KeyEvent.changeTimeRepeat(it, now, it.repeatCount + 1)
                forwardedDowns[code] = repeat
                inject(repeat)
            }
            KEY_UP -> previous?.let {
                forwardedDowns.remove(code)
                detector?.onOtherKeyUp()
                inject(KeyEvent.changeTimeRepeat(KeyEvent.changeAction(it, KeyEvent.ACTION_UP), now, 0))
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
        const val KEY_REPEAT = 2
        const val EVENTS_PER_READ = 64
        const val REPLAY_GAP_MS = 60L
        val TIME_SIZE = if (Process.is64Bit()) 16 else 8
        val EVENT_SIZE = TIME_SIZE + 8
    }
}
