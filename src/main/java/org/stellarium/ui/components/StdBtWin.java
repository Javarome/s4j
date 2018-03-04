/*
 * User: freds
 * Date: Nov 25, 2006
 * Time: 12:04:01 AM
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


public class StdBtWin extends StdWin {
    StdBtWin(String someTitle, STexture someHeaderTex, SFontIfc someWinFont, int headerSize) {
        super(someTitle, someHeaderTex, someWinFont, headerSize);
        hideBt = new Button();
        hideBt.reshape(3, 3, headerSize - 6, headerSize - 6);
        hideBt.setOnPressCallback(new StelCallback() {
            public void execute() {
                onHideBt();
            }
        });
        addComponent(hideBt);
    }

    public StdBtWin(String someTitle) {
        this(someTitle, null, null, 18);
    }

    public void draw() {
        if (!visible) return;
        hideBt.setPos(size.i0 - hideBt.getSizeX() - 3, 3);
        draw();
    }

    void onHideBt() {
        visible = false;
        if (onHideBtCallback != null) onHideBtCallback.execute();
    }

    public void setOnHideBtCallback(StelCallback c) {
        onHideBtCallback = c;
    }

    protected Button hideBt;

    protected StelCallback onHideBtCallback;
}
