package ru.hh.jclient.common;

import com.google.common.base.Throwables;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.hamcrest.Matcher;
import org.junit.Test;
import ru.hh.jclient.common.util.storage.Storage;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

public class HttpClientFactoryBuilderTest {

  @Test
  public void testImmutableByDefault() {
    var initial = new HttpClientFactoryBuilder(mock(Storage.class), List.of());
    testBuilderMethods(initial, not(equalTo(initial)));
  }

  @Test
  public void testMutableCopy() {
    var initial = new HttpClientFactoryBuilder(new HttpClientFactoryBuilder(mock(Storage.class), List.of()), true);
    testBuilderMethods(initial, equalTo(initial));
  }

  private void testBuilderMethods(HttpClientFactoryBuilder initial, Matcher<Object> matcher) {
    var methods = HttpClientFactoryBuilder.class.getDeclaredMethods();
    Stream.of(methods)
        .filter(method -> !Modifier.isStatic(method.getModifiers())
            && Modifier.isPublic(method.getModifiers())
            && HttpClientFactoryBuilder.class.equals(method.getReturnType())
            && method.getParameterTypes().length == 1)
        .forEach(builderMethod -> {
          try {
            var actual = builderMethod.invoke(initial, generateArgument(builderMethod));
            assertThat(actual, matcher);
          } catch (Exception e) {
            fail("Fail to invoke method " + builderMethod.getName() + ":" + System.lineSeparator() + Throwables.getStackTraceAsString(e));
          }
        });
  }

  private Object generateArgument(Method method) {
    var argClass = method.getParameterTypes()[0];
    if (Object.class.equals(argClass)) {
      return new DefaultAsyncHttpClientConfig.Builder().build();
    } else if (boolean.class.equals(argClass)) {
      return true;
    } else if (double.class.equals(argClass)) {
      return 1.1;
    } else if (int.class.equals(argClass)) {
      return 1;
    } else if (String.class.equals(argClass)) {
      return "test";
    } else if (Collection.class.equals(argClass)) {
      return List.of();
    } else {
      return mock(argClass);
    }
  }
}
