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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import org.junit.jupiter.api.Test;
import ru.mail.polis.Iters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Unit tests for {@link Iters} facilities.
 *
 * @author Dmitry Schitinin
 */
class ItersTest {
    @Test
    void until() {
        assertFalse(Iters.until(Iters.empty(), 0).hasNext());

        final ImmutableList<Integer> sixNumbers = ImmutableList.of(1, 2, 3, 4, 5, 6);

        assertFalse(Iters.until(sixNumbers.iterator(), 0).hasNext());
        
        assertEquals(1, Iterators.size(Iters.until(sixNumbers.iterator(), 2)));
        assertEquals(3, Iterators.size(Iters.until(sixNumbers.iterator(), 4)));
        assertEquals(6, Iterators.size(Iters.until(sixNumbers.iterator(), 7)));
        assertEquals(6, Iterators.size(Iters.until(sixNumbers.iterator(), 100)));
    }

    @Test
    void collapseEquals() {
        assertFalse(Iters.collapseEquals(Iters.empty()).hasNext());

        final Integer[] collapsed = Iterators.toArray(
                Iters.collapseEquals(
                        ImmutableList.of(1, 1, 2, 3, 3, 5, 6).iterator()),
                Integer.class);
        assertEquals(
                ImmutableList.of(1, 2, 3, 5, 6),
                ImmutableList.copyOf(collapsed));
    }
}
