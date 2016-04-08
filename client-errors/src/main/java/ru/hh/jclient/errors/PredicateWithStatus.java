package ru.hh.jclient.errors;

import java.util.Optional;
import java.util.function.Predicate;

class PredicateWithStatus<T> {

  private Predicate<T> predicate;
  private Optional<Integer> status;

  public PredicateWithStatus(Predicate<T> predicate) {
    this(predicate, null);
  }

  public PredicateWithStatus(Predicate<T> predicate, Integer status) {
    this.predicate = predicate;
    this.status = Optional.ofNullable(status);
  }

  public Predicate<T> getPredicate() {
    return predicate;
  }

  public Optional<Integer> getStatus() {
    return status;
  }

}
