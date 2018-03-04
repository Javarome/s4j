/*
 * User: freds
 * Date: Nov 24, 2006
 * Time: 11:30:39 PM
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


public class Vector2i {
    public int i0;

    public int i1;

    public Vector2i() {
        this(0, 0);
    }

    public Vector2i(int i0, int i1) {
        set(i0, i1);
    }

    public void set(int i0, int i1) {
        this.i0 = i0;
        this.i1 = i1;
    }

    public Vector2i plus(Vector2i y) {
        return new Vector2i(i0 + y.i0, i1 + y.i1);
    }

    public Vector2i minus(Vector2i y) {
        return new Vector2i(i0 - y.i0, i1 - y.i1);
    }

    public Vector2i div(int d) {
        return new Vector2i(i0 / d, i1 / d);
    }
}
