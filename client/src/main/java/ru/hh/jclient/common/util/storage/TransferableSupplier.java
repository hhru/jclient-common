package ru.hh.jclient.common.util.storage;

import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.jclient.common.HttpClientContextThreadLocalSupplier;

@FunctionalInterface
public interface TransferableSupplier<T> extends Supplier<T> {

  TransferableStorage<T> getStorage();

  default Logger getLog() {
    return LoggerFactory.getLogger(HttpClientContextThreadLocalSupplier.class);
  }

  @Override
  default T get() {
    return getStorage().get();
  }

  default void set(T t) {
    if (getStorage().get() != null) {
      getLog().warn("Replacing existing object {} with {}", getStorage().get(), t);
    }
    getStorage().set(t);
  }

  default void remove() {
    getStorage().remove();
  }

  default void remove(T t) {
    if (getStorage().get() != t) {
      getLog().warn("Unexpected object when removing {} - was {}", t, getStorage().get());
    }
    else if (getStorage().get() == null) {
      getLog().warn("Unexpected object when removing {} - null", t);
    }
    remove();
  }

}
