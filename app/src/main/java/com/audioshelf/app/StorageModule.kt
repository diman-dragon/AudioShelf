package com.audioshelf.app

import android.app.Application
import com.audioshelf.app.data.FolderStore
import com.audioshelf.app.data.HiddenStore
import com.audioshelf.app.data.Kv
import com.audioshelf.app.data.Prefs
import com.audioshelf.app.data.SqliteKv
import com.audioshelf.app.library.Background
import com.audioshelf.app.library.DurationStore
import com.audioshelf.app.library.MetaStore
import com.audioshelf.app.library.NoteStore
import com.audioshelf.app.library.ProgressStore
import com.audioshelf.app.library.QueueStore
import com.audioshelf.app.player.ChapterStore
import com.audioshelf.app.player.MarkStore
import com.audioshelf.app.player.PlayPrefs
import com.audioshelf.app.sound.EqStore
import com.audioshelf.app.sound.FxStore
import com.audioshelf.app.sound.SoundPrefs
import com.audioshelf.app.stats.StatsStore
import kotlinx.coroutines.CoroutineScope

/** Everything that is stored: the database and the stores on top of it. */
class StorageModule(app: Application, scope: CoroutineScope, background: Background) {
    val kv = Kv(SqliteKv(app), scope, background.writer, background::calm)
    val prefs = Prefs(kv)
    val folders = FolderStore(kv)
    val meta = MetaStore(kv)
    val hidden = HiddenStore(kv)
    val progress = ProgressStore(kv)
    val durations = DurationStore(kv)
    val playPrefs = PlayPrefs(kv)
    val chapters = ChapterStore(kv)
    val marks = MarkStore(kv)
    val eqStore = EqStore(kv)
    val fxStore = FxStore(kv)
    val stats = StatsStore(kv)
    val queue = QueueStore(kv)
    val notes = NoteStore(kv)
    val soundPrefs = SoundPrefs(kv)
}
