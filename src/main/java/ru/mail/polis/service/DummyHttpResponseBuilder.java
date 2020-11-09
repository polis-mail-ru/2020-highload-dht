package ru.mail.polis.service;

import javax.net.ssl.SSLSession;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

public class DummyHttpResponseBuilder implements HttpResponse<byte[]> {

    private int code;
    private byte[] body;

    DummyHttpResponseBuilder setCode(final int code) {
        this.code = code;
        return this;
    }

    DummyHttpResponseBuilder setBody(final byte[] body) {
        this.body = body.clone();
        return this;
    }

    @Override
    public int statusCode() {
        return code;
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
        return body.clone();
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
