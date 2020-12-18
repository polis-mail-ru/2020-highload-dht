package ru.mail.polis.service.basta123.executejs;

import com.google.common.base.Splitter;
import one.nio.http.HttpClient;
import one.nio.http.HttpException;
import one.nio.http.Request;
import one.nio.http.Response;
import one.nio.pool.PoolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;
import ru.mail.polis.service.basta123.ReplicHttpServerImpl;
import ru.mail.polis.service.basta123.Topology;

import javax.script.ScriptException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static java.nio.charset.StandardCharsets.UTF_8;

public class SendJSToNodes {

    private static final Logger LOGGER = LoggerFactory.getLogger(SendJSToNodes.class);
    private final ExecJSNashorn execJSNashorn;
    private final Map<String, HttpClient> clientAndNode;

    public SendJSToNodes(Map<String, HttpClient> clientAndNode, DAO dao) {
        this.clientAndNode = clientAndNode;
        execJSNashorn = new ExecJSNashorn(dao);
    }


    public Response sendJSToNodes(final Request request,
                                  final boolean requestForward, final Topology<String> topology) {
        final String js = new String(request.getBody(), UTF_8);
        if (requestForward) {
            try {
                return execJSNashorn.execOnNodes(js);
            } catch (NoSuchElementException | ScriptException exc) {
                LOGGER.error("can't exec JS: ");
                return new Response(Response.NOT_FOUND, Response.EMPTY);
            }
        }
        final List<Response> responses = new ArrayList<>();
        final List<String> nodes = topology.getAllNodes();

        for (final String node : nodes) {
            try {
                Response response;
                if (topology.isLocal(node)) {
                    response = execJSNashorn.execOnNodes(js);
                } else {
                    request.addHeader(ReplicHttpServerImpl.FORWARD_REQ);
                    response = clientAndNode.get(node).invoke(request);
                }
                if ((response.getStatus() == 404 && response.getBody().length == 0) || response.getStatus() == 500) {
                    continue;
                } else {
                    responses.add(response);
                }

            } catch (HttpException | PoolException | InterruptedException | IOException | ScriptException e) {
                LOGGER.error("can't get response: ", e);
            }
        }

        return onCoordinator(js, responses);

    }

    Response onCoordinator(final String js, List<Response> responses) {
        final List<String> stringArray = new ArrayList<>();
        for (final Response response : responses) {
            stringArray.add(response.getBodyUtf8());
        }

        final List<String> arrayArrays = new ArrayList<>();
        for (final String string : stringArray) {
            arrayArrays.addAll(Splitter.on(',').splitToList(string));
        }

        return execJSNashorn.execOnCoordinator(js, arrayArrays);
    }
}
