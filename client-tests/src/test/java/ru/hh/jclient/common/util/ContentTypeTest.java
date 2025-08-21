package ru.hh.jclient.common.util;

import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import static ru.hh.jclient.common.util.ContentType.ANY;
import static ru.hh.jclient.common.util.ContentType.APPLICATION_JSON;
import static ru.hh.jclient.common.util.ContentType.TEXT_ANY;
import static ru.hh.jclient.common.util.ContentType.TEXT_PLAIN;
import static ru.hh.jclient.common.util.ContentType.withCharset;

public class ContentTypeTest {

  @Test
  public void testWildcard() {
    assertTrue(new ContentType(TEXT_ANY).allows(new ContentType(TEXT_PLAIN)));
    assertFalse(new ContentType(TEXT_PLAIN).allows(new ContentType(TEXT_ANY)));

    assertTrue(new ContentType(ANY).allows(new ContentType(TEXT_ANY)));
    assertFalse(new ContentType(TEXT_ANY).allows(new ContentType(ANY)));

    assertFalse(new ContentType(TEXT_ANY).allows(new ContentType(APPLICATION_JSON)));
    assertFalse(new ContentType(APPLICATION_JSON).allows(new ContentType(TEXT_ANY)));
  }

  @Test
  public void testCharset() {
    assertTrue(new ContentType(withCharset(TEXT_PLAIN, "utf-8")).allows(new ContentType(withCharset(TEXT_PLAIN, "utf-8"))));
    assertTrue(new ContentType(withCharset(TEXT_PLAIN, StandardCharsets.UTF_8)).allows(new ContentType(withCharset(TEXT_PLAIN, "utf-8"))));
    assertTrue(new ContentType(withCharset(TEXT_PLAIN, "UTF-8")).allows(new ContentType(withCharset(TEXT_PLAIN, "utf-8"))));
    assertFalse(new ContentType(withCharset(TEXT_PLAIN, "utf-8")).allows(new ContentType(withCharset(TEXT_PLAIN, "cp1251"))));

    assertTrue(new ContentType(TEXT_PLAIN).allows(new ContentType(withCharset(TEXT_PLAIN, "utf-8"))));
    assertFalse(new ContentType(withCharset(TEXT_PLAIN, "utf-8")).allows(new ContentType(TEXT_PLAIN)));

    assertTrue(new ContentType(TEXT_ANY).allows(new ContentType(withCharset(TEXT_PLAIN, "utf-8"))));
    assertFalse(new ContentType(withCharset(TEXT_PLAIN, "utf-8")).allows(new ContentType(TEXT_ANY)));
  }

  @Test
  public void testNull() {
    assertThrows(IllegalArgumentException.class, () -> new ContentType(null));
  }

  @Test
  public void testIncorrect() {
    assertThrows(IllegalArgumentException.class, () -> new ContentType("zxcsad"));
  }
}
