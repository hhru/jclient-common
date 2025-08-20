package ru.hh.jclient.common;

import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;

public class JClientBaseTest {

  private JClientBase jClientBase;

  @BeforeEach
  public void setUp() {
    jClientBase = new JClientBase("test", mock(HttpClientFactory.class)) {};
  }

  @Test
  public void testVarargsNotEven() {
    assertThrows(IllegalArgumentException.class, () -> jClientBase.build("GET", "http://test", "a", "b", "c"));
  }

  @Test
  public void testVarargsNotStringKey() {
    assertThrows(ClassCastException.class, () -> jClientBase.build("GET", "http://test", 1, "2"));
  }
}
