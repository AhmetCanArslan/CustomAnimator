package com.arslan.customanimator.utils

import java.io.File

object ShellApkInstaller {
    private const val APK_EXTENSION = ".apk"
    private const val SHELL_SUCCESS = "Success"
    private val SESSION_ID = Regex("\\[(\\d+)]")

    class StagedApk(val path: String, val size: Long)

    class InstallResult(val success: Boolean, val output: String)

    fun stage(files: List<File>, directory: String): List<StagedApk>? {
        if (!ShizukuHelper.executeShellCommand(arrayOf("mkdir", "-p", directory))) return null
        return files.mapIndexed { index, file ->
            val target = "$directory/$index$APK_EXTENSION"
            if (!ShizukuHelper.executeShellCommand(arrayOf("cp", file.absolutePath, target))) return null
            StagedApk(target, file.length())
        }
    }

    fun install(apks: List<StagedApk>, flags: List<String> = emptyList()): InstallResult {
        val created = ShizukuHelper.executeShellCommandWithOutput(arrayOf("pm", "install-create", "-r") + flags)
        val session = SESSION_ID.find(created.output)?.groupValues?.get(1)
        if (!created.isSuccess || session == null) return InstallResult(false, created.output)
        apks.forEachIndexed { index, apk ->
            val written = run(
                arrayOf("pm", "install-write", "-S", apk.size.toString(), session, "$index$APK_EXTENSION", apk.path)
            )
            if (!written.success) {
                ShizukuHelper.executeShellCommand(arrayOf("pm", "install-abandon", session))
                return written
            }
        }
        return run(arrayOf("pm", "install-commit", session))
    }

    fun run(command: Array<String>): InstallResult {
        val result = ShizukuHelper.executeShellCommandWithOutput(command)
        return InstallResult(result.isSuccess && result.output.contains(SHELL_SUCCESS), result.output)
    }
}
