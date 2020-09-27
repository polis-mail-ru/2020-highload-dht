/*
 * Copyright 2020 (c) Odnoklassniki
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.mail.polis.dao;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Record from {@link DAO}.
 *
 * @author Dmitry Schitinin
 */
public class Record implements Comparable<Record> {
    private final ByteBuffer key;
    private final ByteBuffer value;

    Record(
            @NotNull final ByteBuffer key,
            @NotNull final ByteBuffer value) {
        this.key = key;
        this.value = value;
    }

    public static Record of(
            @NotNull final ByteBuffer key,
            @NotNull final ByteBuffer value) {
        return new Record(key, value);
    }

    public ByteBuffer getKey() {
        return key.asReadOnlyBuffer();
    }

    public ByteBuffer getValue() {
        return value.asReadOnlyBuffer();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof Record)) return false;
        final Record record = (Record) o;
        return Objects.equals(key, record.key)
                && Objects.equals(value, record.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, value);
    }

    @Override
    public int compareTo(@NotNull final Record other) {
        return this.key.compareTo(other.key);
    }
}
