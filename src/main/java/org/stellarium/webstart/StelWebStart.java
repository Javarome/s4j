package org.stellarium.webstart;

import org.stellarium.Main;
import org.stellarium.StellariumException;
import org.stellarium.data.ResourceLocatorUtil;
import org.stellarium.data.WebFileLoaderImpl;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by IntelliJ IDEA.
 * User: freds
 * Date: Feb 1, 2008
 * Time: 8:04:49 PM
 * To change this template use File | Settings | File Templates.
 */
public class StelWebStart {
    private final int BUFFER = 2048;

    public static void main(String[] args) {
        ResourceLocatorUtil locatorUtil = ResourceLocatorUtil.getInstance();
        File homeDir = locatorUtil.getHomeDir();
        File targetFolder = null;
        if (homeDir == null) {
            ResourceFolderChooser folderChooser = new ResourceFolderChooser();
            folderChooser.showSelectDialog();
            targetFolder = folderChooser.getResourceFolder();
        } else {
            // use the default home dir
            targetFolder = new File(homeDir, ResourceLocatorUtil.STELLARIUM_DIR_NAME);
        }
        if (targetFolder == null) {
            throw new StellariumException("No target folder for Stellarium 4 java");
        }
        if (!targetFolder.exists()) {
            if (!targetFolder.mkdirs()) {
                throw new StellariumException("Cannot create stellarium for java folder " + targetFolder);
            }
        }

        StelWebStart webStart = new StelWebStart();
        if (args != null && args.length > 0) {
            WebFileLoaderImpl fileLoader = new WebFileLoaderImpl(locatorUtil, args[0]);
            locatorUtil.setFileLoader(fileLoader);
            // TODO: May need some basic download
            // webStart.downloadResources(targetFolder,args[0]);
        } else {
            webStart.extractResources(targetFolder);
        }

        Main.main(new String[]{"--home", targetFolder.getAbsolutePath()});
    }

    public void extractResources(File targetFolder) {
        targetFolder.mkdirs();
        InputStream inputStream = null;
        try {
            inputStream = getClass().getClassLoader().getResourceAsStream("stellarium-resources.zip");
            BufferedOutputStream dest;
            ZipInputStream zis = new
                    ZipInputStream(new BufferedInputStream(inputStream));
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {

                int count;
                byte data[] = new byte[BUFFER];
                // write the files to the disk
                File file = new File(targetFolder, entry.getName());

                if (entry.isDirectory()) {
                    file.mkdirs();
                } else {
                    fireMessage("Extracting: " + file);

                    FileOutputStream fos = new
                            FileOutputStream(file);
                    dest = new
                            BufferedOutputStream(fos, BUFFER);
                    while ((count = zis.read(data, 0, BUFFER))
                            != -1) {
                        dest.write(data, 0, count);
                    }

                    dest.flush();
                    dest.close();
                }
            }
            zis.close();
        } catch (Exception e) {
            fireMessage("Exception while unzip: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("failed to unzip", e);
        } finally {
            try {
                if (inputStream != null) inputStream.close();
            } catch (IOException e) {
                fireMessage("Exception while unzip: " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException("failed to close input stream", e);
            }
        }

    }

    private void fireMessage(String message) {
        System.out.println(message);
    }
}
