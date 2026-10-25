package com.audioshelf.app.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.audioshelf.app.R
import com.audioshelf.app.stats.BarPoint
import com.audioshelf.app.stats.StatsRange
import com.audioshelf.app.stats.StatsReport
import com.audioshelf.app.stats.StatsStore
import com.audioshelf.app.ui.components.AText
import com.audioshelf.app.ui.components.SegItem
import com.audioshelf.app.ui.components.Segmented
import com.audioshelf.app.ui.components.tap
import com.audioshelf.app.ui.theme.AppTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StatsScreen(store: StatsStore, titleOf: (String) -> String, onClose: () -> Unit) {
    val c = AppTheme.colors
    val version by store.version.collectAsState()
    var range by remember { mutableStateOf(StatsRange.Week) }
    val report = remember(range, version) {
        StatsReport.build(store.days(), store.books(), store.dayBooks(), range, System.currentTimeMillis())
    }
    Column(Modifier.fillMaxSize().background(c.bg).systemBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            AText(
                "←", 22f, c.text,
                Modifier.tap(description = stringResource(R.string.marks_close), onClick = onClose).padding(horizontal = 12.dp, vertical = 8.dp),
            )
            AText(stringResource(R.string.stats_title), 18f, c.text, weight = FontWeight.Bold)
        }
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Segmented(
                listOf(
                    SegItem(StatsRange.Week.id, stringResource(R.string.stats_week)),
                    SegItem(StatsRange.Month.id, stringResource(R.string.stats_month)),
                    SegItem(StatsRange.Year.id, stringResource(R.string.stats_year)),
                    SegItem(StatsRange.All.id, stringResource(R.string.stats_all)),
                ),
                range.id,
                { range = StatsRange.of(it) },
                fill = true,
            )
            if (report.empty) {
                AText(stringResource(R.string.stats_empty), 15f, c.muted, Modifier.fillMaxWidth().padding(top = 24.dp), align = TextAlign.Center)
            } else {
                Summary(report)
                Section(stringResource(R.string.stats_by_time)) { BarChart(report) }
                Section(stringResource(R.string.stats_calendar)) { Heatmap(report) }
                Section(stringResource(R.string.stats_hours)) { Hours(report.hours) }
                if (report.speeds.isNotEmpty()) Section(stringResource(R.string.stats_speeds)) { Speeds(report) }
                if (report.top.isNotEmpty()) Section(stringResource(R.string.stats_top)) { Top(report, titleOf) }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AText(title, 15f, AppTheme.colors.text, weight = FontWeight.Bold)
        content()
    }
}

@Composable
fun duration(ms: Long): String {
    val minutes = (ms.coerceAtLeast(0) / 60_000L).toInt()
    val h = minutes / 60
    val m = minutes % 60
    return if (h > 0) stringResource(R.string.stats_hm, h, m) else stringResource(R.string.stats_m, m)
}

@Composable
private fun Summary(r: StatsReport) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Card(stringResource(R.string.stats_listened), duration(r.listenedMs), Modifier.weight(1f))
            Card(stringResource(R.string.stats_avg_day), duration(r.avgPerDayMs), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Card(stringResource(R.string.stats_streak), stringResource(R.string.stats_days, r.streak), Modifier.weight(1f))
            Card(stringResource(R.string.stats_best_streak), stringResource(R.string.stats_days, r.bestStreak), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Card(stringResource(R.string.stats_finished), r.finishedBooks.toString(), Modifier.weight(1f))
            Card(stringResource(R.string.stats_started), r.startedBooks.toString(), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Card(stringResource(R.string.stats_sessions), r.sessions.toString(), Modifier.weight(1f))
            Card(stringResource(R.string.stats_avg_active), duration(r.avgActiveMs), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Card(stringResource(R.string.stats_saved), duration(r.savedMs), Modifier.weight(1f))
            Card(stringResource(R.string.stats_all_time), duration(r.allTimeMs), Modifier.weight(1f))
        }
    }
}

@Composable
private fun Card(label: String, value: String, modifier: Modifier = Modifier) {
    val c = AppTheme.colors
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier.clip(shape).background(c.surface).border(1.dp, c.line, shape).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        AText(label, 12f, c.muted, maxLines = 2)
        AText(value, 20f, c.text, weight = FontWeight.Bold, maxLines = 1, tabular = true)
    }
}

private fun label(point: BarPoint, monthly: Boolean, locale: Locale, weekly: Boolean): String {
    val pattern = when {
        monthly -> "MMM"
        weekly -> "EE"
        else -> "d"
    }
    return SimpleDateFormat(pattern, locale).format(Date(point.start))
}

@Composable
private fun BarChart(r: StatsReport) {
    val c = AppTheme.colors
    val locale = LocalConfiguration.current.locales[0]
    val max = (r.bars.maxOfOrNull { it.ms } ?: 0L).coerceAtLeast(1L)
    val weekly = r.range == StatsRange.Week
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        AText(duration(max), 12f, c.muted)
        Canvas(Modifier.fillMaxWidth().height(130.dp)) {
            val n = r.bars.size.coerceAtLeast(1)
            val slot = size.width / n
            val bar = slot * 0.7f
            r.bars.forEachIndexed { i, p ->
                val h = if (p.ms <= 0) 2f else (size.height * p.ms / max).coerceAtLeast(3f)
                drawRoundRect(
                    color = if (p.ms > 0) c.accent else c.line,
                    topLeft = Offset(i * slot + (slot - bar) / 2f, size.height - h),
                    size = Size(bar, h),
                    cornerRadius = CornerRadius(3f, 3f),
                )
            }
        }
        if (r.bars.size <= 12) {
            Row(Modifier.fillMaxWidth()) {
                r.bars.forEach { p ->
                    AText(label(p, r.monthly, locale, weekly), 11f, c.muted, Modifier.weight(1f), align = TextAlign.Center, maxLines = 1)
                }
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                AText(SimpleDateFormat("d MMM", locale).format(Date(r.bars.first().start)), 11f, c.muted)
                AText(SimpleDateFormat("d MMM", locale).format(Date(r.bars.last().start)), 11f, c.muted)
            }
        }
    }
}

@Composable
private fun Heatmap(r: StatsReport) {
    val c = AppTheme.colors
    val max = (r.heat.maxOfOrNull { it.ms } ?: 0L).coerceAtLeast(1L)
    Canvas(Modifier.fillMaxWidth().height(130.dp)) {
        val weeks = r.heat.size / 7
        val gap = 4f
        val cell = minOf((size.width - gap * (weeks - 1)) / weeks, (size.height - gap * 6) / 7)
        val left = (size.width - (cell * weeks + gap * (weeks - 1))) / 2f
        r.heat.forEachIndexed { i, d ->
            val col = i / 7
            val row = i % 7
            if (d.ms < 0) return@forEachIndexed
            val level = if (d.ms < 60_000L) 0f else (0.25f + 0.75f * d.ms / max).coerceAtMost(1f)
            drawRoundRect(
                color = if (level == 0f) c.line else c.accent.copy(alpha = level),
                topLeft = Offset(left + col * (cell + gap), row * (cell + gap)),
                size = Size(cell, cell),
                cornerRadius = CornerRadius(3f, 3f),
            )
        }
    }
}

@Composable
private fun Hours(hours: LongArray) {
    val c = AppTheme.colors
    val max = (hours.maxOrNull() ?: 0L).coerceAtLeast(1L)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Canvas(Modifier.fillMaxWidth().height(80.dp)) {
            val slot = size.width / 24f
            val bar = slot * 0.7f
            for (h in 0 until 24) {
                val v = hours[h]
                val height = if (v <= 0) 2f else (size.height * v / max).coerceAtLeast(3f)
                drawRoundRect(
                    color = if (v > 0) c.accent else c.line,
                    topLeft = Offset(h * slot + (slot - bar) / 2f, size.height - height),
                    size = Size(bar, height),
                    cornerRadius = CornerRadius(2f, 2f),
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("0", "6", "12", "18", "24").forEach { AText(it, 11f, c.muted) }
        }
    }
}

@Composable
private fun Bar(fraction: Float) {
    val c = AppTheme.colors
    Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(c.line)) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(fraction.coerceIn(0.02f, 1f)).background(c.accent))
    }
}

@Composable
private fun Speeds(r: StatsReport) {
    val c = AppTheme.colors
    val total = r.speeds.sumOf { it.ms }.coerceAtLeast(1L)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        r.speeds.forEach { s ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AText(String.format(Locale.ROOT, "%.2f×", s.speedKey / 100f), 14f, c.text, Modifier.weight(1f), tabular = true)
                    AText("${(100 * s.ms / total)}% · ${duration(s.ms)}", 13f, c.muted, tabular = true)
                }
                Bar(s.ms.toFloat() / total)
            }
        }
    }
}

@Composable
private fun Top(r: StatsReport, titleOf: (String) -> String) {
    val c = AppTheme.colors
    val max = (r.top.firstOrNull()?.ms ?: 1L).coerceAtLeast(1L)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        r.top.forEach { b ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AText(titleOf(b.id), 14f, c.text, Modifier.weight(1f), maxLines = 1)
                    AText(duration(b.ms), 13f, c.muted, tabular = true)
                }
                Bar(b.ms.toFloat() / max)
            }
        }
    }
}
