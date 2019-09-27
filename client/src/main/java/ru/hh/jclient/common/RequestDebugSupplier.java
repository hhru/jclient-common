package ru.hh.jclient.common;

import java.util.function.Supplier;

/**
 * DI should identify right supplier
 */
@FunctionalInterface
public interface RequestDebugSupplier extends Supplier<RequestDebug> {
}
