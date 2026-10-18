package com.audioshelf.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

final class Chapter {
  final String title;
  final long startMs;
  long endMs = -1;

  Chapter(String title, long startMs) {
    this.title = title == null ? "" : title.trim();
    this.startMs = startMs;
  }

  static List<Chapter> tidy(List<Chapter> raw) {
    List<Chapter> list = new ArrayList<>(raw);
    Collections.sort(list, Comparator.comparingLong(c -> c.startMs));
    List<Chapter> out = new ArrayList<>();
    for (Chapter c : list) if (out.isEmpty() || c.startMs > out.get(out.size() - 1).startMs) out.add(c);
    if (out.size() < 2) return new ArrayList<>();
    List<Chapter> fixed = new ArrayList<>();
    for (int i = 0; i < out.size(); i++) {
      Chapter c = new Chapter(out.get(i).title.isEmpty() ? "" + (i + 1) : out.get(i).title, i == 0 ? 0 : out.get(i).startMs);
      c.endMs = i + 1 < out.size() ? out.get(i + 1).startMs : -1;
      fixed.add(c);
    }
    return fixed;
  }
}
