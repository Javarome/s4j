package org.stellarium.ui;

import org.stellarium.ui.render.SColor;
import org.stellarium.ui.render.SFontIfc;

/**
 * @author Jerome Beau
 * @version 28 mai 2008 01:00:33
 */
public interface FontFactory {
    // TODO: Calculate the size correctly
    SFontIfc create(int size, @Deprecated String fontName);

    SFontIfc create(int size, @Deprecated String fontName, SColor fontColor);
}
