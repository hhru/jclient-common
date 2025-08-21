package ru.hh.jclient.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class SimpleRangeTest {

  @Test
  public void testSingletonRange() {
    SimpleRange simpleRange = SimpleRange.singleton(200);
    assertEquals(new SimpleRange(200, 200), simpleRange);
    assertTrue(simpleRange.contains(200));
    assertFalse(simpleRange.contains(201));
    assertFalse(simpleRange.contains(199));
  }

  @Test
  public void testRange() {
    SimpleRange range = new SimpleRange(190, 200);
    assertEquals(new SimpleRange(190, 200), range);

    assertTrue(range.contains(200));
    assertTrue(range.contains(190));
    assertTrue(range.contains(195));
    assertFalse(range.contains(189));
    assertFalse(range.contains(201));
  }

  @Test
  public void testIsConnected() {
    SimpleRange range = new SimpleRange(190, 200);
    assertTrue(range.isConnected(new SimpleRange(191, 192)));
    assertTrue(range.isConnected(new SimpleRange(189, 192)));
    assertTrue(range.isConnected(new SimpleRange(199, 201)));
    assertFalse(range.isConnected(new SimpleRange(180, 189)));
    assertFalse(range.isConnected(new SimpleRange(201, 210)));
  }
}
