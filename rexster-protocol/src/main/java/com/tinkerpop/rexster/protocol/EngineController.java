package com.tinkerpop.rexster.protocol;

import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Manages the list of EngineHolder items for the current ScriptEngineManager.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class EngineController {
    private final ScriptEngineManager manager = new ScriptEngineManager();
    private final Map<String, EngineHolder> engines = new HashMap<String, EngineHolder>();

    /**
     * All gremlin engines are prefixed with this value.
     */
    private static final String ENGINE_NAME_PREFIX = "gremlin-";

    /**
     * Add all flavors of gremlin to this list. This should be the true name of the language.
     */
    private final List<String> gremlinEngineNames = new ArrayList<String>() {{
        add("gremlin-groovy");
    }};

    private static EngineController engineController;

    private EngineController() {
        // for ruby
        System.setProperty("org.jruby.embed.localvariable.behavior", "persistent");
        for (ScriptEngineFactory factory : this.manager.getEngineFactories()) {

            // only add engine factories for those languages that are gremlin based.
            if (gremlinEngineNames.contains(factory.getLanguageName())) {
                this.engines.put(factory.getLanguageName(), new EngineHolder(factory));
            }
        }
    }

    public static EngineController getInstance() {
        if (engineController == null) {
            engineController = new EngineController();
        }

        return engineController;
    }

    public List<String> getAvailableEngineLanguages() {
        final List<String> languages = new ArrayList<String>();
        for (String fullLanguageName : this.engines.keySet()) {
            languages.add(fullLanguageName.substring(fullLanguageName.indexOf(ENGINE_NAME_PREFIX) + ENGINE_NAME_PREFIX.length()));
        }

        return Collections.unmodifiableList(languages);
    }

    public boolean isEngineAvailable(final String languageName) {
        boolean available = false;
        try {
            getEngineByLanguageName(languageName);
            available = true;
        } catch (ScriptException se) {
            available = false;
        }

        return available;
    }

    public EngineHolder getEngineByLanguageName(final String languageName) throws ScriptException {

        final EngineHolder engine = this.engines.get(ENGINE_NAME_PREFIX + languageName);
        if (engine == null) {
            throw new ScriptException("No engine for the language: " + languageName);
        }

        return engine;
    }
}
