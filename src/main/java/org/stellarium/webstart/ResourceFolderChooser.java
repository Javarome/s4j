package org.stellarium.webstart;

import javax.swing.*;
import java.io.File;
import java.util.prefs.Preferences;

public class ResourceFolderChooser {

    public static final String LAST_FILE_PREF_KEY = "last-file";

    private File resourceFolder;

    public File getResourceFolder() {
        return resourceFolder;
    }

    public void showSelectDialog() {
        Preferences preferences = Preferences.userNodeForPackage(ResourceFolderChooser.class);
        String lastFolderSelected = preferences.get(LAST_FILE_PREF_KEY, null);

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        if (lastFolderSelected != null) {
            fileChooser.setSelectedFile(new File(lastFolderSelected));
        }

        fileChooser.setDialogTitle("Folder to extract graphic and data resources");
        fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
        fileChooser.setFileHidingEnabled(true);
        fileChooser.setMultiSelectionEnabled(false);
        int state = fileChooser.showOpenDialog(null);

        if (state == JFileChooser.CANCEL_OPTION) {
            return;
        }

        if (state == JFileChooser.ERROR_OPTION) {
            JOptionPane.showMessageDialog(null, "Unknown error!");
            return;
        }

        this.resourceFolder = fileChooser.getSelectedFile();
        preferences.put(LAST_FILE_PREF_KEY, this.resourceFolder.getAbsolutePath());
    }
}
