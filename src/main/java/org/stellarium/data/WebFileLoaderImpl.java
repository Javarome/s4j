package org.stellarium.data;

import org.stellarium.StellariumException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * User: freds
 * Date: Apr 5, 2008
 * Time: 3:02:49 PM
 */
public class WebFileLoaderImpl extends AbstractFileLoader {
    private final String homeServerUrl;

    public WebFileLoaderImpl(ResourceLocatorUtil locatorUtil, String homeServerUrl) {
        super(locatorUtil);
        this.homeServerUrl = homeServerUrl;
    }

    public URL getOrLoadFile(File file) {
        if (!file.exists()) {

            // Create cache local folder on the fly
            File folder = file.getParentFile();
            if (!folder.exists()) {
                if (!folder.mkdirs()) {
                    throw new StellariumException("Loading file " + file +
                            " cannot be done since folder " + folder + " cannot be created.");
                }
            }
            String extraPath = null;
            while (folder != null) {
                if (folder.equals(locatorUtil.getStellariumHome())) {
                    // Good the file suppose to be under stellarium home
                    extraPath = file.getPath().substring(folder.getPath().length()).replace('\\', '/');
                    break;
                }
                folder = folder.getParentFile();
            }
            if (extraPath != null) {
                loadDynamically(file, extraPath);
            }
        }
        try {
            return new URL(file.toURI().toURL(), "");
        } catch (MalformedURLException e) {
            throw new StellariumException(e);
        }
    }

/*    public File[] listFiles(File dir) {
        try {
            File listFile = getOrLoadFile(new File(dir, "list.txt"));
            List<String> stringList = DataFileUtil.getLines(listFile, "List of files in " + dir, true);
            List<File> result = new ArrayList<File>();
            for (String fileName : stringList) {
                if (fileName != null) {
                    fileName = fileName.trim();
                    if (fileName.length() > 0) {
                        File file = new File(dir, fileName);
                        if (!result.contains(file)) {
                            result.add(file);
                        }
                    }
                }
            }
            return result.toArray(new File[result.size()]);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Backup solution list local files
        return dir.listFiles();
    }   */

    /*public File[] listFiles(URL dir, FilenameFilter filter) {
        File[] files = listFiles(dir);
        List<File> result = new ArrayList<File>();
        for (File file : files) {
            if (filter.accept(dir, file.getName())) {
                result.add(file);
            }
        }
        return result.toArray(new File[result.size()]);
    } */

    private void loadDynamically(File result, String extraPath) {
        URL url = null;
        InputStream is = null;
        FileOutputStream fos = null;
        try {
            url = new URL(homeServerUrl + extraPath);
            is = url.openStream();
            fos = new FileOutputStream(result);
            byte[] buff = new byte[8192];
            int nbRead;
            while ((nbRead = is.read(buff)) > 0) fos.write(buff, 0, nbRead);
        } catch (IOException e) {
            throw new StellariumException("Cannot dynamically load " + result + " from " + url);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace(System.out);
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace(System.out);
                }
            }
        }
    }
}
