/*
 * User: freds
 * Date: Nov 24, 2006
 * Time: 11:35:44 PM
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

import static org.stellarium.ui.SglAccess.*;
import org.stellarium.ui.render.SColor;
import org.stellarium.ui.render.SFontIfc;
import org.stellarium.ui.render.STexture;
import org.stellarium.vecmath.Vector2i;

import javax.media.opengl.GL;


/**
 * Class used to manage all the drawings for a component. Stores informations
 * like colors or used textures. Performs the primitives drawing.
 */
public class Painter {
    public Painter(Painter p) {
        textColor = p.textColor;
        baseColor = p.baseColor;
        font = p.font;
        tex1 = p.tex1;
    }

    public Painter(STexture someTex1, SFontIfc someFont, SColor someBaseColor, SColor someTextColor) {
        textColor = someTextColor;
        baseColor = someBaseColor;
        font = someFont;
        tex1 = someTex1;
    }

    /**
     * Draw the edges of the defined square with the default base color
     */
    public void drawSquareEdge(Vector2i pos, Vector2i sz) {
        float[] color = new float[4];
        baseColor.set(color);
        glColor4fv(color, 0);
        glEnable(GL.GL_BLEND);
        glDisable(GL.GL_TEXTURE_2D);
        glBegin(GL.GL_LINE_LOOP);
        glVertex2f(pos.i0 + 0.5f, pos.i1 + 0.5f);
        glVertex2f(pos.i0 + sz.i0 - 0.5f, pos.i1 + 0.5f);
        glVertex2f(pos.i0 + sz.i0 - 0.5f, pos.i1 + sz.i1 - 0.5f);
        glVertex2f(pos.i0 + 0.5f, pos.i1 + sz.i1 - 0.5f);
        glEnd();
    }

    /**
     * Draw the edges of the defined square with the given color
     */
    public void drawSquareEdge(Vector2i pos, Vector2i sz, SColor c) {
        float[] cValues = new float[4];
        c.get(cValues);
        glColor4fv(cValues, 0);
        glEnable(GL.GL_BLEND);
        glDisable(GL.GL_TEXTURE_2D);
        glBegin(GL.GL_LINE_LOOP);
        glVertex2f(pos.i0 + 0.5f, pos.i1 + 0.5f);
        glVertex2f(pos.i0 + sz.i0 - 0.5f, pos.i1 + 0.5f);
        glVertex2f(pos.i0 + sz.i0 - 0.5f, pos.i1 + sz.i1 - 0.5f);
        glVertex2f(pos.i0 + 0.5f, pos.i1 + sz.i1 - 0.5f);
        glEnd();
    }

    /**
     * Fill the defined square with the default texture and default base color
     */
    public void drawSquareFill(Vector2i pos, Vector2i sz) {
        float[] color = new float[4];
        baseColor.set(color);
        float[] baseColorValues = new float[3];
        baseColor.get(baseColorValues);
        glColor4fv(baseColorValues, 0);
        displayTex1(pos, sz);
    }

    private void displayTex1(Vector2i pos, Vector2i sz) {
        glEnable(GL.GL_TEXTURE_2D);
        glEnable(GL.GL_BLEND);
        glBindTexture(GL.GL_TEXTURE_2D, tex1.getID());
        tex1.displayTexture(pos.i0, pos.i1, sz.i0, sz.i1);
    }

    /**
     * Fill the defined square with the default texture and given color
     */
    public void drawSquareFill(Vector2i pos, Vector2i sz, SColor c) {
        float[] color = new float[4];
        c.set(color);
        glColor4fv(color, 0);
        displayTex1(pos, sz);
    }

    /**
     * Fill the defined square with the given texture and given color
     */
    public void drawSquareFill(Vector2i pos, Vector2i sz, SColor c, STexture t) {
        float[] color = new float[4];
        c.set(color);
        glColor4fv(color, 0);
        displayTex1(pos, sz);
    }

    /**
     * Draw a cross with the default base color
     */
    public void drawCross(Vector2i pos, Vector2i sz) {
        float[] color = new float[4];
        baseColor.set(color);
        glColor4fv(color, 0);
        glEnable(GL.GL_BLEND);
        glDisable(GL.GL_TEXTURE_2D);
        glBegin(GL.GL_LINES);
        glVertex2f(pos.i0 + 0.5f, pos.i1 + 0.5f);
        glVertex2f(pos.i0 + sz.i0 - 0.5f, pos.i1 + sz.i1 - 0.5f);
        glVertex2f(pos.i0 + sz.i0 - 0.5f, pos.i1 + 0.5f);
        glVertex2f(pos.i0 + 0.5f, pos.i1 + sz.i1 - 0.5f);
        glEnd();
    }

    /**
     * Print the text with the default font and default text color
     */
    public void print(int x, int y, String str) {
        print(x, y, str, textColor);
    }

    /**
     * Print the text with the default font and a given text color
     */
    public void print(int x, int y, String str, SColor c) {
        /*float[] color = new float[4];
        c.set(color);
        glColor4fv(color, 0);
        glEnable(GL.GL_TEXTURE_2D);
        glEnable(GL.GL_BLEND);*/
        boolean upsideDown = false; // 0 for upside down mode
        font.print(x, y, str, upsideDown, c);
    }

    public void drawLine(Vector2i pos1, Vector2i pos2) {
        drawLine(pos1, pos2, baseColor);
    }

    public void drawLine(Vector2i pos1, Vector2i pos2, SColor c) {
        float[] cValues = new float[4];
        c.get(cValues);
        glColor4fv(cValues, 0);
        glEnable(GL.GL_BLEND);
        glDisable(GL.GL_TEXTURE_2D);
        glBegin(GL.GL_LINES);
        glVertex2f(pos1.i0 + 0.5f, pos1.i1 + 0.5f);
        glVertex2f(pos2.i0 + 0.5f, pos2.i1 + 0.5f);
        glEnd();
    }

    public void setTexture(STexture tex) {
        tex1 = tex;
    }

    public void setFont(SFontIfc f) {
        font = f;
    }

    public void setTextColor(SColor c) {
        textColor = c;
    }

    public void setBaseColor(SColor c) {
        baseColor = c;
    }

    public SColor getBaseColor() {
        return baseColor;
    }

    public SColor getTextColor() {
        return textColor;
    }

    public SFontIfc getFont() {
        return font;
    }

    public void setOpaque(boolean opaque) {
        this.opaque = opaque;
    }

    private STexture tex1;

    private SFontIfc font;

    private SColor baseColor;

    private SColor textColor;

    private boolean opaque;
}
