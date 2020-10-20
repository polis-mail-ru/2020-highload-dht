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
import ru.mail.polis.dao.s3ponia.PersistenceDAO;

import java.io.File;
import java.io.IOException;

/**
 * Custom {@link DAO} factory.
 *
 * @author Vadim Tsesko
 */
public final class DAOFactory {
    static final long MAX_HEAP = 256 * 1024 * 1024;

    private DAOFactory() {
        // Not instantiatable
    }

    /**
     * Construct a {@link DAO} instance.
     *
     * @param data local disk folder to persist the data to
     * @return a storage instance
     */
    @NotNull
    public static DAO create(@NotNull final File data) throws IOException {
        if (Runtime.getRuntime().maxMemory() > MAX_HEAP) {
            throw new IllegalStateException("The heap is too big. Consider setting Xmx.");
        }

        if (!data.exists()) {
            throw new IllegalArgumentException("Path doesn't exist: " + data);
        }

        if (!data.isDirectory()) {
            throw new IllegalArgumentException("Path is not a directory: " + data);
        }

        return PersistenceDAO.of(data, MAX_HEAP / 2);
    }
}
