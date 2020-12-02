package ru.mail.polis.utils;

import one.nio.http.HttpSession;
import one.nio.http.Request;
import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.service.mrsandman5.ServiceImpl;
import ru.mail.polis.service.mrsandman5.clustering.Topology;
import ru.mail.polis.service.mrsandman5.replication.ReplicasFactor;

import java.time.Instant;

public final class RequestUtils {

    private static final Logger log = LoggerFactory.getLogger(ServiceImpl.class);

    private RequestUtils() {
    }

    /** Calculate replicas factor from request*/
    public static ReplicasFactor getReplicasFactor(@NotNull final HttpSession session,
                                                   @NotNull final Topology<String> topology,
                                                   final boolean proxied,
                                                   final String replicas,
                                                   @NotNull final ReplicasFactor quorum) {
        ReplicasFactor replicasFactor;
        try {
            replicasFactor = proxied || replicas == null ? quorum : ReplicasFactor.parser(replicas);
        } catch (NumberFormatException e) {
            log.error("Request replica parsing error", e);
            ResponseUtils.sendEmptyResponse(session, Response.BAD_REQUEST);
            return null;
        }
        if (replicasFactor.getAck() < 1
                || replicasFactor.getFrom() < replicasFactor.getAck()
                || replicasFactor.getFrom() > topology.all().size()) {
            ResponseUtils.sendEmptyResponse(session, Response.BAD_REQUEST);
            return null;
        }
        return replicasFactor;
    }

    /** Calculate expire time from request*/
    public static Instant getExpire(@NotNull final Request request,
                                    final String expire,
                                    final boolean proxied) {
        final boolean expired = request.getHeader(ResponseUtils.EXPIRES) != null;
        final String proxyExpire = expired ? request.getHeader(ResponseUtils.EXPIRES).substring(2) : null;
        Instant expireTime;
        if (expire == null) {
            expireTime = (proxied && proxyExpire != null ? ResponseUtils.parseExpires(proxyExpire) : Instant.MAX);
        } else {
            expireTime = ResponseUtils.parseExpires(expire);
        }
        return expireTime;
    }

}
