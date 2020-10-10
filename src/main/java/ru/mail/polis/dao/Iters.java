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

import com.google.common.base.Functions;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.dao.bmendli.NoSuchElementLightException;

import java.util.Iterator;
import java.util.function.Function;

/**
 * Utility methods for iterators.
 *
 * @author Dmitry Schitinin
 */
public final class Iters {

    private static final Iterator<Object> EMPTY = new Iterator<>() {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public Object next() {
            throw new NoSuchElementLightException("Next on empty iterator");
        }
    };

    private Iters() {

    }

    @SuppressWarnings("unchecked")
    public static <E> Iterator<E> empty() {
        return (Iterator<E>) EMPTY;
    }

    public static <E extends Comparable<E>> Iterator<E> until(
            @NotNull final Iterator<E> iter,
            @NotNull final E until) {
        return new UntilIterator<>(iter, until);
    }

    public static <E> Iterator<E> collapseEquals(
            @NotNull final Iterator<E> iter,
            @NotNull final Function<E, ?> byKey) {
        return new CollapseEqualsIterator<>(iter, byKey);
    }

    public static <E> Iterator<E> collapseEquals(@NotNull final Iterator<E> iter) {
        return new CollapseEqualsIterator<>(iter);
    }

    private static class UntilIterator<E extends Comparable<E>> implements Iterator<E> {
        private final Iterator<E> iter;
        private final E until;

        private E next;

        UntilIterator(
                @NotNull final Iterator<E> iter,
                @NotNull final E until) {
            this.iter = iter;
            this.until = until;
            this.next = iter.hasNext() ? iter.next() : null;
        }

        @Override
        public boolean hasNext() {
            return next != null && next.compareTo(until) < 0;
        }

        @Override
        public E next() {
            assert hasNext();

            final E result = this.next;
            this.next = iter.hasNext() ? iter.next() : null;
            return result;
        }
    }

    private static class CollapseEqualsIterator<E> implements Iterator<E> {
        private final Iterator<E> iter;
        private final Function<E, ?> keyExtractor;

        private E next;

        CollapseEqualsIterator(
                @NotNull final Iterator<E> iter,
                @NotNull final Function<E, ?> keyExtractor) {
            this.iter = iter;
            this.keyExtractor = keyExtractor;
            this.next = iter.hasNext() ? iter.next() : null;
        }

        CollapseEqualsIterator(@NotNull final Iterator<E> iter) {
            this(iter, Functions.identity());
        }

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public E next() {
            assert hasNext();

            final E result = next;

            // Advance to the next distinct key
            this.next = null;
            while (iter.hasNext()) {
                final E key = iter.next();
                if (!keyExtractor.apply(key)
                        .equals(keyExtractor.apply(result))) {
                    this.next = key;
                    break;
                }
            }

            return result;
        }
    }
}
