package com.audioshelf.app.ui.player

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import com.audioshelf.app.library.CoverSource
import com.audioshelf.app.library.LibFile
import com.audioshelf.app.library.NoCovers
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.audioshelf.app.R
import com.audioshelf.app.player.BookProgress
import com.audioshelf.app.player.Kind
import com.audioshelf.app.player.Placeholder
import com.audioshelf.app.player.PlayerActions
import com.audioshelf.app.player.PlayerUi
import com.audioshelf.app.player.fmtTime
import com.audioshelf.app.ui.components.AText
import com.audioshelf.app.ui.components.EmptyState
import com.audioshelf.app.ui.theme.AppTheme
import com.audioshelf.app.ui.theme.Dimens

private val COVER_MAX = 340.dp
private val COVER_MIN = 64.dp
private val BODY_MAX = 420.dp

@Composable
fun PlayerScreen(ui: PlayerUi, actions: PlayerActions, covers: CoverSource = NoCovers, modifier: Modifier = Modifier) {
    if (!ui.loaded) {
        EmptyState(
            emoji = R.drawable.emoji_headphones,
            title = stringResource(R.string.player_empty),
            hint = stringResource(R.string.player_empty_hint),
            modifier = modifier,
            action = stringResource(R.string.player_to_library),
            onAction = actions::onToLibrary,
        )
        return
    }

    val c = AppTheme.colors
    val heading = heading(ui)
    Box(modifier.fillMaxSize().padding(Dimens.screenPadding)) {
        Column(
            Modifier.widthIn(max = BODY_MAX).fillMaxHeight().align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Cover(ui.placeholder, ui.coverFile, covers, Modifier.weight(1f).fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            AText(heading.title, 18f, c.text, Modifier.fillMaxWidth(), FontWeight.Bold, TextAlign.Center)
            if (heading.sub.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                AText(heading.sub, 14f, c.muted, Modifier.fillMaxWidth(), align = TextAlign.Center)
            }
            if (heading.label.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                AText(heading.label, 14f, c.muted, Modifier.fillMaxWidth(), align = TextAlign.Center)
            }
            Spacer(Modifier.height(12.dp))
            SeekBar(ui.positionMs, ui.durationMs, actions::onSeek)
            ui.progress?.let {
                Spacer(Modifier.height(5.dp))
                BookLine(it)
            }
            Spacer(Modifier.height(12.dp))
            Controls(ui, actions)
            Spacer(Modifier.height(12.dp))
            ExtrasRow(ui, actions)
        }
    }
}

private class Heading(val title: String, val sub: String, val label: String)

@Composable
private fun heading(ui: PlayerUi): Heading {
    val ch = ui.chapter
    if (ui.multi && ui.kind == Kind.Album) {
        return Heading(
            title = ch.name,
            sub = listOf(ui.title, ui.author).filter { it.isNotEmpty() }.joinToString(" · "),
            label = stringResource(R.string.player_track_of, ch.index + 1, ch.count),
        )
    }
    val label = if (ui.multi) {
        "${stringResource(R.string.player_chapter_of, ch.index + 1, ch.count)} · ${ch.name}"
    } else ""
    return Heading(ui.title, ui.author, label)
}

@Composable
private fun Cover(placeholder: Placeholder, file: LibFile?, covers: CoverSource, modifier: Modifier = Modifier) {
    val c = AppTheme.colors
    val bitmap by produceState(file?.let { covers.peek(it, true) }, file?.uri, file?.modified, file?.size) {
        value = file?.let { covers.load(it, true) }
    }
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val side: Dp = minOf(maxWidth, maxHeight, COVER_MAX).coerceAtLeast(COVER_MIN)
        val shape = RoundedCornerShape(Dimens.radius)
        Box(
            Modifier
                .size(side)
                .clip(shape)
                .background(c.surface)
                .border(1.dp, c.line, shape)
                .padding(1.dp),
        ) {
            Image(
                painter = painterResource(
                    when (placeholder) {
                        Placeholder.ClosedBook -> R.drawable.emoji_book_closed
                        Placeholder.OpenBook -> R.drawable.emoji_book_open
                        Placeholder.Disc -> R.drawable.emoji_disc
                    },
                ),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding((side - 2.dp) * 0.125f),
            )
            bitmap?.let { b ->
                val image = remember(b) { b.asImageBitmap() }
                Image(
                    bitmap = image,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun BookLine(progress: BookProgress) {
    val c = AppTheme.colors
    val text = stringResource(
        if (progress.exact) R.string.player_book_left else R.string.player_book_left_approx,
        progress.percent,
        fmtTime(progress.leftMs),
    )
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(c.line)) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress.percent.coerceIn(0, 100) / 100f)
                    .background(c.accent),
            )
        }
        Spacer(Modifier.height(4.dp))
        AText(text, 12f, c.muted, Modifier.fillMaxWidth(), align = TextAlign.Center)
    }
}
