/*
* Stellarium
* Copyright (C) 2002 Fabien Chereau
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

import javax.vecmath.Tuple4f;
import javax.vecmath.Vector3d;


public class SColor extends Tuple4f {
    public SColor(float v, float v1, float v2, float v3) {
        super(v, v1, v2, v3);
    }

    public SColor(float v, float v1, float v2) {
        super(v, v1, v2, 1f);
    }

    public SColor(float[] floats) {
        super(floats);
    }

    public SColor(Tuple4f t1) {
        super(t1);
    }

    public SColor mul(float m) {
        float[] floatValues = new float[4];
        get(floatValues);
        for (int i = 0; i < floatValues.length; i++) {
            floatValues[i] *= m;
        }
        return new SColor(floatValues);
    }

    public SColor div(int d) {
        float[] floatValues = new float[4];
        get(floatValues);
        for (int i = 0; i < floatValues.length; i++) {
            floatValues[i] /= d;
        }
        return new SColor(floatValues);
    }

    public float[] toFloats() {
        float[] floats = new float[4];
        get(floats);
        return floats;
    }

    public Vector3d toVector3d() {
        return new Vector3d(x, y, z);
    }
}
