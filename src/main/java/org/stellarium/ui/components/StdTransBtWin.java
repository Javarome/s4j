/*
 * User: freds
 * Date: Nov 25, 2006
 * Time: 12:04:09 AM
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


/**
 * stdbtwin with transient (timeout) functionality
 */
public class StdTransBtWin extends StdBtWin {
    public StdTransBtWin(String _title, int someTimeOut, STexture _header_tex, SFontIfc _winfont, int headerSize) {
        super(_title, _header_tex, _winfont, headerSize);
        setTimeout(someTimeOut);
    }

    public StdTransBtWin(String _title, int someTimeOut) {
        this(_title, someTimeOut, null, null, 18);
    }

    public void update(int deltaTime) {

    }

    public void setTimeout(int timeout) {
        timeLeft = timeout;
    }

    protected int timeLeft;

    protected boolean timerOn;
}
