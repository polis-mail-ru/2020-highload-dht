package ru.mail.polis.service.alexander.marashov.analyzers;

import one.nio.http.Response;

public class ResponseAnalyzerDelete extends ResponseAnalyzer<Boolean> {

    private int successCount;

    public ResponseAnalyzerDelete(final int neededReplicasCount, final int totalReplicasCount) {
        super(neededReplicasCount, totalReplicasCount);
        this.successCount = 0;
    }

    @Override
    protected void privateAccept(final Response response) {
        if (response == null) {
            failedCount++;
        } else {
            answeredCount++;
            privateAccept(response.getStatus() == 202);
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
            return new Response(Response.ACCEPTED, Response.EMPTY);
        } else {
            return new Response(Response.GATEWAY_TIMEOUT, Response.EMPTY);
        }
    }
}
