package org.stellarium;

import junit.framework.TestCase;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * @author Jerome Beau
 * @version 27 nov. 2006 00:35:38
 */
public class CommandLineTest extends TestCase {

    public void testVersion() {
        System.setOut(new PrintStream(new ByteArrayOutputStream()) {
            public void println(String outputLine) {
                assertEquals(Main.APP_NAME, outputLine);
            }
        });
        assertEquals(new Integer(0), Main.checkCommandLine(new String[]{"--version"}));
        assertEquals(new Integer(0), Main.checkCommandLine(new String[]{"-v"}));
    }

    public void testHelp() {
        System.setOut(new PrintStream(new ByteArrayOutputStream()) {
            int i = 0;

            public void println(String outputLine) {
                assertEquals(Main.USAGE_TEXT[i++], outputLine);
            }

            public void println() {
                i = 0;
            }
        });
        assertEquals(new Integer(0), Main.checkCommandLine(new String[]{"--help"}));
        System.out.println();
        assertEquals(new Integer(0), Main.checkCommandLine(new String[]{"-h"}));
    }

    public void testWrong() {
        System.setOut(new PrintStream(new ByteArrayOutputStream()) {
            int i = 0;

            public void println(String outputLine) {
                assertEquals(Main.USAGE_TEXT[i++], outputLine);
            }
        });
//        assertEquals(new Integer(1), Main.checkCommandLine(new String[]{"--fkdlfk"}));
//        assertEquals(new Integer(1), Main.checkCommandLine(new String[]{"-fkdlfk"}));
    }
}
