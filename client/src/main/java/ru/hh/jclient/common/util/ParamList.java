package ru.hh.jclient.common.util;

import com.google.common.base.Joiner;
import com.ning.http.client.Param;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ParamList {

  private List<Param> params = new ArrayList<>();

  public void add(String k, Object v) {
    if (v instanceof Collection) {
      addCollection(k, (Collection) v);
    } else
      addSingle(k, v);

  }

  public void add(String k, int v) {
    addSingle(k, String.valueOf(v));
  }

  public void add(String k, long v) {
    addSingle(k, String.valueOf(v));
  }

  public List<Param> getList() {
    return params;
  }

  private void addSingle(String k, Object v) {
    if (v != null) {
      params.add(new Param(k, v.toString()));
    }
  }

  private void addCollection(String k, Collection v) {
    if (v != null && !v.isEmpty()) {
      params.add(new Param(k, joinCollection(v)));
    }
  }

  private <T> String joinCollection(Collection<T> collection) {
    return Joiner.on(',').join(collection);
  }

}
