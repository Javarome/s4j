/*
 * User: freds
 * Date: Nov 25, 2006
 * Time: 12:02:51 AM
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
package org.stellarium.ui.components;

import org.stellarium.vecmath.Vector2i;
import org.stellarium.vecmath.Vector4i;


/**
 * Container with a frame around it
 */
class FramedContainer extends StelContainer {

    public FramedContainer() {
        inside = new StelContainer();
        inside.reshape(frameSize.v0, frameSize.v3, 10, 10);
        childs.add(0, inside);
    }

    public void draw() {
        if (!visible) return;
        painter.drawSquareEdge(pos, size);
        painter.drawSquareEdge(inside.getPos().minus(new Vector2i(1, 1)).plus(pos), inside.getSize().plus(new Vector2i(2, 2)));

        draw();
    }

    public void reshape(Vector2i somePos, Vector2i someSize) {
        pos = somePos;
        inside.setSize(someSize);
        size = someSize.plus(new Vector2i(frameSize.v0 + frameSize.v1, frameSize.v2 + frameSize.v3));
    }

    public void reshape(int x, int y, int w, int h) {
        pos = new Vector2i(x, y);
        inside.setSize(new Vector2i(w, h));
        size = new Vector2i(w, h).plus(new Vector2i(frameSize.v0 + frameSize.v1, frameSize.v2 + frameSize.v3));
    }

    public void setSize(Vector2i someSize) {
        inside.setSize(someSize);
        size = someSize.plus(new Vector2i(frameSize.v0 + frameSize.v1, frameSize.v2 + frameSize.v3));
    }

    public void setSize(int w, int h) {
        inside.setSize(new Vector2i(w, h));
        size = new Vector2i(w, h).plus(new Vector2i(frameSize.v0 + frameSize.v1, frameSize.v2 + frameSize.v3));
    }

    public void setFrameSize(int left, int right, int bottom, int top) {
        frameSize.v0 = left;
        frameSize.v1 = right;
        frameSize.v2 = bottom;
        frameSize.v3 = top;
        inside.setPos(left, top);
        size = inside.getSize().plus(new Vector2i(left + right, bottom + top));
    }

    public void addComponent(StellariumComponent c) {
        inside.addComponent(c);
    }

    public int getSizeX() {
        return inside.getSizeX();
    }

    public int getSizeY() {
        return inside.getSizeY();
    }

    public Vector2i getSize() {
        return inside.getSize();
    }

    protected StelContainer inside;

    protected Vector4i frameSize = new Vector4i(3, 3, 3, 3);
}
