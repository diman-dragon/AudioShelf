package com.audioshelf.app;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Chapters (and, for Matroska, a cover) found inside one audio file, whatever its container. */
final class FileChapters {
  final List<Chapter> chapters;
  final byte[] cover;

  private FileChapters(List<Chapter> chapters, byte[] cover) {
    this.chapters = chapters;
    this.cover = cover;
  }

  static FileChapters read(ByteSource src, boolean wantCover) throws IOException {
    if (MkaFile.matches(src)) {
      MkaFile f = MkaFile.read(src, wantCover);
      return new FileChapters(f.chapters, f.cover);
    }
    if (Mp4Chapters.matches(src)) return new FileChapters(Mp4Chapters.read(src), null);
    if (Id3Chapters.matches(src)) return new FileChapters(Id3Chapters.read(src), null);
    return new FileChapters(new ArrayList<>(), null);
  }
}
