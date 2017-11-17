package com.github.yeriomin.crashes

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id

@Entity
data class CrashReport (
        @Id
        val time: Long = 0,
        val directoryName: String = "",
        val versionCode: Int = 0,
        val source: String = "",
        val topic: String = "",
        val deviceName: String = "",
        val userId: String = "",
        @Column(length=4096)
        val message: String = "",
        @Column(length=16384)
        var stackTrace: String = "",
        val hasStackTrace: Boolean = false,
        val hasDeviceDefinition: Boolean = false,
        val hasLog: Boolean = false,
        val hasPreferences: Boolean = false
)
