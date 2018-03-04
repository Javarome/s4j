/*
 * Copyright (C) 2006 Frederic Simon
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

import org.ini4j.IniFile;
import org.stellarium.StellariumException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * See the original C++ version:
 * URL: https://svn.sourceforge.net/svnroot/stellarium/tags/stellarium-0-8-1/stellarium/src/init_parser.cpp
 * Last Changed Author: gajdosik
 * Last Changed Rev: 1214
 * <p/>
 * C++ comments: Class which parse an ini file and provide methods original to the C++ init_parser.cpp file of stellarium.
 * <p/>
 * In Java this class is wrapping the ini4j free library ( http://ini4j.sourceforge.net/ )
 */
public class IniFileParser {

    public static final String VIDEO_SECTION = "video";
    public static final String SCREEN_WIDTH = "screen_w";
    public static final String SCREEN_HEIGHT = "screen_h";

    public static final String PROJECTION_SECTION = "projection";
    public static final String VIEWPORT = "viewport";
    public static final String DISK_VIEWPORT = "disk";
    public static final String MAXIMIZED_VIEWPORT = "maximized";
    public static final String MOVE_MOUSE_ENABLED = "flag_enable_move_mouse";

    public static enum projection {viewport, type}

    public static final String FULLSCREEN = "fullscreen";
    public static final String LATITUDE = "latitude";
    public static final String LONGITUDE = "longitude";
    public static final String NAME = "name";

    public static final String ASTRO_SECTION = "astro";
    public static final String LANDSCAPE_SECTION = "landscape";
    public static final String STARS_SECTION = "stars";
    public static final String NAVIGATION_SECTION = "navigation";
    public static final String GUI_SECTION = "gui";
    public static final String TEXT_UI_SECTION = "tui";
    public static final String VIEWING_SECTION = "viewing";

    public static final String LOCALIZATION_SECTION = "localization";
    public static final String INFO_SECTION = "info";
    public static final String STAR_SCALE = "star_scale";
    public static final String SPHERIC_MIRROR_SECTION = "spheric_mirror";
    public static final String HORIZONTAL_OFFSET = "horizontal_offset";
    public static final String VERTICAL_OFFSET = "vertical_offset";
    public static final String TYPE = "type";
    public static final String BBP_MODE = "bbp_mode";
    public static final String CHART_ENABLED = "flag_chart";
    public static final String NIGHT_MODE = "flag_night";
    public static final String DISTORTER = "distorter";
    public static final String SHOW_MENU = "flag_menu";
    public static final String SHOW_TOPBAR = "flag_show_topbar";
    public static final String SHOW_TIME = "flag_show_time";
    public static final String SHOW_DATE = "flag_show_date";
    public static final String SHOW_APPLICATION_NAME = "flag_show_appname";
    public static final String SHOW_FIELD_OF_VIEW = "flag_show_fov";
    public static final String SHOW_FRAMES_PER_SECOND = "flag_show_fps";
    public static final String BASE_FONT_SIZE = "base_font_size";
    public static final String BASE_FONT_NAME = "base_font_name";
    public static final String SHOW_TEXT_MENU = "flag_enable_tui_menu";
    public static final String MOUSE_CURSOR_TIMEOUT = "mouse_cursor_timeout";
    public static final String SHOW_SELECTION_INFO = "flag_show_selected_object_info";
    public static final String SHOW_FLIP_BUTTONS = "flag_show_flip_buttons";
    public static final String MOUSE_ZOOM_FACTOR = "mouse_zoom";

    private IniFile _iniFile;

    public IniFileParser(Class component, File iniFile) throws StellariumException {
        this(component, iniFile, IniFile.Mode.RO);
    }

    public IniFileParser(Class component, URL iniFile) throws StellariumException {
        this(component, iniFile, IniFile.Mode.RO);
    }

    public IniFileParser(Class component, Object iniFile, IniFile.Mode mode) throws StellariumException {
        //setAsPreferences(component, iniFile);
        try {
            _iniFile = new IniFile(iniFile, mode);
        } catch (BackingStoreException e) {
            throw new StellariumException(e);
        }
    }

    public String[] getSectionNames() throws StellariumException {
        try {
            return _iniFile.keys();
        } catch (BackingStoreException e) {
            throw new StellariumException(e);
        }
    }

    public Preferences getSection(String section) {
        return _iniFile.node(section);
    }

    public String getStr(String secName, String key) {
        return _iniFile.node(secName).get(key, null);
    }

    public String getStr(String secName, String key, String def) {
        return _iniFile.node(secName).get(key, def);
    }

    public double getDouble(String secName, String key) {
        return _iniFile.node(secName).getDouble(key, 0.0d);
    }

    public double getDouble(String secName, String key, double def) {
        return _iniFile.node(secName).getDouble(key, def);
    }

    public boolean getBoolean(String secName, String key) {
        return _iniFile.node(secName).getBoolean(key, false);
    }

    public boolean getBoolean(String secName, String key, boolean def) {
        return _iniFile.node(secName).getBoolean(key, def);
    }

    public int getInt(String section, String s) {
        return getInt(section, s, 0);
    }

    public int getInt(String secName, String key, int def) {
        return _iniFile.node(secName).getInt(key, def);
    }

    public boolean findEntry(String section) throws StellariumException {
        try {
            return _iniFile.nodeExists(section);
        } catch (BackingStoreException e) {
            throw new StellariumException(e);
        }
    }

    public void setStr(String section, String key, String value) {
        _iniFile.node(section).put(key, value);
    }

    public void flush() throws StellariumException {
        try {
            _iniFile.flush();
        } catch (BackingStoreException e) {
            throw new StellariumException(e);
        }
    }

    private static void setAsPreferences(Class component, File resourceToConvert) {
        try {
            BufferedReader inputReader = new BufferedReader(new FileReader(resourceToConvert));
            Preferences preferences = Preferences.userNodeForPackage(component);
            String section;
            Preferences sectionNode = null;
            while (inputReader.ready()) {
                String line = inputReader.readLine().trim();
                if (line == null) {
                    break;
                } else if (line.startsWith("[")) {
                    section = line.substring(1, line.indexOf("]"));
                    sectionNode = preferences.node(section);
                } else if (line.length() > 2 && !line.startsWith("#")) {
                    int sep = line.indexOf("=");
                    if (sep > 0) {
                        String key = line.substring(0, sep).trim();
                        String value = line.substring(sep + 1).trim();
                        System.out.println(sectionNode + " " + key + "=" + value);
                        if (Boolean.TRUE.toString().equals(value) || Boolean.FALSE.toString().equals(value)) {
                            sectionNode.putBoolean(key, Boolean.valueOf(value));
                        } else {
                            try {
                                double d = Integer.parseInt(value);
                                sectionNode.putInt(key, Integer.valueOf(value));
                            } catch (NumberFormatException ei) {
                                try {
                                    double d = Double.parseDouble(value);
                                    sectionNode.putDouble(key, Double.valueOf(value));
                                } catch (NumberFormatException ed) {
                                    sectionNode.put(key, value);
                                }
                            }
                            //                           assert sectionNode.get(key, "").equals(value) : sectionNode + " should have value " + value + " for key " + key + " but had " + sectionNode.get(key, "");
                        }
                        sectionNode.flush();
                    }
                }
            }
            preferences.flush();

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (BackingStoreException e) {
            throw new RuntimeException(e);
        }
    }
}
