package ru.hh.jclient.common.util.jersey;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

class UrlUtils {
  private static final int RADIX = 16;
  private static final byte ESCAPE_CHAR = '%';
  private static final byte PLUS_CHAR = '+';

  private UrlUtils() {

  }

  static String urlEncode(String value, String enc) {

    try {
      value = URLEncoder.encode(value, enc);
    }
    catch (UnsupportedEncodingException ex) {
      throw new RuntimeException(ex);
    }

    return value;
  }

  /**
   * Decodes using URLDecoder - use when queries or form post values are decoded
   *
   * @param value
   *          value to decode
   * @param enc
   *          encoding
   */
  static String urlDecode(String value, String enc) {
    return urlDecode(value, enc, false);
  }

  private static String urlDecode(String value, String enc, boolean isPath) {

    boolean needDecode = false;
    int escapesCount = 0;
    int i = 0;
    while (i < value.length()) {
      char ch = value.charAt(i++);
      if (ch == ESCAPE_CHAR) {
        escapesCount += 1;
        i += 2;
        needDecode = true;
      }
      else if (!isPath && ch == PLUS_CHAR) {
        needDecode = true;
      }
    }
    if (needDecode) {
      final byte[] valueBytes = StringUtils.toBytes(value, enc);
      ByteBuffer in = ByteBuffer.wrap(valueBytes);
      ByteBuffer out = ByteBuffer.allocate(in.capacity() - 2 * escapesCount);
      while (in.hasRemaining()) {
        final int b = in.get();
        if (!isPath && b == PLUS_CHAR) {
          out.put((byte) ' ');
        }
        else if (b == ESCAPE_CHAR) {
          try {
            final int u = digit16(in.get());
            final int l = digit16(in.get());
            out.put((byte) ((u << 4) + l));
          }
          catch (final ArrayIndexOutOfBoundsException e) {
            throw new RuntimeException("Invalid URL encoding: ", e);
          }
        }
        else {
          out.put((byte) b);
        }
      }
      out.flip();
      return Charset.forName(enc).decode(out).toString();
    }
    else {
      return value;
    }
  }

  private static int digit16(final byte b) {
    final int i = Character.digit((char) b, RADIX);
    if (i == -1) {
      throw new RuntimeException("Invalid URL encoding: not a valid digit (radix " + RADIX + "): " + b);
    }
    return i;
  }

  static String urlDecode(String value) {
    return urlDecode(value, "UTF-8");
  }

  /**
   * URL path segments may contain '+' symbols which should not be decoded into ' ' This method replaces '+' with %2B and delegates to URLDecoder
   *
   * @param value
   *          value to decode
   */
  static String pathDecode(String value) {
    return urlDecode(value, "UTF-8", true);
  }
}
