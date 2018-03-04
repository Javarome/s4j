package org.stellarium;/*
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

// rms added color as parameter

import static org.stellarium.ui.SglAccess.*;

import javax.media.opengl.GL;

class Draw {
    /**
     * Draw a point... (used for tests)
     *
     * @param x
     * @param y
     * @param z
     */
    public static void drawPoint(double x, double y, double z) {
        glColor3d(0.8, 1.0, 0.8);
        glDisable(GL.GL_TEXTURE_2D);
        //glEnable(GL_BLEND);
        glPointSize(20);
        glBegin(GL.GL_POINTS);
        glVertex3d(x, y, z);
        glEnd();
    }
}
