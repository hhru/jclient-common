package ru.hh.jclient.common.util.jersey;

import java.util.List;
import java.util.Map;

interface MultivaluedMap<K, V> extends Map<K, List<V>> {

  /**
   * Set the key's value to be a one item list consisting of the supplied value. Any existing values will be replaced.
   *
   * @param key
   *          the key
   * @param value
   *          the single value of the key
   */
  void putSingle(K key, V value);

  /**
   * Add a value to the current list of values for the supplied key.
   *
   * @param key
   *          the key
   * @param value
   *          the value to be added.
   */
  void add(K key, V value);

  /**
   * A shortcut to get the first value of the supplied key.
   *
   * @param key
   *          the key
   * @return the first value for the specified key or null if the key is not in the map.
   */
  V getFirst(K key);

}
