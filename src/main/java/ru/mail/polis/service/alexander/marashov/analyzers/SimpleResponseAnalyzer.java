package ru.mail.polis.service.alexander.marashov.analyzers;

import one.nio.http.Response;

public class SimpleResponseAnalyzer extends ResponseAnalyzer<Boolean> {

    private int successCount;
    private final int successCode;
    private final String successResponseString;

    public SimpleResponseAnalyzer(
            final int neededReplicasCount,
            final int totalReplicasCount,
            final int successCode,
            final String successResponseString
    ) {
        super(neededReplicasCount, totalReplicasCount);
        this.successCount = 0;
        this.successCode = successCode;
        this.successResponseString = successResponseString;
    }

    @Override
    protected void privateAccept(final Response response) {
        if (response == null) {
            failedCount++;
        } else {
            answeredCount++;
            privateAccept(response.getStatus() == successCode);
        }
    }

    @Override
    protected void privateAccept(final Boolean isSuccess) {
        if (isSuccess) {
            successCount++;
        }
    }

    @Override
    protected boolean hasEnoughAnswers() {
        return successCount >= neededReplicasCount || super.hasEnoughAnswers();
    }

    @Override
    protected Response privateGetResult() {
        if (successCount >= neededReplicasCount) {
            return new Response(successResponseString, Response.EMPTY);
        } else {
            return new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
        }
    }
}
