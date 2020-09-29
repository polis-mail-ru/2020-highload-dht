package ru.mail.polis.service.mariarheon;

import com.google.common.net.HttpHeaders;
import one.nio.http.Response;

/**
 * Response with zero length of body
 **/

public class ZeroResponse extends Response {
    /**
     * Create response with zero length of body
     * @param resultCode http result code
     **/
    public ZeroResponse(final String resultCode) {
        super(resultCode);
        this.addHeader(HttpHeaders.CONTENT_LENGTH + ": " + 0);
    }
}
