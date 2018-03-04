/*
 * User: freds
 * Date: Nov 25, 2006
 * Time: 12:00:37 AM
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


public class LabeledCheckBox extends StelContainer {
    public LabeledCheckBox(boolean state, String label) {
        checkbx = new CheckBox(state);
        addComponent(checkbx);
        lbl = new StelLabel(label);
        lbl.setPos(checkbx.getSizeX() + 4, 0);
        addComponent(lbl);
        setSize(checkbx.getSizeX() + lbl.getSizeX() + 2,
                checkbx.getSizeY() > lbl.getSizeY() ? checkbx.getSizeY() : lbl.getSizeY());
    }

    public boolean getState() {
        return checkbx.getState();
    }

    public void setState(boolean s) {
        checkbx.setState(s);
    }

    public void setOnPressCallback(StelCallback c) {
        checkbx.setOnPressCallback(c);
    }

    protected CheckBox checkbx;

    protected StelLabel lbl;
}
