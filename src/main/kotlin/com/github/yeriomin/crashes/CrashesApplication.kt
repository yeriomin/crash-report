package com.github.yeriomin.crashes

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class CrashesApplication

fun main(args: Array<String>) {
    SpringApplication.run(CrashesApplication::class.java, *args)
}
