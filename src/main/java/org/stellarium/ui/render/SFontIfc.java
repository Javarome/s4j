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
package org.stellarium.ui.render;

import java.awt.*;

public interface SFontIfc {
    /**
     * Method with upsideDown by default
     *
     * @param str
     * @param x
     * @param y
     */
    void print(int x, int y, String str);

    void print(int x, int y, String str, boolean upsidedown);

    void printChar(char c);

    void printCharOutlined(char c);

    int getStrLen(String str);

    int getLineHeight();

    int getAscent();

    int getDescent();

    void close();

    void print(int x, int y, String str, boolean upsidedown, SColor color);

    void print(int x, int y, String str, boolean upsidedown, Color color);

    void print(int i, int i1, String str, boolean b, Color interColor, float angle);
}