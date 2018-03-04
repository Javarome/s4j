/*
 * User: freds
 * Date: Nov 28, 2006
 * Time: 11:04:05 PM
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

import com.sun.opengl.util.texture.Texture;
import com.sun.opengl.util.texture.TextureCoords;
import com.sun.opengl.util.texture.TextureIO;
import org.stellarium.StellariumException;
import org.stellarium.data.ResourceLocatorUtil;

import javax.media.opengl.GL;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.FloatBuffer;
import java.util.logging.Logger;

import static javax.media.opengl.GL.GL_QUADS;
import static org.stellarium.ui.SglAccess.*;


/**
 * Stellarium Texture
 *
 * @author Fred Simon
 * @version 0.8.2
 */
public class STextureJogl implements STexture {

    private final boolean wholePath;

    private final String textureName;

    private final boolean mipmap;

    private Texture texture = null;

    private int loadType;

    private int loadType2;
    protected final Logger logger;

    public STextureJogl(String someTextureName, Logger parentLogger) throws StellariumException {
        this(false, someTextureName, GlPng.PNG_BLEND1, true, parentLogger);
    }

    public STextureJogl(String textureName, int loadType, Logger parentLogger) throws StellariumException {
        this(false, textureName, loadType, true, parentLogger);
    }

    public STextureJogl(boolean fullPath, String textureName, int loadType, Logger parentLogger) throws StellariumException {
        this(fullPath, textureName, loadType, true, parentLogger);
    }

    public STextureJogl(String textureName, int loadType, boolean mipmap, Logger parentLogger) throws StellariumException {
        this(false, textureName, loadType, mipmap, parentLogger);
    }

    public STextureJogl(boolean fullPath, String textureName, int loadType, boolean mipmap, Logger parentLogger) throws StellariumException {
        logger = Logger.getLogger(getClass().getName());
        if (parentLogger != null) {
            logger.setParent(parentLogger);
        }

        this.wholePath = fullPath;
        this.textureName = textureName;
        this.mipmap = mipmap;
        initLoadType(loadType);
    }

    private void initLoadType(int loadType) {
        this.loadType = GlPng.PNG_BLEND1;
        loadType2 = GL.GL_CLAMP_TO_EDGE;
        switch (loadType) {
            case TEX_LOAD_TYPE_PNG_ALPHA:
                this.loadType = GlPng.PNG_ALPHA;
                break;
            case TEX_LOAD_TYPE_PNG_SOLID:
                this.loadType = GlPng.PNG_SOLID;
                break;
            case TEX_LOAD_TYPE_PNG_BLEND3:
                this.loadType = GlPng.PNG_BLEND3;
                break;
            case TEX_LOAD_TYPE_PNG_BLEND4:
                this.loadType = GlPng.PNG_BLEND4;
                break;
            case TEX_LOAD_TYPE_PNG_BLEND1:
                this.loadType = GlPng.PNG_BLEND1;
                break;
            case TEX_LOAD_TYPE_PNG_BLEND8:
                this.loadType = GlPng.PNG_BLEND8;
                break;
            case TEX_LOAD_TYPE_PNG_REPEAT:
                this.loadType = GlPng.PNG_BLEND1;
                loadType2 = GL.GL_REPEAT;
                break;
            case TEX_LOAD_TYPE_PNG_SOLID_REPEAT:
                this.loadType = GlPng.PNG_SOLID;
                loadType2 = GL.GL_REPEAT;
                break;
            default:
                this.loadType = GlPng.PNG_BLEND3;
        }
    }

    protected void finalize() throws Throwable {
        unload();
        super.finalize();
    }

    private void load() {
        if (wholePath) {
            loadFromFile();
        } else {
            loadFromLocator();
        }
    }

    private void loadFromFile() {
        File tempFile = new File(textureName);
        if (!tempFile.exists()) {
            throw new StellariumException("WARNING : Can't find texture file " + textureName + "!");
        }
        try {
            logger.finer("Loading texture " + textureName + " from file " + tempFile);
            texture = TextureIO.newTexture(tempFile, mipmap);
            assert getID() != 0 : "Expected texture " + textureName + " to have an ID assigned since loading";
        } catch (IOException e) {
            throw new StellariumException("Error while loading texture " + texture + " from " + tempFile);
        }
    }

    private void loadFromLocator() {
        // TODO: Logging here blocks the thread, possibly because of some non-reentrance issue in Logging or JOGL API
        //logger.finer("Loading texture " + textureName + " from locator");

        // Add PNG if no extension specified in texture name
        String extension = "png";
        int lastDot = textureName.lastIndexOf('.');
        String resourceName;
        if (lastDot > 0 && lastDot < textureName.length() - 1) {
            resourceName = textureName;
            extension = textureName.substring(lastDot + 1);
        } else {
            resourceName = textureName + "." + extension;
        }
        try {
            URL texIs = ResourceLocatorUtil.getInstance().getTextureResource(resourceName);
            texture = TextureIO.newTexture(texIs, mipmap, extension);
            assert getID() != 0 : "Expected texture " + textureName + " to have an ID assigned after loading";
        } catch (IOException e) {
            throw new StellariumException("Error while loading texture " + textureName + " as " + resourceName + " from locator");
        }
    }

    void unload() {
        if (texture != null) {
            Texture myTexture = texture;
            texture = null;
            myTexture.dispose();
        }
    }

    void reload() throws StellariumException {
        unload();
        load();
    }

    // Return the texture size in pixels
    public int getWidth() {
        return getTexture().getWidth();
    }

    /**
     * @return the average texture luminance : 0 is black, 1 is white
     */
    public float getAverageLuminance() {
        Texture delegate = getTexture();
        int width = delegate.getWidth();
        int height = delegate.getHeight();
        int size = height * width;
        FloatBuffer p = FloatBuffer.allocate(size);

        glBindTexture(GL.GL_TEXTURE_2D, getID());
        glGetTexImage(GL.GL_TEXTURE_2D, 0, GL.GL_LUMINANCE, GL.GL_FLOAT, p);
        float sum = 0.f;
        for (int i = 0; i < size; ++i) {
            sum += p.get();
        }

        /*
        // This provides more correct result on some video cards (matrox)
        // TODO test more before switching

        GLubyte* pix = (GLubyte*)calloc(w*h*3, sizeof(GLubyte));

        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        glGetTexImage(GL_TEXTURE_2D, 0, GL_RGB, GL_UNSIGNED_BYTE, pix);

        float lum = 0.f;
        for (int i=0;i<w*h*3;i+=3)
        {
          double r = pix[i]/255.;
          double g = pix[i+1]/255.;
          double b = pix[i+2]/255.;
          lum += r*.299 + g*.587 + b*.114;
        }
        free(pix);

        printf("Luminance calc 2: Sum %f\tw %d h %d\tlum %f\n", lum, w, h, lum/(w*h));
        */

        return sum / size;
    }

    public int[] getDimensions() {
        //return new int[]{getTexture().getImageWidth(), getTexture().getImageHeight()};
        return new int[]{getTexture().getWidth(), getTexture().getHeight()};
    }

    public void close() {
        unload();
    }

    public String getTextureName() {
        return textureName;
    }

    public Texture getTexture() {
        if (texture == null) {
            load();
        }
        return texture;
    }

    public int getID() {
        return getTexture().getTextureObject();
    }

    public TextureCoords getImageTexCoords() {
        return getTexture().getImageTexCoords();
    }

    public void displayTexture(double x, double y, double width, double height) {
        TextureCoords coords = getImageTexCoords();
        glBegin(GL_QUADS);
        // Bottom Left
        glTexCoord2d(coords.left(), coords.bottom());
        glVertex3d(x, y, 0);
        // Bottom Right
        glTexCoord2d(coords.right(), coords.bottom());
        glVertex3d(x + width, y, 0);
        // Top Right
        glTexCoord2d(coords.right(), coords.top());
        glVertex3d(x + width, y + height, 0);
        // Top Left
        glTexCoord2d(coords.left(), coords.top());
        glVertex3d(x, y + height, 0);
        glEnd();
    }
}
