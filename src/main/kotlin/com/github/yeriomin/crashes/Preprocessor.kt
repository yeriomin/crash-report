package com.github.yeriomin.crashes

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.file.*
import java.nio.file.StandardWatchEventKinds.ENTRY_CREATE
import java.nio.file.StandardWatchEventKinds.OVERFLOW
import java.util.logging.Logger
import kotlin.concurrent.thread
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.Files
import java.util.*

@Component
class Preprocessor : ApplicationListener<ApplicationReadyEvent> {

    @Value("\${crashesdirectory}")
    private val directory: String? = null

    @Autowired
    var repository: CrashReportRepository? = null

    override fun onApplicationEvent(event: ApplicationReadyEvent) {
        watch()
        collect()
    }

    fun watch() {
        Logger.getLogger(javaClass.name).info("Watching " + directory)
        val watcher = FileSystems.getDefault().newWatchService()
        try {
            Paths.get(directory).register(watcher, ENTRY_CREATE)
            thread(start = true) {
                var previous = ""
                while (true) {
                    var key: WatchKey
                    try {
                        key = watcher.take()
                    } catch (x: InterruptedException) {
                        break
                    }
                    for (event in key.pollEvents()) {
                        if (event.kind() === OVERFLOW) {
                            continue
                        }
                        val ev = event as WatchEvent<Path>
                        val newDir = File(directory, ev.context().toFile().toString())
                        if (newDir.absolutePath == previous) {
                            continue
                        }
                        previous = newDir.absolutePath
                        thread(start = true) {
                            Thread.sleep(6000)
                            if (eligible(newDir)) {
                                save(newDir)
                            }
                        }
                    }
                    if (!key.reset()) {
                        Logger.getLogger(javaClass.name).warning(directory + " can no longer be watched")
                        break
                    }
                }
            }
        } catch (e: IOException) {
            Logger.getLogger(javaClass.name).warning("Could not init watcher: " + e.message)
        }
    }

    fun collect() {
        Logger.getLogger(javaClass.name).info("Collecting crash reports from " + directory)
        File(directory)
                .walkTopDown()
                .maxDepth(1)
                .filterIndexed { _, file -> eligible(file) }
                .forEach {
                    save(it)
                }
    }

    fun eligible(file: File): Boolean {
        return file.exists() && file.isDirectory && file.name.split("-").size > 8
    }

    fun save(directory: File) {
        Logger.getLogger(javaClass.name).info("Inserting " + directory.name)
        try {
            repository!!.save(getCrashReport(directory))
        } catch (e: Exception) {
            Logger.getLogger(javaClass.name).warning("Could not insert " + directory.name + " : " + e.message)
        }
    }

    fun getCrashReport(directory: File): CrashReport {
        val explodedName = directory.name.split("-")
        var versionCode = explodedName[7].split(".")[1].toInt()
        var deviceName = explodedName.last()
        var source = if (versionCode > 20) explodedName[explodedName.size - 3] else "fdroid"
        val stackTraceFile = File(directory, "stacktrace.txt")
        val hasStackTrace = stackTraceFile.exists() && stackTraceFile.length() > 0
        var topic = "feedback"
        val messageFile = File(directory, "message.txt")
        var userId = ""
        var message = ""
        if (messageFile.exists() && messageFile.length() > 0) {
            val messageLines = messageFile.readLines(Charsets.UTF_8)
            val secondaryVersionCode = getVersionCode(messageLines)
            if (secondaryVersionCode > 29) {
                versionCode = secondaryVersionCode
                val properties = getProperties(messageFile)
                userId = properties.getProperty("userId", userId)
                message = properties.getProperty("message", message)
                deviceName = properties.getProperty("deviceName", deviceName)
                source = properties.getProperty("source", source)
                topic = properties.getProperty("topic", topic)
            } else {
                userId = messageLines[0].substring(5).trim().replace("null", "").replace("'", "''")
                message = messageLines.drop(2).joinToString("\n").substring(8).trim().replace("null", "").replace("'", "''")
            }
        } else if (versionCode > 20 && source.length == 3) {
            source = "unknown"
        }
        if (versionCode < 30) {
            if (hasStackTrace) {
                topic = "crash"
            } else if (message.contentEquals("Sent from device definition request dialog")) {
                topic = "device"
            }
        }
        val deviceDefinitionFile = File(directory, "device-$deviceName.properties")
        val hasDeviceDefinition = deviceDefinitionFile.exists() && deviceDefinitionFile.length() > 0
        var stackTrace = ""
        if (stackTraceFile.exists()) {
            stackTrace = getStackTraceBrief(stackTraceFile.readLines(Charsets.UTF_8))
        }
        val logFile = File(directory, "log.txt")
        val hasLog = logFile.exists() && logFile.length() > 0
        val preferencesFile = File(directory, "preferences.txt")
        val hasPreferences = preferencesFile.exists() && preferencesFile.length() > 0
        return CrashReport(Files.readAttributes(directory.toPath(), BasicFileAttributes::class.java).creationTime().toMillis() / 1000, directory.name, versionCode, source, topic, deviceName, userId, message, stackTrace, hasStackTrace, hasDeviceDefinition, hasLog, hasPreferences)
    }

    fun getVersionCode(messageLines: List<String>): Int {
        val marker ="versionCode = "
        return messageLines
                .firstOrNull { it.startsWith(marker) }
                ?.let { it.substring(marker.length).toInt() }
                ?: 29
    }

    fun getProperties(file: File): Properties {
        val properties = Properties()
        try {
            properties.load(FileInputStream(file))
        } catch (e: IOException) {
            Logger.getLogger(javaClass.name).warning("Could not read message file: " + e.message)
        }
        return properties
    }

    fun getStackTraceBrief(stackTraceLines: List<String>): String {
        val packageName = "com.github.yeriomin.yalpstore"
        var chosenCause = ""
        for (line in stackTraceLines) {
            val lineTrimmed = line.trim()
            if (lineTrimmed.startsWith("at")) {
                if (lineTrimmed.contains(packageName)) {
                    return chosenCause + " " + lineTrimmed.replace(packageName + ".", "")
                }
            } else {
                val marker = "Caused by: "
                chosenCause = if (lineTrimmed.startsWith(marker)) lineTrimmed.substring(marker.length) else lineTrimmed
            }
        }
        return ""
    }
}