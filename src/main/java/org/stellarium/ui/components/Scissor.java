/*
 * User: freds
 * Date: Nov 24, 2006
 * Time: 11:33:57 PM
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

import org.stellarium.ui.SglAccess;
import org.stellarium.ui.render.SColor;
import org.stellarium.vecmath.SSquare;
import org.stellarium.vecmath.Vector2i;

import javax.media.opengl.GL;
import java.util.LinkedList;


/**
 * Manages the use of the OpenGL Scissor test to prevent drawings outside the
 * components borders. Use a stack like for openGL matrices.
 */
public class Scissor {
    public static final SColor S_WHITE = new SColor(1f, 1f, 1f, 0f);

    public static Scissor scissor;

    public static Painter defaultPainter;

    public Scissor(int someWinW, int someWinH) {
        this.winW = someWinW;
        this.winH = someWinH;
        push(0, 0, winW, winH);
    }

    /**
     * Define a new GlScissor zone relative to the previous one and apply it so
     * that nothing can be drawn outside the limits
     */
    public void push(int posX, int posY, int sizeX, int sizeY) {
        if (stack.isEmpty()) {
            SSquare el = new SSquare(posX, posY, sizeX, sizeY);
            // Apply the new values
            SglAccess.glScissor(el.v0, winH - el.v1 - el.v3, el.v2, el.v3);
            // Add the new value at the end of the stack
            stack.addFirst(el);
        } else {
            SSquare v = stack.getFirst();
            SSquare el = new SSquare(v.v0 + posX, v.v1 + posY, sizeX, sizeY);

            // Check and adjust in case of overlapping
            if (posX + sizeX > v.v2) {
                v.v2 -= posX;
                if (el.v2 < 0) el.v2 = 0;
            }
            if (posY + sizeY > v.v3) {
                el.v3 -= posY;
                if (el.v3 < 0) el.v3 = 0;
            }
            // Apply the new values
            SglAccess.glScissor(el.v0, winH - el.v1 - el.v3, el.v2, el.v3);
            // Add the new value at the end of the stack
            stack.addFirst(el);
        }
    }

    // See above
    public void push(Vector2i pos, Vector2i size) {
        if (stack.isEmpty()) {
            SSquare el = new SSquare(pos.i0, pos.i1, size.i0, size.i1);
            // Apply the new values
            SglAccess.glScissor(el.v0, winH - el.v1 - el.v3, el.v2, el.v3);
            // Add the new value at the end of the stack
            stack.addFirst(el);
        } else {
            SSquare v = stack.getFirst();
            SSquare el = new SSquare(v.v0 + pos.i0, v.v1 + pos.i1, size.i0, size.i1);

            // Check and adjust in case of overlapping
            if (pos.i0 + size.i0 > v.v2) {
                el.v2 = v.v2 - pos.i0;
                if (el.v2 < 0) el.v2 = 0;
            }
            if (pos.i1 + size.i1 > v.v3) {
                el.v3 = v.v3 - pos.i1;
                if (el.v3 < 0) el.v3 = 0;
            }
            // Apply the new values
            SglAccess.glScissor(el.v0, winH - el.v1 - el.v3, el.v2, el.v3);

            // Add the new value at the end of the stack
            stack.add(0, el);
        }
    }

    /**
     * Remove the last element in the stack : ie comes back to the previous GlScissor borders.
     */
    public void pop() {
        // Remove the last value from the stack
        stack.removeFirst();

        if (!stack.isEmpty()) {
            // Apply the previous value
            SSquare v = stack.getFirst();
            SglAccess.glScissor(v.v0, winH - v.v1 - v.v3, v.v2, v.v3);
        }
    }

    public void activate() {
        SglAccess.glEnable(GL.GL_SCISSOR_TEST);
    }

    public void desactivate() {
        SglAccess.glDisable(GL.GL_SCISSOR_TEST);
    }

    private int winW, winH;

    private LinkedList<SSquare> stack = new LinkedList<SSquare>();
}
