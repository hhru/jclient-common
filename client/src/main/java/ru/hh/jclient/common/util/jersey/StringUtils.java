package ru.hh.jclient.common.util.jersey;

import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class StringUtils {
  public static final Map<String, Pattern> PATTERN_MAP = new HashMap<String, Pattern>();

  static {
    String patterns[] = { "/", " ", ":", ",", ";", "=", "\\.", "\\+" };
    for (String p : patterns) {
      PATTERN_MAP.put(p, Pattern.compile(p));
    }
  }

  private StringUtils() {
  }

  static String[] split(String s, String regex) {
    Pattern p = PATTERN_MAP.get(regex);
    if (p != null) {
      return p.split(s);
    }
    return s.split(regex);
  }

  static boolean isEmpty(String str) {
    if (str != null) {
      int len = str.length();
      for (int x = 0; x < len; ++x) {
        if (str.charAt(x) > ' ') {
          return false;
        }
      }
    }
    return true;
  }

  static byte[] toBytes(String str, String enc) {
    try {
      return str.getBytes(Charset.forName(enc));
    }
    catch (UnsupportedCharsetException ex) {
      throw new RuntimeException(ex);
    }
  }
}
