package org.stellarium.data;

import org.stellarium.StellariumException;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * Created by IntelliJ IDEA.
 * User: freds
 * Date: Apr 5, 2008
 * Time: 2:57:50 PM
 * To change this template use File | Settings | File Templates.
 */
public class SimpleFileLoaderImpl extends AbstractFileLoader {

    public SimpleFileLoaderImpl(ResourceLocatorUtil locatorUtil) {
        super(locatorUtil);
    }

    public URL getOrLoadFile(File file) {
        try {
            return new URL(file.toURI().toURL().toExternalForm());
        } catch (IOException e) {
            throw new StellariumException(e);
        }
    }

    public File[] listFiles(File dir) {
        if (!dir.exists()) {
            throw new StellariumException("Cannot list content of non existing folder " + dir);
        }
        return dir.listFiles();
    }

    /*public File[] listFiles(URL dir, FilenameFilter filter) {
        return dir.listFiles(filter);
    } */
}
