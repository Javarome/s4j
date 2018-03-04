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

//#define PARSE_DEBUG 1

import org.stellarium.StellariumException;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * @see <a href="http://stellarium.cvs.sourceforge.net/stellarium/stellarium/src/command_interface.h?view=markup">C++ header of this file</>
 * @see <a href="http://stellarium.cvs.sourceforge.net/stellarium/stellarium/src/command_interface.cpp?view=markup">C++ implementation of this file</>
 */
abstract class CommandInterface {
    private static final boolean PARSE_DEBUG = false;

    static String parseCommand(String commandLine, Map<String, String> arguments) {
        StringTokenizer commandstr = new StringTokenizer(commandLine);
        String key, value;
//        char nextc;

        String command = commandLine.substring(0, commandLine.indexOf(' '));
        commandstr.nextToken();

        while (commandstr.hasMoreTokens()) {
            key = commandstr.nextToken();
            value = commandstr.nextToken();
            if (value.charAt(0) == '"') {
                if (value.charAt(value.length() - 1) == '"') {
                    // one word in quotes
                    value = value.substring(1, value.length() - 2);
                } else {
                    // multiple words in quotes
                    value = value.substring(1, value.length() - 1);

                    //                    while (true) {
                    //                        nextc = commandstr.get();
                    //                        if (nextc == '"' || !commandstr.good()) {
                    //                            break;
                    //                        }
                    //                        value.add(nextc);
                    //                    }
                }

            }
            key = key.toLowerCase();
            arguments.put(key, value);
        }

        if (PARSE_DEBUG) {
            System.out.print("Command: " + command + "\n" + "Argument hash:");

            for (Map.Entry iter : arguments.entrySet()) {
                System.out.println("\t" + iter.getKey() + " : " + iter.getValue());
            }
        }

        return command;// no error checking yet
    }

    /**
     * for quick testing set to 1 and compile just this file
     */
    //        if (false)
    public static void main(String[] ags) {

        String command = "test";

        String commandline = "flag art on atmosphere off location \"West Virginia\"";

        // if static...
        Map<String, String> args = new HashMap<String, String>();
        parseCommand(commandline, args);
    }

    //        #endif
    public abstract void executeCommand(String commandline) throws StellariumException;
}