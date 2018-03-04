/*
 * User: freds
 * Date: Nov 25, 2006
 * Time: 12:04:48 AM
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


public class IntIncDecVert extends IntIncDec {
    IntIncDecVert(SFontIfc someFont, STexture texUp, STexture texDown, int someMin, int someMax, int initValue, int someInc) {
        super(someFont, texUp, texDown, someMin, someMax, initValue, someInc);
        label.setPos(0, 3);
        btmore.setPos(someMax / 10 * 8 + 8, 0);
        btless.setPos(someMax / 10 * 8 + 8, 8);
        setSize(someMax / 10 * 8 + 16, 40);
    }
}
