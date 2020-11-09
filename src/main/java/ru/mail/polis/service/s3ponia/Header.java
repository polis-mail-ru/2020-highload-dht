package ru.mail.polis.service.s3ponia;

import one.nio.http.Request;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;

public final class Header {
    public final String key;
    public final String value;

    private Header(@NotNull final String key, @NotNull final String value) {
        this.key = key;
        this.value = value;
    }

    private static Header getHeader(@NotNull final String key,
                                            @NotNull final String[] headers,
                                            final int headerCount) {
        final int keyLength = key.length();
        for (int i = 1; i < headerCount; ++i) {
            if (headers[i].regionMatches(true, 0, key, 0, keyLength)) {
                final var value = headers[i].substring(headers[i].indexOf(':') + 1).stripLeading();
                return new Header(headers[i], value);
            }
        }

        return null;
    }

    /**
     * Get header with key from request.
     *
     * @param key     header's key
     * @param request request with headers
     * @return request's header
     */
    public static Header getHeader(@NotNull final String key, @NotNull final Request request) {
        final var headers = request.getHeaders();
        final var headerCount = request.getHeaderCount();

        return getHeader(key, headers, headerCount);
    }

    /**
     * Get header with key from response.
     *
     * @param key      header's key
     * @param response response with headers
     * @return response's header
     */
    public static Header getHeader(@NotNull final String key, @NotNull final Response response) {
        final var headers = response.getHeaders();
        final var headerCount = response.getHeaderCount();

        return getHeader(key, headers, headerCount);
    }
}
