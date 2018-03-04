/*
* Stellarium for Java
* Copyright (c) 2005-2006 Jerome Beau
*
* Java adaptation of <a href="http://www.stellarium.org">Stellarium</a>
* The C++ file was:
* This file Copyright (C) 2005 Robert Spearman
*
* This program is free software; you can redistribute it and/or
* modify it under the terms of the GNU General Public License
* as published by the Free Software Foundation; either version 2
* of the License, or (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program; if not, write to the Free Software
* Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
*/
package org.stellarium.command;

import org.stellarium.StelUtility;
import org.stellarium.StellariumException;
import org.stellarium.data.ResourceLocatorUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

/**
 * @author Jerome Beau, Fred Simon
 * @version 0.8.2
 */
public class ScriptMgr {
    private static final String STELLARIUM_FILE_PREFIX = "stellarium";

    private static final String STS_EXTENSION = ".sts";

    public ScriptMgr(StelCommandInterface commandInterface) {
        commander = commandInterface;
        recording = false;
        playing = false;
        recordElapsedTime = 0l;

        // used when scripts are on a CD that needs to be mounted manually
        removeableScriptDirectory = "";
        removeableDirectoryMounted = false;
    }

    /**
     * path is used for loading script assets
     *
     * @param scriptFile
     * @param scriptPath
     */
    void playScript(String scriptFile, String scriptPath) throws StellariumException {
        // load script...

        if (playing) {
            // cancel current script and start next (one script can call another)
            cancelScript();
        }

        setGuiDebug(false);// Default off until script sets otherwise

        script = new Script();

        // if script is on mountable disk, mount that now
        mountIfNeeded(scriptFile);

        try {
            script.load(scriptFile, scriptPath);
        } catch (StellariumException e) {
            cancelScript();
            throw new StellariumException(e);
        }

        playing = true;
        playPaused = false;
        elapsedTime = waitTime = 0;
    }

    private void mountIfNeeded(String scriptFile) throws StellariumException {
        if (!StelUtility.isEmpty(removeableScriptDirectory) &&
                scriptFile.startsWith(removeableScriptDirectory)) {
            System.out.println("MOUNT DISK " + removeableScriptDirectory + " to read script\n");
            ResourceLocatorUtil.getInstance().execScript("script_mount_script_disk");
            removeableDirectoryMounted = true;
        }
    }

    /**
     * delete script object...
     */
    void cancelScript() {
        if (script != null) {
            script.close();
        }
        script = null;
        // images loaded are deleted from stel_command_interface directly
        playing = false;
        playPaused = false;

        unmountIfNeeded();
    }

    private void unmountIfNeeded() {
        if (removeableDirectoryMounted) {
            removeableDirectoryMounted = false;
            System.out.println("UMOUNT DISK " + removeableScriptDirectory + "\n");
            ResourceLocatorUtil.getInstance().execScript("script_mount_script_disk");
        }
    }

    void pauseScript() throws StellariumException {
        playPaused = true;
        // need to pause time as well
        commander.executeCommand("timerate action pause");
    }

    void resumeScript() throws StellariumException {
        if (playPaused) {
            playPaused = false;
            commander.executeCommand("timerate action resume");
        }
    }

    void recordScript(String scriptFilename) {
        // TODO: filename should be selected in a UI window, but until then this works
        if (recording) {
            System.out.println("Already recording script.");
            return;
        }

        File scriptFile;
        if (StelUtility.isEmpty(scriptFilename)) {
            // The script file name is automatically generated to stellariumXXX.sts under
            // My Documents in Windows, Desktop on MacOSX and Home folder on others OS
            File folder = null;

            File userHome = null;
            String userHomeName = System.getProperty("user.home");
            if (userHomeName != null) {
                userHome = new File(userHomeName);
            }

            if (userHome == null || !userHome.exists()) {
                // Only on Windows 98, which does not have good Java5 implementation...
                // If the user home does not exists, use the temp and that's it
                String tmpDir = System.getProperty("java.io.tmpdir");
                folder = new File(tmpDir);
            } else {
                if (ResourceLocatorUtil.isWindows()) {
                    folder = new File(userHome, "My Documents");
                } else if (ResourceLocatorUtil.isMacOSX()) {
                    folder = new File(userHome, "Desktop");
                } else {
                    folder = userHome;
                }
            }

            int currentMax = 0;
            String[] allFiles = folder.list();
            for (int i = 0; i < allFiles.length; i++) {
                String fileName = allFiles[i];
                if (fileName.startsWith(STELLARIUM_FILE_PREFIX) && fileName.endsWith(STS_EXTENSION)) {
                    int fileNb = Integer.parseInt(fileName.substring(STELLARIUM_FILE_PREFIX.length(), fileName.length() - STS_EXTENSION.length()));
                    if (fileNb > currentMax) {
                        currentMax = fileNb;
                    }
                }
            }
            currentMax++;

            scriptFile = new File(folder, STELLARIUM_FILE_PREFIX + currentMax + STS_EXTENSION);
        } else {
            scriptFile = new File(scriptFilename);
        }

        try {
            recFile = new FileWriter(scriptFile);
            recording = true;
            recordElapsedTime = 0;
            recordFileName = scriptFile.getAbsolutePath();
        } catch (IOException e) {
            System.out.println("Error opening script file " + scriptFile + " :" + e.getMessage());
            e.printStackTrace(System.out);
            recordFileName = "";
        }
    }

    public void recordCommand(String commandline) {
        if (recording) {
            // write to file...

            try {
                if (recordElapsedTime != 0) {
                    recFile.write("wait duration " + recordElapsedTime / 1000.f + "\n");
                    recordElapsedTime = 0;
                }

                recFile.write(commandline + "\n");

                // TEMPORARY for debugging
                System.out.println(commandline);
            } catch (IOException e) {
                throw new StellariumException(e);
            }
        }
    }

    void cancelRecordScript() throws StellariumException {
        // close file...
        try {
            recFile.close();
        } catch (IOException e) {
            throw new StellariumException(e);
        }
        recording = false;
        System.out.println("Script recording stopped.");
    }

    /**
     * runs maximum of one command per update
     * note that waits can drift by up to 1/fps seconds
     */
    public void update(long deltaTime) throws StellariumException {
        if (recording) recordElapsedTime += deltaTime;

        if (playing && !playPaused) {

            elapsedTime += deltaTime;// time elapsed since last command (should have been) executed

            if (elapsedTime >= waitTime) {
                // now time to run next command

                //      cout << "dt " << deltaTime << " et: " << elapsed_time << endl;
                elapsedTime -= waitTime;
                long wait = 0;
                String comd = script.nextCommand();
                if (comd != null) {
                    wait = commander.executeCommand(comd, wait, false);// untrusted commands
                    waitTime = wait;
                } else {
                    // script done
                    System.out.println("Script completed.");
                    commander.executeCommand("script action end");
                }
            }
        }
    }

    /**
     * @param directory A given directory
     * @return A list of script files from directory
     * @throws StellariumException If the directory is not an existing directory
     */
    public String getScriptList(String directory) throws StellariumException {
        String result = "";

        // if directory is on mountable disk, mount that now
        mountIfNeeded(directory);

        File dp = new File(directory);
        if (!dp.exists()) {
            throw new StellariumException(directory + " does not exist");
        }
        if (!dp.isDirectory()) {
            throw new StellariumException(directory + " is not a directory");
        }
        String[] files = dp.list();

        // TODO: sort the directory
        for (int i = 0; i < files.length; i++) {
            String tmp = files[i];
            if (tmp.length() > 4 && tmp.indexOf(STS_EXTENSION, tmp.length() - 4) != 0) {
                result += tmp + "\n";
                System.out.println(tmp);
            }
        }

        unmountIfNeeded();

        System.out.println("Result = " + result);
        return result;
    }

    String getScriptPath() {
        if (script != null) {
            return script.getPath();
        } else {
            return "";
        }
    }

    // look for a script called "startup.sts"
    public boolean playStartupScript() {
        // first try on removeable directory
        if (!StelUtility.isEmpty(removeableScriptDirectory)) {
            try {
                playScript(removeableScriptDirectory + "scripts/startup.sts",
                        removeableScriptDirectory + "scripts/");
                return true;
            } catch (StellariumException e) {
                e.printStackTrace();
            }
        }

        // try in stellarium tree
        try {
            String getScriptDirPath = ResourceLocatorUtil.getInstance().getScriptsDir().getAbsolutePath();
            playScript(
                    new File(ResourceLocatorUtil.getInstance().getScriptsDir(), "startup.sts").getAbsolutePath(),
                    getScriptDirPath + "/");
            return true;
        } catch (StellariumException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * @return is a script playing?
     */
    public boolean isPlaying() {
        return playing;
    }

    /**
     * @return is a script paused?
     */
    public boolean isPaused() {
        return playPaused;
    }

    /**
     * @return is a script being recorded?
     */
    public boolean isRecording() {
        return recording;
    }

    /**
     * @return file record is writing to
     */
    public String getRecordFileName() {
        return recordFileName;
    }

    public void setAllowUI(boolean aui) {
        allowUI = aui;
    }

    public boolean isAllowUI() {
        return allowUI;
    }

    public boolean isGuiDebug() {
        return guiDebug;
    }

    /**
     * Should script errors be shown onscreen?
     *
     * @param guiDebug
     */
    public void setGuiDebug(boolean guiDebug) {
        this.guiDebug = guiDebug;
    }

    /**
     * for executing script commands
     */
    private StelCommandInterface commander;

    /**
     * currently loaded script
     */
    private Script script;

    /**
     * ms since last script command executed
     */
    private long elapsedTime;

    /**
     * ms until next script command should be executed
     */
    private long waitTime;

    /**
     * ms since last command recorded
     */
    private long recordElapsedTime;

    /**
     * is a script being recorded?
     */
    private boolean recording;

    /**
     * is a script playing?  (could be paused)
     */
    private boolean playing;

    /**
     * is script playback paused?
     */
    private boolean playPaused;

    private Writer recFile;

    private String recordFileName;

    private String removeableScriptDirectory;

    private boolean removeableDirectoryMounted;

    /**
     * Allow user interface to function during scripts
     * (except for time related keys which control script playback)
     */
    private boolean allowUI;

    private boolean guiDebug;
}