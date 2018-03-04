package org.stellarium.data;

import java.net.URL;

/**
 * Created by IntelliJ IDEA.
 * User: freds
 * Date: Apr 5, 2008
 * Time: 3:03:41 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class AbstractFileLoader implements FileLoader {
    protected final ResourceLocatorUtil locatorUtil;

    public AbstractFileLoader(ResourceLocatorUtil locatorUtil) {
        this.locatorUtil = locatorUtil;
    }

    public URL getOrLoadFile(URL url) {
        return url;
    }
}
