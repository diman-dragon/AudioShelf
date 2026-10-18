package com.audioshelf.app;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

final class MkaChapters {
  private static final long CHAPTERS = 0x1043A770L;
  private static final long EDITION = 0x45B9L;
  private static final long ATOM = 0xB6L;
  private static final long START = 0x91L;
  private static final long HIDDEN = 0x98L;
  private static final long ENABLED = 0x4598L;
  private static final long DISPLAY = 0x80L;
  private static final long STRING = 0x85L;

  private MkaChapters() {}

  static List<Chapter> parse(ByteSource src, Ebml.Element chapters) throws IOException {
    long end = chapters.end(src.length());
    for (long p = chapters.dataPos; p < end; ) {
      Ebml.Element e = Ebml.at(src, p);
      if (e == null || e.size == Ebml.UNKNOWN) break;
      if (e.id == EDITION) {
        List<Chapter> list = new ArrayList<>();
        atoms(src, e.dataPos, e.end(end), list);
        List<Chapter> tidy = Chapter.tidy(list);
        if (!tidy.isEmpty()) return tidy;
      }
      p = e.end(end);
    }
    return new ArrayList<>();
  }

  private static boolean atoms(ByteSource src, long from, long to, List<Chapter> out) throws IOException {
    boolean any = false;
    for (long p = from; p < to; ) {
      Ebml.Element e = Ebml.at(src, p);
      if (e == null || e.size == Ebml.UNKNOWN) break;
      if (e.id == ATOM) {
        any = true;
        atom(src, e, out);
      }
      p = e.end(to);
    }
    return any;
  }

  private static void atom(ByteSource src, Ebml.Element atom, List<Chapter> out) throws IOException {
    long start = -1;
    String title = "";
    boolean shown = true;
    boolean leaf = true;
    long end = atom.end(src.length());
    for (long p = atom.dataPos; p < end; ) {
      Ebml.Element e = Ebml.at(src, p);
      if (e == null || e.size == Ebml.UNKNOWN) break;
      if (e.id == START) start = Ebml.uint(src, e) / 1_000_000L;
      else if (e.id == HIDDEN) shown &= Ebml.uint(src, e) == 0;
      else if (e.id == ENABLED) shown &= Ebml.uint(src, e) != 0;
      else if (e.id == DISPLAY && title.isEmpty()) title = display(src, e);
      else if (e.id == ATOM) {
        leaf = false;
        atom(src, e, out);
      }
      p = e.end(end);
    }
    if (leaf && shown && start >= 0) out.add(new Chapter(title, start));
  }

  private static String display(ByteSource src, Ebml.Element display) throws IOException {
    long end = display.end(src.length());
    for (long p = display.dataPos; p < end; ) {
      Ebml.Element e = Ebml.at(src, p);
      if (e == null || e.size == Ebml.UNKNOWN) break;
      if (e.id == STRING) return Ebml.text(src, e);
      p = e.end(end);
    }
    return "";
  }

  static long chaptersId() {
    return CHAPTERS;
  }
}
