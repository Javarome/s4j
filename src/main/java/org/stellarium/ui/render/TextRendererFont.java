package org.stellarium.ui.render;

import com.sun.opengl.util.j2d.TextRenderer;
import org.stellarium.StellariumException;
import org.stellarium.ui.SglAccess;

import java.awt.*;
import java.awt.geom.Dimension2D;

/**
 * A Stellarium Font that uses AWT (2D)Graphics to display.
 *
 * @author <a href="mailto:javarome@javarome.net"/>J&eacute;r&ocirc;me Beau</a>
 * @version 0.8.2
 * @see <a href="http://stellarium.cvs.sourceforge.net/stellarium/stellarium/src/s_font.h?view=markup&pathrev=stellarium-0-8-2">s_font.h</a>
 * @see <a href="http://stellarium.cvs.sourceforge.net/stellarium/stellarium/src/typeface.cpp?view=log&pathrev=stellarium-0-8-2">typeface.cpp</a>
 * @see <a href="http://stellarium.cvs.sourceforge.net/stellarium/stellarium/src/typeface.h?view=log&pathrev=stellarium-0-8-2">typeface.h</a>
 */
public class TextRendererFont implements SFontIfc {

    private final Color color;
    private final Font typeFace;
    private final TextRenderer renderer;
    private Dimension2D viewportSize;

    /**
     * Stellarium 0.8.1 use ttf font names loaded
     *
     * @param size
     * @param fontName
     * @throws org.stellarium.StellariumException
     *
     */
    public TextRendererFont(int size, @Deprecated String fontName, Dimension2D viewportSize) throws StellariumException {
        // White by default
        this(size, fontName, null, viewportSize);
    }

    /**
     * Stellarium 0.8.1 use ttf font names loaded
     *
     * @param size
     * @param ttfFileName
     * @param fontColor
     * @throws org.stellarium.StellariumException
     *
     */
    public TextRendererFont(int size, @Deprecated String ttfFileName, SColor fontColor, Dimension2D viewportSize) throws StellariumException {
        typeFace = new Font(ttfFileName, Font.PLAIN, size);
        renderer = new TextRenderer(typeFace);
        color = toColor(fontColor);
        this.viewportSize = viewportSize;
    }

    // TODO(JBE): Move in a common color class
    public static Color toColor(SColor floatsColor) {
        return floatsColor == null ? null : new Color(floatsColor.x, floatsColor.y, floatsColor.z, floatsColor.w);
    }

    public void close() {
    }

    /**
     * Method with upsideDown by default
     */
    public void print(int x, int y, String str) {
        print(x, y, str, true);
    }

    public void print(int x, int y, String str, boolean upsidedown) {
        print(x, y, str, upsidedown, color);
    }

    public void print(int x, int y, String str, boolean upsidedown, SColor color) {
        print(x, y, str, upsidedown, toColor(color));
    }

    public void print(int x, int y, String str, boolean upsidedown, Color color) {
        print(x, y, str, upsidedown, color, 0);
    }

    public void print(int x, int y, String str, boolean upsidedown, Color someColor, float angle) {
        //if (upsidedown) y = strings.length * lineHeight;

        if (angle != 0) {
            renderer.begin3DRendering();
            SglAccess.glRotatef(angle, 0, 0, -1);
        } else {
            // TODO(JBE): Could we optimize (reduce drawing window) width and height here ?
            renderer.beginRendering((int) Math.ceil(viewportSize.getWidth()), (int) Math.ceil(viewportSize.getHeight()));
        }
        float[] currentColor = null;
        try {
            if (someColor != null) {
                currentColor = SglAccess.getCurrentColor();
                renderer.setColor(someColor);
            }
            boolean multiLine = str.indexOf('\n') >= 0;
            if (multiLine) {
                String[] strings = str.split("\n");
                int lineHeight = typeFace.getSize() + 2;
                for (String string : strings) {
                    renderer.draw(string, x, y);
                    y += upsidedown ? -lineHeight : lineHeight;
                }
            } else {
                renderer.draw(str, x, y);
            }
        } finally {
            if (angle == 0) {
                renderer.endRendering();
            } else {
                renderer.end3DRendering();
            }
            if (someColor != null) {
                SglAccess.glColor4fv(currentColor, 0);
            }
        }
    }

    // TODO(JBE): Not used. Check if this can be removed
    public void printChar(char c) {
        throw new RuntimeException("Not implemented");
    }

    // TODO(JBE): Not used. Check if this can be removed
    public void printCharOutlined(char c) {
        // TODO: Fred from what I understood this is the Bold version of the font !
        printChar(c);
    }

    public int getStrLen(String str) {
        return (int) renderer.getBounds(str).getWidth();
    }

    public int getLineHeight() {
        return (int) renderer.getBounds("X").getHeight();
    }

    // TODO(JBE): Not used. Check if this can be removed
    public int getAscent() {
        return typeFace.getSize();
    }

    // TODO(JBE): Not used. Check if this can be removed
    public int getDescent() {
        return typeFace.getSize();
    }
}