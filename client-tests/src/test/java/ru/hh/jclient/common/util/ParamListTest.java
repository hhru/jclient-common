package ru.hh.jclient.common.util;

import com.ning.http.client.Param;
import java.util.Arrays;
import java.util.Collections;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import org.junit.Before;
import org.junit.Test;

public class ParamListTest {

  private ParamList paramList;

  @Before
  public void setup() {
    paramList = new ParamList();
  }

  @Test
  public void addInt() {
    paramList.add("key", 1);
    assertThat(paramList.getList(), contains(new Param("key", "1")));
  }

  @Test
  public void addLong() {
    paramList.add("key", 1L);
    assertThat(paramList.getList(), contains(new Param("key", "1")));
  }

  @Test
  public void addSomeObject() {
    paramList.add("key", new StringBuilder("abc"));
    assertThat(paramList.getList(), contains(new Param("key", "abc")));
  }

  @Test
  public void addMultipleItems() {
    paramList.add("foo", 1);
    paramList.add("bar", 2L);
    paramList.add("buz", new StringBuilder("3"));
    assertThat(paramList.getList(), containsInAnyOrder(
        new Param("foo", "1"), new Param("bar", "2"), new Param("buz", "3")
    ));
  }

  @Test
  public void addNullValue() {
    paramList.add("key", null);
    assertThat(paramList.getList(), empty());
  }

  @Test
  public void addCollection() {
    paramList.add("key", Arrays.asList(1, 2, 3));
    assertThat(paramList.getList(), contains(new Param("key", "1,2,3")));
  }

  @Test
  public void addEmptyCollection() {
    paramList.add("key", Collections.emptyList());
    assertThat(paramList.getList(), empty());
  }

  @Test
  public void addPairs() {
    paramList.addPairs("foo", 1, "bar", 2);
    assertThat(paramList.getList(), containsInAnyOrder(new Param("foo", "1"), new Param("bar", "2")));
  }

  @Test
  public void addEmptyPairs() {
    paramList.addPairs();
    assertThat(paramList.getList(), empty());
  }

  @Test(expected = IllegalArgumentException.class)
  public void addNotEvenPairs() {
    paramList.addPairs("foo", 1, "bar");
  }

}
