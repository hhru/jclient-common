package ru.hh.jclient.common.util;

import com.google.common.base.Joiner;
import static com.google.common.base.Preconditions.checkArgument;
import com.ning.http.client.Param;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class ParamList {

  private List<Param> params = new ArrayList<>();

  public void add(String k, Object v) {
    if (v instanceof Collection) {
      addCollection(k, (Collection<?>) v);
    } else
      addSingle(k, v);
  }

  public void add(String k, int v) {
    addSingle(k, String.valueOf(v));
  }

  public void add(String k, long v) {
    addSingle(k, String.valueOf(v));
  }

  public void addPairs(Object... pairs) {
    checkArgument(pairs.length % 2 == 0, "pairs should have event amount of items");
    for (int i = 0; i < pairs.length; i += 2) {
      addSingle(pairs[i].toString(), pairs[i + 1]);
    }
  }

  public void add(Map.Entry<String, Object> entry) {
    add(entry.getKey(), entry.getValue());
  }

  public List<Param> getList() {
    return params;
  }

  private void addSingle(String k, Object v) {
    if (v != null) {
      params.add(new Param(k, v.toString()));
    }
  }

  private void addCollection(String k, Collection<?> v) {
    if (v != null && !v.isEmpty()) {
      params.add(new Param(k, joinCollection(v)));
    }
  }

  private <T> String joinCollection(Collection<T> collection) {
    return Joiner.on(',').join(collection);
  }

  public ParamList merge(ParamList other) {
    getList().addAll(other.getList());
    return this;
  }
}
