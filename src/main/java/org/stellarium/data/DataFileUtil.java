/*
 * User: freds
 * Date: Nov 8, 2006
 * Time: 10:06:38 PM
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
package org.stellarium.data;

import org.stellarium.StellariumException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for all flat file manipulation in Stellarium
 *
 * @author Fred Simon
 */
public class DataFileUtil {

    /**
     * Extract all the lines of a file
     *
     * @param file            - the path of the file to open
     * @param fileDescription - the message if file cannot be read or exception occurs
     * @param exceptionOnOpen
     * @return null if cannot open file, or an array of all the lines
     * @throws StellariumException If exception during the file reading, but not during file opening
     */
    public static List<String> getLines(URL file, String fileDescription, boolean exceptionOnOpen) throws StellariumException {
        List<String> recordLines = new ArrayList<String>();
        LineNumberReader texFile;
        try {
            Charset charset = Charset.forName("UTF-8");
            texFile = new LineNumberReader(new InputStreamReader(file.openStream(), charset));
        } catch (IOException e) {
            String message = "Can't open " + fileDescription + " " + file
                    + " due to:" + e.getMessage();
            if (exceptionOnOpen)
                throw new StellariumException(message, e);
            System.err.println(message);
            return null;
        }

        // determine total number to be loaded for percent complete display
        // And load everything in a String list for future parsing
        try {
            String record;
            while ((record = texFile.readLine()) != null) {
                recordLines.add(record);
            }
            texFile.close();
        } catch (IOException e) {
            throw new StellariumException("Error reading " + fileDescription + " file " + file, e);
        }

        return recordLines;
    }
}
