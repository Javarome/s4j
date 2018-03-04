/*
 * User: freds
 * Date: Nov 24, 2006
 * Time: 11:39:34 PM
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

import org.stellarium.ui.render.SColor;
import org.stellarium.ui.render.SFontIfc;
import org.stellarium.ui.render.STexture;
import org.stellarium.vecmath.Vector2i;

import java.awt.event.*;


/**
 * Mother class for every SGUI object.
 */
public abstract class StellariumComponent extends KeyAdapter implements MouseListener, MouseMotionListener, MouseWheelListener {
    /**
     * GUI return values
     */
    public static enum S_GUI_VALUE {
        S_GUI_MOUSE_LEFT,
        S_GUI_MOUSE_RIGHT,
        S_GUI_MOUSE_MIDDLE,
        S_GUI_PRESSED,
        S_GUI_RELEASED
    }

    protected StellariumComponent() {
    }

    public void reshape(Vector2i _pos, Vector2i _size) {
        pos = _pos;
        size = _size;
    }

    public void reshape(int x, int y, int w, int h) {
        pos.set(x, y);
        size.set(w, h);
    }

    public boolean isIn(int x, int y) {
        return (pos.i0 <= x && (size.i0 + pos.i0) >= x && pos.i1 <= y && (pos.i1 + size.i1) >= y);
    }

    public static void initScissor(int winW, int winH) {
        scissor = new Scissor(winW, winH);
    }

    public static void deleteScissor() {
        scissor = null;
    }

    public abstract void draw();

    int getPosx() {
        return pos.i0;
    }

    int getPosy() {
        return pos.i1;
    }

    public int getSizeX() {
        return size.i0;
    }

    int getSizeY() {
        return size.i1;
    }

    public void setSizeX(int v) {
        size.i0 = v;
    }

    void setSizey(int v) {
        size.i1 = v;
    }

    void setPosx(int v) {
        pos.i0 = v;
    }

    void setPosy(int v) {
        pos.i1 = v;
    }

    Vector2i getPos() {
        return pos;
    }

    public Vector2i getSize() {
        return size;
    }

    public void setPos(Vector2i _pos) {
        pos = _pos;
    }

    public void setSize(Vector2i _size) {
        size = _size;
    }

    public void setPos(int x, int y) {
        pos.i0 = x;
        pos.i1 = y;
    }

    public void setSize(int w, int h) {
        size.i0 = w;
        size.i1 = h;
    }

    public void setVisible(boolean _visible) {
        visible = _visible;
    }

    boolean getVisible() {
        return visible;
    }

    void setActive(boolean _active) {
        active = _active;
    }

    boolean getActive() {
        return active;
    }

    void setFocus(boolean _focus) {
        focus = _focus;
    }

    boolean getFocus() {
        return focus;
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseMoved(MouseEvent e) {
    }

    public void keyPressed(KeyEvent e) {
    }

    public void setTexture(STexture tex) {
        painter.setTexture(tex);
    }

    void setFont(SFontIfc f) {
        painter.setFont(f);
    }

    SFontIfc getFont() {
        return painter.getFont();
    }

    void setTextColor(SColor c) {
        painter.setTextColor(c);
    }

    void setBaseColor(SColor c) {
        painter.setBaseColor(c);
    }

    public void setPainter(Painter p) {
        painter = new Painter(p);
    }

    public static void setDefaultPainter(Painter p) {
        defaultPainter = p;
    }

    public static void enableScissor() {
        scissor.activate();
    }

    public static void disableScissor() {
        scissor.desactivate();
    }

    public boolean inFront() {
        return moveToFront;
    }

    public boolean getNeedNewEdit() {
        return needNewTopEdit;
    }

    public void setNeedNewEdit(boolean _b) {
        needNewTopEdit = _b;
    }

    public void setInFront(boolean b) {
        moveToFront = b;
    }// signals this component to move to front

    public void setOpaque(boolean b) {
        painter.setOpaque(b);
    }

    public int getType() {
        return type;
    }

    public void setColorScheme(SColor baseColor, SColor textColor) {
        if (guiColorSchemeMember) {
            painter.setTextColor(textColor);
            painter.setBaseColor(baseColor);
        }
    }

    public void setGUIColorSchemeMember(boolean _b) {
        guiColorSchemeMember = _b;
    }

    protected Vector2i pos = new Vector2i();

    protected Vector2i size = new Vector2i();

    protected boolean visible;

    protected boolean active;

    protected boolean focus;

    protected boolean focusing;

    protected Painter painter = new Painter(defaultPainter);

    protected static SColor guiBaseColor;

    protected static SColor guiTextColor;

    protected boolean guiColorSchemeMember;

    protected static Painter defaultPainter;

    protected static Scissor scissor;

    protected boolean moveToFront;

    protected boolean needNewTopEdit;

    protected int type;

    protected boolean desktop;

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mouseDragged(MouseEvent e) {
    }

    public void mouseWheelMoved(MouseWheelEvent e) {
    }
}
