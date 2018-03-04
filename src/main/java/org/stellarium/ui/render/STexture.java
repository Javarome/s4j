/*
 * This file is part of Stellarium for Java, Copyright (c) 2005 Jerome Beau
 * and is a Java version of the original Stellarium C++ version,
 * User: freds
 * Date: Nov 9, 2006
 * Time: 12:35:15 AM
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

/**
 *
 */
public interface STexture {
    int TEX_LOAD_TYPE_PNG_ALPHA = 0;

    int TEX_LOAD_TYPE_PNG_SOLID = 1;

    int TEX_LOAD_TYPE_PNG_BLEND3 = 2;

    int TEX_LOAD_TYPE_PNG_BLEND8 = 5;

    int TEX_LOAD_TYPE_PNG_BLEND4 = 6;

    int TEX_LOAD_TYPE_PNG_BLEND1 = 7;

    int TEX_LOAD_TYPE_PNG_REPEAT = 3;

    int TEX_LOAD_TYPE_PNG_SOLID_REPEAT = 4;

    /**
     * @return the width in pixels
     */
    int getWidth();

    /**
     * @return the average texture luminance : 0 is black, 1 is white
     */
    float getAverageLuminance();

    /**
     * @return the pointer ID used in OpenGL
     */
    int getID();

    int[] getDimensions();

    void close();

    String getTextureName();

    void displayTexture(double x, double y, double width, double height);
}