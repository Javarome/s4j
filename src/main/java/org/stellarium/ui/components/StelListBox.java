/*
 * User: freds
 * Date: Nov 25, 2006
 * Time: 12:02:30 AM
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

import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;


public class StelListBox extends StellariumComponent {

    private static final int LISTBOX_ITEM_HEIGHT = (Button.BUTTON_HEIGHT - 6);

    public StelListBox(int _displayLines) {
        displayLines = _displayLines;
        scrollBar.setVisible(false);
        scrollBar.setElementsForBar(displayLines);
    }

    public void setOnChangeCallback(StelCallback c) {
        onChangeCallback = c;
    }

    public void setCurrent(String ws) {
        int i = firstItemIndex;
        for (LabeledButton labeledButton : itemBt) {
            if (ws.equals(labeledButton.getLabel())) {
                value = i;
                break;
            }
            i++;
        }
    }

    public void mouseClicked(MouseEvent e) {
        if (visible) {
            int x = e.getX();
            int y = e.getY();
            if (!isIn(x, y)) {
                return;
            }

            x = x - pos.i0;
            y = y - pos.i1;
            e = new MouseEvent((java.awt.Component) e.getSource(), e.getID(), e.getWhen(), e.getModifiers(), x, y, e.getClickCount(), e.isPopupTrigger(), e.getButton());
            if (scrollBar.getVisible()) {
                scrollBar.mouseClicked(e);
                if (e.isConsumed()) {
                    return;
                }
            }

            int i = firstItemIndex;
            for (LabeledButton iter : itemBt) {
                iter.mouseClicked(e);
                if (e.isConsumed()) {
                    value = i;
                    if (onChangeCallback != null) {
                        onChangeCallback.execute();
                    }
                    e.consume();
                    return;
                }
                i++;
            }
        }
    }

    public void mouseMoved(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        if (!visible) {
            return;
        }
        x = x - pos.i0;
        y = y - pos.i1;
        MouseEvent eventWithNewPos = new MouseEvent((java.awt.Component) e.getSource(), e.getID(), e.getWhen(), e.getModifiers(), x, y, e.getClickCount(), e.isPopupTrigger(), e.getButton());
        for (LabeledButton iter : itemBt) {
            // highlight the item with the mouse over
            iter.mouseMoved(eventWithNewPos);
        }
        scrollBar.mouseMoved(eventWithNewPos);
    }

    public void draw() {
        if (visible) {
            painter.drawSquareEdge(pos, size);
            SglAccess.glPushMatrix();
            SglAccess.glTranslatef(pos.i0, pos.i1, 0.f);
            scissor.push(pos, size);

            if (scrollBar.getVisible()) {
                scrollBar.draw();
            }

            int i = firstItemIndex;
            int j = 0;
            while (i < firstItemIndex + displayLines && i < items.size()) {
                if (value != -1 && value == i) {
                    itemBt.get(j).setHideBorder(false);
                    itemBt.get(j).setHideBorderMouseOver(false);
                } else {
                    itemBt.get(j).setHideBorder(true);
                    itemBt.get(j).setHideBorderMouseOver(true);
                }
                itemBt.get(j++).draw();
                i++;
            }
            scissor.pop();
            SglAccess.glPopMatrix();
        }
    }

    public void setVisible(boolean _visible) {
        for (LabeledButton iter : itemBt) {
            iter.setVisible(_visible);
        }
        super.setVisible(_visible);
    }

    public String getItem(int value) {
        String item;
        if (items.isEmpty() || value < 0 || value >= items.size()) {
            item = "";
        } else {
            item = items.get(value);
        }
        return item;
    }

    public void addItems(List<String> _items) {
        if (_items.isEmpty()) return;
        String item;

        int i = 0;
        while (i < _items.size()) {
            item = _items.get(i);
            items.add(item);
            i++;
        }
        adjustAfterItemsAdded();
    }

    public void addItem(String _text) {
        if (!items.isEmpty()) {
            items.add(_text);
        }
        adjustAfterItemsAdded();
    }

    public void clear() {
        items.clear();
        adjustAfterItemsAdded();
    }

    public int getValue() {
        return value;
    }

    public String getCurrent() {
        return getItem(getValue());
    }

    private StelCallback onChangeCallback;

    private ScrollBar scrollBar = new ScrollBar(true, 1, 1);

    private void createLines() {
        int i;
        LabeledButton bt;

        itemBt.clear();

        for (i = 0; i < displayLines; i++) {
            bt = new LabeledButton("");
            bt.setHideBorder(true);
            bt.setHideBorderMouseOver(true);
            bt.setHideTexture(true);
            bt.setJustification(Justification.JUSTIFY_LEFT);
            bt.setVisible(visible);
            itemBt.add(bt);
        }
    }

    private void adjustAfterItemsAdded() {
        createLines();
        setSize(getSizeX(), LISTBOX_ITEM_HEIGHT * displayLines);
        scrollBar.setOnChangeCallback(new StelCallback() {
            public void execute() {
                scrollChanged();
            }
        });
        scrollBar.setSize(SGUI.SCROLL_SIZE, getSizeY());
        scrollBar.setPos(getSizeX() - SGUI.SCROLL_SIZE, 0);
        scrollBar.setTotalElements(items.size());
        scrollBar.setElementsForBar(displayLines);

        scrollChanged();
    }

    private void scrollChanged() {
        firstItemIndex = scrollBar.getValue();
        int i = firstItemIndex;
        int j = 0;
        int w = getSizeX();
        w = w - ((scrollBar.getVisible() ? 1 : 0) * scrollBar.getSizeX());
        while (i < firstItemIndex + displayLines && i < items.size()) {
            itemBt.get(j).setPos(0, j * LISTBOX_ITEM_HEIGHT);
            itemBt.get(j).setSize(w, LISTBOX_ITEM_HEIGHT - 1);
            itemBt.get(j++).setLabel(items.get(i++));
        }
    }

    private StelCallback onChangedCallback;

    private int firstItemIndex;

    private List<LabeledButton> itemBt = new ArrayList<LabeledButton>();

    private List<String> items = new ArrayList<String>();

    private int value = -1;

    private int displayLines = 5;
}
