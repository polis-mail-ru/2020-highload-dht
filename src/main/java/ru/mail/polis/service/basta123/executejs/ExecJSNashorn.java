package ru.mail.polis.service.basta123.executejs;

import one.nio.http.Response;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.dao.DAO;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.List;

public class ExecJSNashorn {
    private final DAO dao;
    private static final Logger LOGGER = LoggerFactory.getLogger(ExecJSNashorn.class);
    final ScriptEngine engine;

    public ExecJSNashorn(final DAO dao) {
        this.dao = dao;
        engine = new ScriptEngineManager().getEngineByName("nashorn");
    }

    /**
     * Method execute function execOnNodes.
     *
     * @param js - js code
     */
    @NotNull
    public Response execOnNodes(@NotNull final String js) throws ScriptException {
        execJSCode(js);
        final Invocable invocable = (Invocable) engine;
        Object result = new Object();
        try {
            result = invocable.invokeFunction("execOnNodes", dao);
        } catch (ScriptException | NoSuchMethodException e) {
            LOGGER.error("can't invoke function: ", e);
        }
        return Response.ok(String.valueOf(result));
    }

    private void execJSCode(@NotNull String js) {
        try {
            engine.eval(js);
        } catch (ScriptException e) {
            LOGGER.error("error with eval: ", e);
        }
    }

    /**
     * Method execute function cCoordinatorrrrrr.
     *
     * @param js        - js code
     * @param responses - responses from nodes
     */
    @NotNull
    public Response cCoordinatorrrrrr(@NotNull final String js,
                                      @NotNull final List<String> responses) {
        execJSCode(js);
        final Invocable invocable = (Invocable) engine;
        Object result = new Object();
        try {
            result = invocable.invokeFunction("cCoordinatorrrrrr", responses);
        } catch (ScriptException | NoSuchMethodException e) {
            LOGGER.error("can't invoke function: ", e);
        }
        return Response.ok(String.valueOf(result));
    }

}
