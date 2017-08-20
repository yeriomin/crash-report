package com.github.yeriomin.crashes

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.boot.autoconfigure.http.HttpMessageConverters
import org.springframework.context.annotation.Bean
import org.springframework.web.servlet.mvc.method.annotation.AbstractJsonpResponseBodyAdvice
import org.springframework.web.bind.annotation.ControllerAdvice
import java.util.*


@RestController
class CrashReportController (val repository:CrashReportRepository) {

    @ControllerAdvice
    internal class JsonpAdvice : AbstractJsonpResponseBodyAdvice("callback")

    @Bean
    fun customConverters(): HttpMessageConverters {
        return HttpMessageConverters(false,  Collections.singleton(MappingJackson2HttpMessageConverter()) as Collection<HttpMessageConverter<*>>)
    }

    @GetMapping("/crashreport")
    fun get(
            @RequestParam(value = "page", defaultValue = "0") page: Int,
            @RequestParam(value = "pagesize", defaultValue = "20") pageSize: Int,
            @RequestParam(value = "sortcol", defaultValue = "time") sortColumn: String,
            @RequestParam(value = "sortdir", defaultValue = "asc") sortDirection: String
    ) = repository.findAll(PageRequest(page, pageSize, Sort((if (sortDirection == "asc") Sort.Direction.ASC else Sort.Direction.DESC), sortColumn)))
}
