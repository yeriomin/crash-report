package com.github.yeriomin.crashes

import org.springframework.data.repository.PagingAndSortingRepository

interface CrashReportRepository : PagingAndSortingRepository<CrashReport, Long>