package org.stellarium.astro;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Commons for all Stellarium tests.
 */
public class AbstractStellariumTest extends TestCase {
    private final java.util.List<String> commandLineArgs = new ArrayList<String>();

    protected void setUp() throws Exception {
        Properties properties = System.getProperties();
        for (Map.Entry<Object, Object> objectObjectEntry : properties.entrySet()) {
            String key = (String) objectObjectEntry.getKey();
            String prefix = "org.stellarium";
            if (key.startsWith(prefix)) {
                commandLineArgs.add("--" + key.substring(prefix.length() + 1));
                commandLineArgs.add(String.valueOf(objectObjectEntry.getValue()));
            }
        }
    }

    protected List<String> getCommandLineArgs() {
        return commandLineArgs;
    }
}
