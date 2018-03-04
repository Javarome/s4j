package org.stellarium.command;

/*
* Stellarium
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

import org.stellarium.StellariumException;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * @author Jerome Beau, Fred Simon
 * @version 0.8.2
 */
class Script {
    Script() {
        inputFile = null;
        path = "";
    }

    public void close() {
        if (inputFile != null) {
            try {
                inputFile.close();
            } catch (IOException e) {
                System.out.println("Ignoring closing file error " + e);
                e.printStackTrace(System.out);
            }
        }
    }

    void load(String scriptFile, String scriptPath) throws StellariumException {
        try {
            inputFile = new BufferedReader(new FileReader(scriptFile));
            path = scriptPath;

            // TODO check first line of file for script identifier... ?
        } catch (FileNotFoundException e) {
            throw new StellariumException("Could not find script file " + scriptFile, e);
        }
    }

    /**
     * @return The next executed command, or null if there wasn't any left.
     * @throws StellariumException If an I/O error occured while reading the next command.
     */
    String nextCommand() throws StellariumException {
        try {
            String buffer;
            do {
                buffer = inputFile.readLine();
                if (buffer != null) {
                    if (buffer.charAt(0) != '#') {
                        //      printf("Buffer is: %s\n", buffer);
                        return buffer;
                    }
                }
            } while (buffer != null);
        } catch (IOException e) {
            throw new StellariumException(e);
        }
        return null;
    }

    String getPath() {
        return path;
    }

    private BufferedReader inputFile;

    private String path;
}
