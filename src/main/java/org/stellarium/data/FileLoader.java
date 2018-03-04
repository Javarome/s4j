package org.stellarium.data;

import java.io.File;
import java.net.URL;

/**
 * Created by IntelliJ IDEA.
 * User: freds
 * Date: Apr 5, 2008
 * Time: 2:56:59 PM
 * To change this template use File | Settings | File Templates.
 */
public interface FileLoader {
    URL getOrLoadFile(File file);

    //File[] listFiles(File dir);

    //File[] listFiles(URL dir, FilenameFilter filter);

    URL getOrLoadFile(URL url);
}
