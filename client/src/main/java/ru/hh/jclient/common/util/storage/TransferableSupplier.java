package ru.hh.jclient.common.util.storage;

import java.util.function.Supplier;

public interface TransferableSupplier<T> extends Supplier<T> {

  PreparedTransfer prepareTransfer();

}
