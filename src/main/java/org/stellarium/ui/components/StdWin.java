/*
 * User: freds
 * Date: Nov 25, 2006
 * Time: 12:03:24 AM
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

import org.stellarium.ui.render.SFontIfc;
import org.stellarium.ui.render.STexture;
import org.stellarium.vecmath.Vector2i;

import java.awt.event.MouseEvent;


/**
 * Standard window widget
 */
public class StdWin extends FramedContainer {
    public StdWin(String someTitle, STexture someHeaderTex, SFontIfc someFont, int headerSize) {
        if (someHeaderTex != null) {
            headerTex = someHeaderTex;
        }
        if (someFont != null) {
            painter.setFont(someFont);
        }
        setFrameSize(1, 1, 1, headerSize);
        titleLabel = new StelLabel(someTitle);
        setTitle(someTitle);
        addComponent(titleLabel);
    }

    void setTitle(String someTitle) {
        titleLabel.setLabel(someTitle);
        titleLabel.adjustSize();
    }

    public void draw() {
        if (!visible) return;
        titleLabel.setPos((size.i0 - titleLabel.getSizeX()) / 2, (frameSize.v3 - titleLabel.getSizeY()) / 2 + 1);
        painter.drawSquareFill(pos, size);
        painter.drawSquareFill(pos, new Vector2i(size.i0, frameSize.v3));
        draw();
    }

    public void mouseReleased(MouseEvent e) {
        if (visible) {
            super.mouseReleased(e);
            if (e.isConsumed()) {
                return;
            }
            int bt = e.getButton();
            if (bt == MouseEvent.BUTTON1) {
                dragging = false;
            }
        }
    }

    public void mouseClicked(MouseEvent e) {
        if (visible) {
            super.mouseClicked(e);
            if (e.isConsumed()) {
                return;
            }
            int x = e.getX();
            int y = e.getY();
            if (isIn(x, y)) {
                int bt = e.getButton();
                if (bt == MouseEvent.BUTTON1) {
                    dragging = true;
                    oldpos.set(x, y);
                }
                e.consume();
            }
        }
    }

    public void mouseMoved(MouseEvent e) {
        if (visible) {
            super.mouseMoved(e);
            if (e.isConsumed()) {
                return;
            }
            if (dragging) {
                int x = e.getX();
                int y = e.getY();
                pos = pos.plus(new Vector2i(x, y).minus(oldpos));
                oldpos.set(x, y);
            }
        }
    }

    String getTitle() {
        return titleLabel.getLabel();
    }

    protected StelLabel titleLabel;

    protected STexture headerTex;

    protected boolean dragging;

    protected Vector2i oldpos;
}
