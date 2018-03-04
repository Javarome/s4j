/*
 * User: freds
 * Date: Nov 25, 2006
 * Time: 12:52:06 AM
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
package org.stellarium.ui.render;

import org.stellarium.ui.FontFactory;

import java.awt.geom.Dimension2D;


public class SFontFactory implements FontFactory {
    private Dimension2D viewportSize;

    public SFontFactory(Dimension2D viewportSize) {
        this.viewportSize = viewportSize;
    }

    // TODO: Calculate the size correctly
    public SFontIfc create(int size, @Deprecated String fontName) {
        if (size <= 2) {
            size = 10;
        }
        return new TextRendererFont(size, fontName, viewportSize);
    }

    public SFontIfc create(int size, @Deprecated String fontName, SColor fontColor) {
        if (size <= 2) {
            size = 10;
        }
        return new TextRendererFont(size, fontName, fontColor, viewportSize);
    }
}
