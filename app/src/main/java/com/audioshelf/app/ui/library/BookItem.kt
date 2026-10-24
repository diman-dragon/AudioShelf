package com.audioshelf.app.ui.library

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.audioshelf.app.R
import com.audioshelf.app.library.BookRow
import com.audioshelf.app.library.CoverSource
import com.audioshelf.app.library.SizeUnits
import com.audioshelf.app.library.fmtDate
import com.audioshelf.app.library.fmtSize
import com.audioshelf.app.library.placeholderOf
import com.audioshelf.app.library.splitName
import com.audioshelf.app.player.Kind
import com.audioshelf.app.ui.components.AText
import com.audioshelf.app.ui.components.drawable
import com.audioshelf.app.ui.components.tap
import com.audioshelf.app.ui.theme.AppTheme
import com.audioshelf.app.ui.theme.Dimens

@Composable
private fun infoLine(row: BookRow): String {
    val locale = LocalConfiguration.current.locales[0]
    val units = SizeUnits(stringResource(R.string.unit_kb), stringResource(R.string.unit_mb), stringResource(R.string.unit_gb))
    val kind = stringResource(if (row.kind == Kind.Book) R.string.kind_book else R.string.kind_album)
    val what = if (row.book.folder) {
        stringResource(R.string.info_files, row.book.files.size)
    } else {
        splitName(row.book.files[0].name).second.removePrefix(".").uppercase()
    }
    return listOf(kind, row.year, what, fmtSize(row.book.size, units, locale), fmtDate(row.book.modified, locale))
        .filter { it.isNotEmpty() }
        .joinToString(" · ")
}

@Composable
private fun bylineOf(row: BookRow): String {
    val narrated = if (row.narrator.isNotEmpty()) stringResource(R.string.info_narrator, row.narrator) else ""
    return listOf(row.author, narrated).filter { it.isNotEmpty() }.joinToString(" · ")
}

@Composable
private fun Thumb(row: BookRow, covers: CoverSource, modifier: Modifier) {
    val c = AppTheme.colors
    val file = row.book.files[0]
    val cover by produceState(covers.peek(file), file.uri, file.modified, file.size) { value = covers.load(file) }
    val fade by animateFloatAsState(if (cover != null) 1f else 0f, tween(200), label = "cover")
    val (placeholder, flip) = placeholderOf(row.kind, row.state)
    Box(modifier.clip(RoundedCornerShape(10.dp)).background(c.bg), contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(placeholder.drawable()),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(0.75f).graphicsLayer { scaleX = if (flip) -1f else 1f },
        )
        cover?.let { bitmap ->
            val image = remember(bitmap) { bitmap.asImageBitmap() }
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().alpha(fade),
            )
        }
    }
}

@Composable
private fun Meta(row: BookRow, modifier: Modifier) {
    val c = AppTheme.colors
    val byline = bylineOf(row)
    Column(modifier) {
        AText(row.title, 15f, c.text, weight = FontWeight.SemiBold)
        if (byline.isNotEmpty()) AText(byline, 13f, c.text.copy(alpha = 0.8f), Modifier.padding(top = 2.dp))
        AText(infoLine(row), 12f, c.muted, Modifier.padding(top = 2.dp))
        row.percent?.let { pc ->
            Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(c.line)) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(pc / 100f).background(c.accent))
                }
                AText("$pc%", 11f, c.muted, Modifier.widthIn(min = 30.dp), align = TextAlign.End, tabular = true)
            }
        }
    }
}

@Composable
private fun More(onMenu: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.tap(description = stringResource(R.string.lib_menu), onClick = onMenu),
        contentAlignment = Alignment.Center,
    ) {
        AText("⋮", 20f, AppTheme.colors.muted)
    }
}

@Composable
fun ListBookItem(row: BookRow, covers: CoverSource, onOpen: () -> Unit, onMenu: () -> Unit) {
    val c = AppTheme.colors
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().tap(onClick = onOpen).padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Thumb(row, covers, Modifier.size(56.dp))
            Meta(row, Modifier.weight(1f))
            More(onMenu, Modifier.size(36.dp))
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
    }
}

@Composable
fun CardBookItem(row: BookRow, covers: CoverSource, onOpen: () -> Unit, onMenu: () -> Unit) {
    val c = AppTheme.colors
    val shape = RoundedCornerShape(Dimens.radius)
    Box(Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(c.surface)
                .border(1.dp, c.line, shape)
                .tap(onClick = onOpen)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Thumb(row, covers, Modifier.fillMaxWidth().aspectRatio(1f))
            Meta(row, Modifier.fillMaxWidth())
        }
        More(
            onMenu,
            Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
                .size(30.dp)
                .clip(CircleShape)
                .background(c.bg.copy(alpha = 0.7f)),
        )
    }
}
