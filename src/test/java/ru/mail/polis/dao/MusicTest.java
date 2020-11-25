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

import com.google.common.base.Charsets;
import com.google.common.collect.Iterators;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.mail.polis.TestBase;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Music database over {@link DAO}.
 *
 * @author Vadim Tsesko
 */
class MusicTest extends TestBase {
    private static final String DELIMITER = ":";

    @NotNull
    private static ByteBuffer artistFrom(
            @NotNull final String artist) {
        assert !artist.contains(DELIMITER);
        return ByteBuffer.wrap((artist + DELIMITER).getBytes(Charsets.UTF_8));
    }

    @NotNull
    private static ByteBuffer albumFrom(
            @NotNull final String artist,
            @NotNull final String album) {
        assert !artist.contains(DELIMITER);
        assert !album.contains(DELIMITER);
        return ByteBuffer.wrap((artist + DELIMITER + album + DELIMITER).getBytes(Charsets.UTF_8));
    }

    @NotNull
    private static ByteBuffer trackFrom(
            @NotNull final String artist,
            @NotNull final String album,
            @NotNull final String track) {
        assert !artist.contains(DELIMITER);
        assert !album.contains(DELIMITER);
        assert !track.contains(DELIMITER);
        return ByteBuffer.wrap((artist + DELIMITER + album + DELIMITER + track).getBytes(Charsets.UTF_8));
    }

    @NotNull
    private static ByteBuffer from(final int... bytes) {
        assert Arrays.stream(bytes).allMatch(b -> Byte.MIN_VALUE <= b && b <= Byte.MAX_VALUE);
        final byte[] buffer = new byte[bytes.length];
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = (byte) bytes[i];
        }
        return ByteBuffer.wrap(buffer);
    }

    @NotNull
    private static ByteBuffer next(@NotNull final ByteBuffer buffer) {
        boolean ones = true;
        for (int i = 0; i < buffer.remaining(); i++) {
            if (buffer.get(i) != Byte.MAX_VALUE) {
                ones = false;
                break;
            }
        }

        final byte[] next = new byte[buffer.remaining() + (ones ? 1 : 0)];
        int j = next.length - 1;
        int carry = 1;
        for (int i = buffer.remaining() - 1; i >= 0; i--, j--) {
            final int b = buffer.get(i);
            final int sum = b + carry;
            final byte v;
            if (sum > Byte.MAX_VALUE) {
                v = 0;
                carry = 1;
            } else {
                v = (byte) sum;
                carry = 0;
            }
            next[j] = v;
        }

        if (ones) {
            next[0] = (byte) carry;
        }

        return ByteBuffer.wrap(next);
    }

    @Test
    void nextTest() {
        assertEquals(from(1), next(from(0)));
        assertEquals(from(-126), next(from(-127)));
        assertEquals(from(1, 0), next(from(127)));
        assertEquals(from(1, 0, 0), next(from(127, 127)));
        assertEquals(from(127, 127), next(from(127, 126)));
        assertEquals(from(127, 0), next(from(126, 127)));
    }

    @Test
    void database(@TempDir File data) throws IOException {
        // Fill music database
        try (DAO dao = DAOFactory.create(data)) {
            dao.upsert(trackFrom("Ar1", "Al11", "T111"), randomValueBuffer(), null);
            dao.upsert(trackFrom("Ar1", "Al11", "T112"), randomValueBuffer(), null);
            dao.upsert(trackFrom("Ar1", "Al12", "T111"), randomValueBuffer(), null);
            dao.upsert(trackFrom("Ar1", "Al12", "T112"), randomValueBuffer(), null);
            dao.upsert(trackFrom("Ar1", "Al12", "T113"), randomValueBuffer(), null);
            dao.upsert(trackFrom("Ar2", "Al21", "T211"), randomValueBuffer(), null);
            dao.upsert(trackFrom("Ar2", "Al21", "T212"), randomValueBuffer(), null);
        }

        // Open music database
        try (DAO dao = DAOFactory.create(data)) {
            // Artists
            assertEquals(5, Iterators.size(dao.range(artistFrom("Ar1"), next(artistFrom("Ar1")))));
            assertEquals(2, Iterators.size(dao.range(artistFrom("Ar2"), next(artistFrom("Ar2")))));

            // Albums
            assertEquals(2, Iterators.size(dao.range(albumFrom("Ar1", "Al11"), next(albumFrom("Ar1", "Al11")))));
            assertEquals(3, Iterators.size(dao.range(albumFrom("Ar1", "Al12"), next(albumFrom("Ar1", "Al12")))));
            assertEquals(2, Iterators.size(dao.range(albumFrom("Ar2", "Al21"), next(albumFrom("Ar2", "Al21")))));
        }
    }
}
