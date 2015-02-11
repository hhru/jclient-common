package ru.hh.jclient.common.util;

import java.util.Collection;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import com.google.common.collect.Multimap;
import com.ning.http.client.FluentCaseInsensitiveStringsMap;

public class MoreCollectors {

  public static <K, V, A extends Multimap<K, V>> Collector<Entry<K, V>, A, A> toMultimap(Supplier<A> supplier) {
    return toMultimap(e -> e.getKey(), e -> e.getValue(), supplier);
  }

  public static <T, K, V, A extends Multimap<K, V>> Collector<T, A, A> toMultimap(Function<? super T, ? extends K> keyMapper,
    Function<? super T, ? extends V> valueMapper, Supplier<A> supplier) {
    return Collector.of(supplier, (map, in) -> map.put(keyMapper.apply(in), valueMapper.apply(in)), (map1, map2) -> {
      map1.putAll(map2);
      return map1;
    });
  }

  public static <T> Collector<T, FluentCaseInsensitiveStringsMap, FluentCaseInsensitiveStringsMap> toFluentCaseInsensitiveStringsMap(
    Function<? super T, String> keyMapper, Function<? super T, Collection<String>> valueMapper) {
    return Collector.of(FluentCaseInsensitiveStringsMap::new, (map, in) -> map.add(keyMapper.apply(in), valueMapper.apply(in)), (map1, map2) -> {
      map1.addAll(map2);
      return map1;
    });
  }
}
