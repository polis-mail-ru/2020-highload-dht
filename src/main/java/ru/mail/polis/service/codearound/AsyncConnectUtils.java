package ru.mail.polis.service.codearound;

import javax.net.ssl.SSLSession;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

/**
 *  class methods implement some structural engineering of HTTP responses.
 */
public class AsyncConnectUtils implements HttpResponse<byte[]> {

    private int returnCode;
    private byte[] body;

    /**
     * retrieves class instance with HTTP return code specified.
     *
     * @param returnCode - HTTP return code
     * @return AsyncConnectUtils instance
     */
    public AsyncConnectUtils setReturnCode(final int returnCode) {
        this.returnCode = returnCode;
        return this;
    }

    /**
     * retrieves class instance with body attribute specified.
     *
     * @param body - HTTP response body
     * @return AsyncConnectUtils instance
     */
    public AsyncConnectUtils setBody(final byte[] body) {
        this.body = body.clone();
        return this;
    }

    @Override
    public int statusCode() {
        return returnCode;
    }

    @Override
    public HttpRequest request() {
        return null;
    }

    @Override
    public Optional<HttpResponse<byte[]>> previousResponse() {
        return Optional.empty();
    }

    @Override
    public HttpHeaders headers() {
        return null;
    }

    @Override
    public byte[] body() {
        return this.body.clone();
    }

    @Override
    public Optional<SSLSession> sslSession() {
        return Optional.empty();
    }

    @Override
    public URI uri() {
        return null;
    }

    @Override
    public HttpClient.Version version() {
        return null;
    }
}
