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
import com.audioshelf.app.library.BookRow
import com.audioshelf.app.library.ProgressState
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
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun StatsScreen(store: StatsStore, rows: List<BookRow>, titleOf: (String) -> String, onClose: () -> Unit) {
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
            if (report.empty) {
                AText(stringResource(R.string.stats_empty), 15f, c.muted, Modifier.fillMaxWidth().padding(top = 24.dp), align = TextAlign.Center)
            } else {
                Core(report)
                Books(rows)
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
                Section(stringResource(R.string.stats_by_time)) { BarChart(report) }
                Section(stringResource(R.string.stats_habits)) { Habits(report) }
                Section(stringResource(R.string.stats_calendar)) { Heatmap(report) }
                Section(stringResource(R.string.stats_wrapped)) { Wrapped(report, rows, titleOf) }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AText(title, 17f, AppTheme.colors.text, weight = FontWeight.Bold)
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

/** The numbers worth showing off: totals and the streak. Always for all time / month / week, whatever range is picked below. */
@Composable
private fun Core(r: StatsReport) {
    val c = AppTheme.colors
    val shape = RoundedCornerShape(14.dp)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(
            Modifier.fillMaxWidth().clip(shape).background(c.surface).border(1.dp, c.line, shape).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            AText(stringResource(R.string.stats_all_time), 13f, c.muted)
            AText(duration(r.allTimeMs), 30f, c.text, weight = FontWeight.Bold, maxLines = 1, tabular = true)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Card(stringResource(R.string.stats_core_month), duration(r.monthMs), Modifier.weight(1f))
            Card(stringResource(R.string.stats_core_week), duration(r.weekMs), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Card(stringResource(R.string.stats_streak), stringResource(R.string.stats_days, r.streak), Modifier.weight(1f))
            Card(stringResource(R.string.stats_best_streak), stringResource(R.string.stats_days, r.bestStreak), Modifier.weight(1f))
        }
    }
}

/** Finished books and the ones in progress with their percent. */
@Composable
private fun Books(rows: List<BookRow>) {
    val c = AppTheme.colors
    val done = rows.count { it.state == ProgressState.Done }
    val going = rows.filter { it.state == ProgressState.Started }.sortedByDescending { it.lastPlayed }
    Section(stringResource(R.string.stats_books)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Card(stringResource(R.string.stats_done), done.toString(), Modifier.weight(1f))
            Card(stringResource(R.string.stats_progress), going.size.toString(), Modifier.weight(1f))
        }
        if (going.isEmpty()) {
            AText(stringResource(R.string.stats_progress_none), 14f, c.muted)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                going.take(PROGRESS_ROWS).forEach { row ->
                    val pc = row.percent ?: 0
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AText(row.title, 14f, c.text, Modifier.weight(1f), maxLines = 1)
                            AText("$pc%", 13f, c.muted, tabular = true)
                        }
                        Bar(pc / 100f)
                    }
                }
            }
        }
    }
}

private const val PROGRESS_ROWS = 6

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
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            AText(duration(r.listenedMs), 15f, c.text, weight = FontWeight.SemiBold)
            AText(stringResource(R.string.stats_avg_day) + ": " + duration(r.avgPerDayMs), 12f, c.muted)
        }
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

private enum class DayPart(val label: Int, val hours: IntRange) {
    Morning(R.string.stats_part_morning, 5..10),
    Day(R.string.stats_part_day, 11..16),
    Evening(R.string.stats_part_evening, 17..22),
    Night(R.string.stats_part_night, 0..0),
}

private fun partOf(hour: Int): DayPart = when (hour) {
    in 5..10 -> DayPart.Morning
    in 11..16 -> DayPart.Day
    in 17..22 -> DayPart.Evening
    else -> DayPart.Night
}

/** When you listen, how long a sitting lasts, and which weekday is yours. */
@Composable
private fun Habits(r: StatsReport) {
    val c = AppTheme.colors
    val locale = LocalConfiguration.current.locales[0]
    val byPart = DayPart.entries.associateWith { part -> (0 until 24).filter { partOf(it) == part }.sumOf { r.hours[it] } }
    val total = byPart.values.sum().coerceAtLeast(1L)
    val favorite = byPart.maxByOrNull { it.value }
    val bestDay = r.weekdays.indices.maxByOrNull { r.weekdays[it] }?.takeIf { r.weekdays[it] > 0 }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Card(
                stringResource(R.string.stats_fav_time),
                if (favorite != null && favorite.value > 0) "${stringResource(favorite.key.label)} · ${100 * favorite.value / total}%" else "—",
                Modifier.weight(1f),
            )
            Card(
                stringResource(R.string.stats_best_weekday),
                bestDay?.let { weekdayName(it, locale, long = true) } ?: "—",
                Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Card(stringResource(R.string.stats_avg_session), duration(r.avgSessionMs), Modifier.weight(1f))
            Card(stringResource(R.string.stats_longest_session), duration(r.longestSessionMs), Modifier.weight(1f))
        }
        AText(stringResource(R.string.stats_hours), 13f, c.muted, weight = FontWeight.SemiBold)
        Hours(r.hours)
        AText(stringResource(R.string.stats_weekdays), 13f, c.muted, weight = FontWeight.SemiBold)
        Weekdays(r.weekdays, locale)
    }
}

private fun weekdayName(index: Int, locale: Locale, long: Boolean): String {
    val c = Calendar.getInstance()
    val days = intArrayOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY)
    c.set(Calendar.DAY_OF_WEEK, days[index])
    return SimpleDateFormat(if (long) "EEEE" else "EE", locale).format(c.time).replaceFirstChar { it.uppercase() }
}

@Composable
private fun Weekdays(values: LongArray, locale: Locale) {
    val c = AppTheme.colors
    val max = (values.maxOrNull() ?: 0L).coerceAtLeast(1L)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Canvas(Modifier.fillMaxWidth().height(80.dp)) {
            val slot = size.width / 7f
            val bar = slot * 0.6f
            for (d in 0 until 7) {
                val v = values[d]
                val height = if (v <= 0) 2f else (size.height * v / max).coerceAtLeast(3f)
                drawRoundRect(
                    color = if (v > 0) c.accent else c.line,
                    topLeft = Offset(d * slot + (slot - bar) / 2f, size.height - height),
                    size = Size(bar, height),
                    cornerRadius = CornerRadius(3f, 3f),
                )
            }
        }
        Row(Modifier.fillMaxWidth()) {
            for (d in 0 until 7) AText(weekdayName(d, locale, long = false), 11f, c.muted, Modifier.weight(1f), align = TextAlign.Center, maxLines = 1)
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

/** The period summary: time, best weekday, top three books and authors. */
@Composable
private fun Wrapped(r: StatsReport, rows: List<BookRow>, titleOf: (String) -> String) {
    val c = AppTheme.colors
    val locale = LocalConfiguration.current.locales[0]
    val authors = HashMap<String, Long>()
    for ((id, ms) in r.perBook) {
        val author = rows.firstOrNull { it.book.id == id }?.author.orEmpty()
        if (author.isNotEmpty()) authors[author] = (authors[author] ?: 0L) + ms
    }
    val topAuthors = authors.entries.sortedByDescending { it.value }.take(3)
    val bestDay = r.weekdays.indices.maxByOrNull { r.weekdays[it] }?.takeIf { r.weekdays[it] > 0 }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Card(stringResource(R.string.stats_total_time), duration(r.listenedMs), Modifier.weight(1f))
            Card(
                stringResource(R.string.stats_best_weekday),
                bestDay?.let { weekdayName(it, locale, long = true) } ?: "—",
                Modifier.weight(1f),
            )
        }
        if (r.top.isNotEmpty()) {
            AText(stringResource(R.string.stats_top), 13f, c.muted, weight = FontWeight.SemiBold)
            Ranking(r.top.take(3).map { titleOf(it.id) to it.ms })
        }
        if (topAuthors.isNotEmpty()) {
            AText(stringResource(R.string.stats_top_authors), 13f, c.muted, weight = FontWeight.SemiBold)
            Ranking(topAuthors.map { it.key to it.value })
        }
    }
}

@Composable
private fun Ranking(items: List<Pair<String, Long>>) {
    val c = AppTheme.colors
    val max = (items.firstOrNull()?.second ?: 1L).coerceAtLeast(1L)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEachIndexed { i, (name, ms) ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AText("${i + 1}", 14f, c.accent, weight = FontWeight.Bold, tabular = true)
                    AText(name, 14f, c.text, Modifier.weight(1f), maxLines = 1)
                    AText(duration(ms), 13f, c.muted, tabular = true)
                }
                Bar(ms.toFloat() / max)
            }
        }
    }
}
