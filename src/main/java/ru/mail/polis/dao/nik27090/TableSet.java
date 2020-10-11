package ru.mail.polis.dao.nik27090;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

final class TableSet {

    @NotNull
    final MemTable mem;

    @NotNull
    final Set<Table> flushing; // read-only memTable

    @NotNull
    final NavigableMap<Integer, SSTable> files; // read-only ssTable

    int generation;

    private TableSet(@NotNull final MemTable mem, @NotNull final Set<Table> flushing,
                     @NotNull final NavigableMap<Integer, SSTable> files, final int generation) {
        assert generation >= 0;
        this.mem = mem;
        this.flushing = Collections.unmodifiableSet(flushing);
        this.files = Collections.unmodifiableNavigableMap(files);
        this.generation = generation;

    }

    @NotNull
    static TableSet fromFiles(@NotNull final NavigableMap<Integer, SSTable> files, final int generation) {
        return new TableSet(new MemTable(), Collections.emptySet(), files, generation);
    }

    @NotNull
    TableSet markedAsFlushing() {
        final Set<Table> flushing = new HashSet<>(this.flushing);
        flushing.add(mem);
        return new TableSet(new MemTable(), flushing, files, this.generation + 1);
    }

    @NotNull
    TableSet flushed(@NotNull final Table mem, @NotNull final SSTable file, final int generation) {
        final Set<Table> flushing = new HashSet<>(this.flushing);
        if(!flushing.remove(mem)) {
            throw new IllegalStateException("Flushing table that doesn't exist");
        }
        final NavigableMap<Integer, SSTable> files = new TreeMap<>(this.files);
        if(files.put(generation, file) != null) {
            throw new IllegalStateException("Error with generation");
        }
        return new TableSet(this.mem, flushing, files, this.generation);
    }

    @NotNull
    TableSet startCompacting() {
        return new TableSet(mem, flushing, files, generation + 1);
    }

    @NotNull
    TableSet replaceCompactedFiles(@NotNull final NavigableMap<Integer, SSTable> source,
                                   @NotNull final SSTable destination, final int generation) {

        final NavigableMap<Integer, SSTable> files = new TreeMap<>(this.files);

        for (final Map.Entry<Integer, SSTable> entry : source.entrySet()) {
            if(!files.remove(entry.getKey(), entry.getValue())) {
                throw new IllegalStateException("Trying to delete nothing");
            }
        }
        if (files.put(generation, destination) != null) {
            throw new IllegalStateException("Problem with generation");
        }

        return new TableSet(this.mem, this.flushing, files, this.generation);
    }
}
