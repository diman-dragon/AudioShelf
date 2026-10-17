package com.audioshelf.app;

import android.content.Context;
import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.BooleanSupplier;

/** Decodes the start of an audio file and measures how loud it is. */
final class LoudnessMeter {
  private static final long WINDOW_US = 5L * 60 * 1000000; // five minutes are enough, and it spares the battery
  private static final long WAIT_US = 10000;

  private LoudnessMeter() {}

  /** Gain in dB for this file; throws if it cannot be decoded. */
  static double gainFor(Context ctx, Uri uri, BooleanSupplier stop) throws IOException {
    MediaExtractor extractor = new MediaExtractor();
    MediaCodec codec = null;
    try {
      extractor.setDataSource(ctx, uri, null);
      MediaFormat format = ExtractorAudio.select(extractor);
      if (format == null) throw new IOException("no audio track");
      codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME));
      codec.configure(format, null, null, 0);
      codec.start();
      long start = ExtractorAudio.windowStartUs(format, WINDOW_US);
      if (start > 0) extractor.seekTo(start, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
      return decode(extractor, codec, format, start + WINDOW_US, stop);
    } catch (IllegalStateException | IllegalArgumentException e) {
      throw new IOException(e);
    } finally {
      if (codec != null) {
        try {
          codec.stop();
        } catch (Exception ignored) {
          // already stopped
        }
        codec.release();
      }
      extractor.release();
    }
  }

  private static double decode(MediaExtractor ex, MediaCodec codec, MediaFormat in, long endUs, BooleanSupplier stop) {
    MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
    LoudnessStats stats = new LoudnessStats(ExtractorAudio.rate(in), ExtractorAudio.channels(in));
    boolean isFloat = false;
    boolean inDone = false;
    boolean outDone = false;
    while (!outDone && !stop.getAsBoolean()) {
      if (!inDone) {
        int i = codec.dequeueInputBuffer(WAIT_US);
        if (i >= 0) {
          ByteBuffer buf = codec.getInputBuffer(i);
          int size = buf == null ? -1 : ex.readSampleData(buf, 0);
          long time = ex.getSampleTime();
          if (size < 0 || time > endUs) {
            codec.queueInputBuffer(i, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            inDone = true;
          } else {
            codec.queueInputBuffer(i, 0, size, time, 0);
            ex.advance();
          }
        }
      }
      int o = codec.dequeueOutputBuffer(info, WAIT_US);
      if (o == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
        MediaFormat out = codec.getOutputFormat();
        isFloat =
            out.containsKey(MediaFormat.KEY_PCM_ENCODING)
                && out.getInteger(MediaFormat.KEY_PCM_ENCODING) == AudioFormat.ENCODING_PCM_FLOAT;
      } else if (o >= 0) {
        ByteBuffer buf = codec.getOutputBuffer(o);
        if (buf != null && info.size > 0) {
          buf.position(info.offset).limit(info.offset + info.size);
          stats.add(buf, isFloat);
        }
        codec.releaseOutputBuffer(o, false);
        outDone = (info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
      }
    }
    return stats.gainDb();
  }
}
