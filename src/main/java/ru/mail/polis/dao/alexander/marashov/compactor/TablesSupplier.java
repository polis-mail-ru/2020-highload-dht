package ru.mail.polis.dao.alexander.marashov.compactor;

import ru.mail.polis.dao.alexander.marashov.Table;

import java.util.NavigableMap;
import java.util.function.Supplier;

public interface TablesSupplier extends Supplier<NavigableMap<Integer, Table>> {

}
