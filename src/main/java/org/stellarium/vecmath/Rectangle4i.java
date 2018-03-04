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
package org.stellarium.vecmath;

import javax.vecmath.Tuple4i;

/**
 * In C++ vector4 is a template that can take int, but in java vecmath only float and double exists.
 *
 * @author Fred Simon
 */
public class Rectangle4i extends Tuple4i {

    public Rectangle4i(int x, int y, int height, int width) {
        super(x, y, height, width);
    }

    public int getHeight() {
        return z;
    }

    public int getWidth() {
        return w;
    }

    public int[] toArray() {
        int[] result = new int[4];
        // Attention: The order is what OpenGL expect (x,y,width,height)
        result[0] = x;
        result[1] = y;
        //Width
        result[2] = w;
        // Height
        result[3] = z;
        return result;
    }
}
