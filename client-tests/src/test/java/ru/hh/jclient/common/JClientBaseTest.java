package ru.hh.jclient.common;

import org.junit.Before;
import org.junit.Test;

import java.util.function.UnaryOperator;

import static org.mockito.Mockito.mock;

public class JClientBaseTest {

  private JClientBase jClientBase;

  @Before
  public void setUp() {
    jClientBase = new JClientBase("test", mock(HttpClientFactory.class)) {
      @Override
      public JClientBase withPreconfiguredEngine(Class engineClass, UnaryOperator mapper) {
        return this;
      }
    };
  }

  @Test(expected = IllegalArgumentException.class)
  public void testVarargsNotEven() {
    jClientBase.build("GET", "http://test", "a", "b", "c");
  }

  @Test(expected = ClassCastException.class)
  public void testVarargsNotStringKey() {
    jClientBase.build("GET", "http://test", 1, "2");
  }
}
