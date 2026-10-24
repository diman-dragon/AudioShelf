package com.audioshelf.app.library

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val MB = 1048576L
private const val GB = 1073741824L

class SizeUnits(val kb: String, val mb: String, val gb: String)

private fun num(value: Double, locale: Locale): String =
    NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 1 }.format(value)

fun fmtSize(bytes: Long, units: SizeUnits, locale: Locale): String = when {
    bytes < MB -> "${maxOf(1L, Math.round(bytes / 1024.0))} ${units.kb}"
    bytes < GB -> "${num(bytes.toDouble() / MB, locale)} ${units.mb}"
    else -> "${num(bytes.toDouble() / GB, locale)} ${units.gb}"
}

fun fmtDate(ms: Long, locale: Locale): String {
    val pattern = if (locale.language == "ru") "dd.MM.yyyy" else "MM/dd/yyyy"
    return SimpleDateFormat(pattern, locale).format(Date(ms))
}
