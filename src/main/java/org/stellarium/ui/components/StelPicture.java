/*
 * User: freds
 * Date: Nov 24, 2006
 * Time: 11:58:43 PM
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

import org.stellarium.data.ResourceLocatorUtil;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;

public class StelPicture extends JLabel {

    public StelPicture(String textureFileName, int xSize, int ySize) {
        this(textureFileName);
        if (xSize != -1 && ySize != -1) {
            setSize(xSize, ySize);
        }
    }

    public StelPicture(String textureFileName) {
        super(getIcon(textureFileName));
    }

    private static ImageIcon getIcon(String textureName) {
        return new ImageIcon(ResourceLocatorUtil.getInstance().getTextureURL(textureName));
    }

    public void setSize(int width, int height) {
        ImageIcon icon = (ImageIcon) getIcon();
        Image scaledImage = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
        icon.setImage(scaledImage);
    }

    void setShowEdge(boolean v) {
        showEdges = v;
        if (showEdges) {
            setBorder(new LineBorder(Color.GRAY));
        } else {
            setBorder(null);
        }
    }

    void setImgColor(Color c) {
        imgColor = c;
    }

    private boolean showEdges;

    private Color imgColor;

    public void close() {
    }
}
