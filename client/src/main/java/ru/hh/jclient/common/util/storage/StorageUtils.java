package ru.hh.jclient.common.util.storage;

import static java.util.stream.Collectors.toList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class StorageUtils {

  private StorageUtils() {
  }

  public static Storages build(Collection<Storage<?>> storages) {
    return new Storages(storages);
  }

  public static Storages build(Storage<?> storage) {
    return new Storages(Collections.singleton(storage));
  }

  public static Storages build(Storage<?>... storages) {
    return new Storages(Arrays.asList(storages));
  }

  public static class Storages {

    protected Collection<Storage<?>> storages;

    protected Storages(Collection<Storage<?>> storages) {
      this.storages = new ArrayList<>(storages);
    }

    public Storages add(Storage<?> storage) {
      this.storages.add(storage);
      return this;
    }

    public Transfers prepare() {
      return new Transfers(storages.stream().map(Storage::prepareTransferToAnotherThread).collect(toList()));
    }

    public Storages copy() {
      return new Storages(storages);
    }

  }

  public static class Transfers {

    private Collection<Transfer> transfers;

    protected Transfers(Collection<Transfer> transfers) {
      this.transfers = transfers;
    }

    public void perform() {
      transfers.forEach(Transfer::perform);
    }

    public void rollback() {
      transfers.forEach(Transfer::rollback);
    }
  }

}
