/*
 * User: freds
 * Date: Nov 24, 2006
 * Time: 11:57:39 PM
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

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.LinkedList;


/**
 * Manages hierarchical components : send signals ad actions  to childrens
 */
public class StelContainer extends CallbackComponent {

    public StelContainer(boolean isDesktop) {
        super();
        type += SGUI.CT_CONTAINER;
        desktop = isDesktop;
    }

    public StelContainer() {
        this(false);
    }

    public void addComponent(StellariumComponent c) {
        childs.addFirst(c);
    }

    public void removeComponent(StellariumComponent c) {
        for (StellariumComponent iter : childs) {
            if (iter == c) {
                break;
            }
            if (iter == childs.getLast()) return;
            childs.remove(iter);
        }
    }

    public void removeAllComponents() {
        childs.clear();
    }

    public void setFocus(boolean _focus) {
        // set the focus to all children
        for (StellariumComponent iter : childs) {
            iter.setFocus(_focus);
        }
        focus = _focus;
        focusing = _focus;
    }

    public void setColorScheme(final SColor _baseColor, final SColor _textColor) {
        if (!guiColorSchemeMember) return;

        for (StellariumComponent iter : childs) {
            iter.setColorScheme(_baseColor, _textColor);
        }
        painter.setTextColor(_textColor);
        painter.setBaseColor(_baseColor);
    }

    public void draw() {
        if (visible) {
            SglAccess.glPushMatrix();
            SglAccess.glTranslatef(pos.i0, pos.i1, 0.f);

            scissor.push(pos, size);

            for (StellariumComponent iter : childs) {
                iter.draw();
            }

            scissor.pop();

            SglAccess.glPopMatrix();
        }
    }

    public void mouseClicked(MouseEvent e) {
        if (visible) {
            for (StellariumComponent iter : childs) {
                int x = e.getX() - pos.i0;
                int y = e.getY() - pos.i1;
                MouseEvent itemE = new MouseEvent((java.awt.Component) e.getSource(), e.getID(), e.getWhen(), e.getModifiers(), x, y, e.getClickCount(), e.isPopupTrigger(), e.getButton());
                iter.mouseClicked(itemE);
                if (itemE.isConsumed()) {
                    // The signal has been intercepted
                    // Set the component in first position in the objects list
                    childs.addFirst(iter);
                    childs.remove(iter);
                    e.consume();
                }
            }
            int x = e.getX();
            int y = e.getY();
            int button = e.getButton();
            callbackOnClic(x, y, button, S_GUI_VALUE.S_GUI_PRESSED);
        }
    }

    public void mouseMoved(MouseEvent e) {
        if (visible) {
            int x = e.getX();
            int y = e.getY();
            for (StellariumComponent iter : childs) {
                MouseEvent iterEvent = new MouseEvent((java.awt.Component) e.getSource(), e.getID(), e.getWhen(), e.getModifiers(), x - pos.i0, y - pos.i1, e.getClickCount(), e.isPopupTrigger(), e.getButton());
                iter.mouseMoved(iterEvent);// The signal may have been intercepted
            }
            callbackOnMove(x, y);
        }
    }

    public void keyPressed(KeyEvent e) {
        if (!visible) return;
        for (StellariumComponent iter : childs) {
            iter.keyPressed(e);
            if (e.isConsumed()) return;// The signal has been intercepted
        }
    }

    /**
     * Container filled with a texture and with an edge
     */
    void Filleddraw() {
        if (!visible) return;
        painter.drawSquareFill(pos, size);
        //painter.drawSquareEdge(pos, size);

        draw();
    }

    LinkedList<StellariumComponent> childs = new LinkedList<StellariumComponent>();
}
