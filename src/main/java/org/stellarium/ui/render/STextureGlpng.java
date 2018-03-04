/*
 * User: freds
 * Date: Nov 27, 2006
 * Time: 1:05:18 AM
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

import org.stellarium.StellariumException;
import org.stellarium.ui.SglAccess;

import javax.media.opengl.GL;
import java.io.File;
import java.nio.FloatBuffer;


/**
 * Stellarium Texture
 *
 * @author Fred Simon
 * @version 0.8.2
 */
class STextureGlpng implements STexture {

    public STextureGlpng(String someTextureName) throws StellariumException {
        textureName = someTextureName;
        texID = 0;
        loadType = GlPng.PNG_BLEND1;
        loadType2 = GL.GL_CLAMP;
        load(texDir + textureName);
    }

    public STextureGlpng(String _textureName, int _loadType) throws StellariumException {
        initTexture(_textureName, _loadType);
        load(texDir + textureName);
    }

    public STextureGlpng(boolean fullPath, String _textureName, int _loadType) throws StellariumException {
        initTexture(_textureName, _loadType, fullPath);
        if (fullPath) load(textureName);
        else load(texDir + textureName);
    }

    public STextureGlpng(boolean fullPath, String _textureName, int _loadType, boolean mipmap) throws StellariumException {
        initTexture(_textureName, _loadType, fullPath);
        if (fullPath) load(textureName, mipmap);
        else load(texDir + textureName, mipmap);
    }

    public STextureGlpng(String _textureName, int _loadType, boolean mipmap) throws StellariumException {
        initTexture(_textureName, _loadType);
        load(texDir + textureName, mipmap);
    }

    private void initTexture(String _textureName, int _loadType, boolean fullPath) {
        initTexture(_textureName, _loadType);
        wholePath = fullPath;
    }

    private void initTexture(String _textureName, int _loadType) {
        textureName = _textureName;
        loadType = GlPng.PNG_BLEND1;
        loadType2 = GL.GL_CLAMP_TO_EDGE;
        switch (_loadType) {
            case TEX_LOAD_TYPE_PNG_ALPHA:
                loadType = GlPng.PNG_ALPHA;
                break;
            case TEX_LOAD_TYPE_PNG_SOLID:
                loadType = GlPng.PNG_SOLID;
                break;
            case TEX_LOAD_TYPE_PNG_BLEND3:
                loadType = GlPng.PNG_BLEND3;
                break;
            case TEX_LOAD_TYPE_PNG_BLEND4:
                loadType = GlPng.PNG_BLEND4;
                break;
            case TEX_LOAD_TYPE_PNG_BLEND1:
                loadType = GlPng.PNG_BLEND1;
                break;
            case TEX_LOAD_TYPE_PNG_BLEND8:
                loadType = GlPng.PNG_BLEND8;
                break;
            case TEX_LOAD_TYPE_PNG_REPEAT:
                loadType = GlPng.PNG_BLEND1;
                loadType2 = GL.GL_REPEAT;
                break;
            case TEX_LOAD_TYPE_PNG_SOLID_REPEAT:
                loadType = GlPng.PNG_SOLID;
                loadType2 = GL.GL_REPEAT;
                break;
            default:
                loadType = GlPng.PNG_BLEND3;
        }
        texID = 0;
    }

    protected void finalize() throws Throwable {
        super.finalize();
        unload();
    }

    boolean load(String fullName) throws StellariumException {
        return load(fullName, true);
    }

    boolean load(String fullName, boolean mipmap) throws StellariumException {
        File tempFile = new File(fullName);
        if (!tempFile.exists()) {
            throw new StellariumException("WARNING : Can't find texture file " + fullName + "!");
        }

        GlPng.PngInfo info = new GlPng.PngInfo();
        GlPng.pngSetStandardOrientation(1);

        // frans van hoesel patch - mipmaps keep nebulas from scintilating as move
        if (mipmap) {
            texID = GlPng.pngBind(fullName, GlPng.PNG_BUILDMIPMAPS,
                    loadType, info, loadType2,
                    GL.GL_LINEAR_MIPMAP_NEAREST, GL.GL_LINEAR);
        } else {
            texID = GlPng.pngBind(fullName, GlPng.PNG_NOMIPMAPS,
                    loadType, info, loadType2,
                    GL.GL_NEAREST, GL.GL_LINEAR);
        }

        return texID != 0;
    }

    void unload() {
        SglAccess.glDeleteTextures(1, new int[]{texID}, 0);// Delete The Texture
    }

    void reload() throws StellariumException {
        unload();
        if (wholePath) load(textureName);
        else load(texDir + textureName);
    }

    // Return the texture size in pixels
    public int getWidth() {
        SglAccess.glBindTexture(GL.GL_TEXTURE_2D, texID);
        int[] w = new int[2];
        SglAccess.glGetTexLevelParameteriv(GL.GL_TEXTURE_2D, 0, GL.GL_TEXTURE_WIDTH, w, 0);
        return w[0];
    }

    /**
     * @return the average texture luminance : 0 is black, 1 is white
     */
    public float getAverageLuminance() {
        int[] wh = getDimensions();
        int size = wh[0] * wh[1];
        FloatBuffer p = FloatBuffer.allocate(size);

        SglAccess.glGetTexImage(GL.GL_TEXTURE_2D, 0, GL.GL_LUMINANCE, GL.GL_FLOAT, p);
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
        SglAccess.glBindTexture(GL.GL_TEXTURE_2D, texID);
        int[] wh = new int[2];
        SglAccess.glGetTexLevelParameteriv(GL.GL_TEXTURE_2D, 0, GL.GL_TEXTURE_WIDTH, wh, 0);
        SglAccess.glGetTexLevelParameteriv(GL.GL_TEXTURE_2D, 0, GL.GL_TEXTURE_HEIGHT, wh, 1);
        return wh;
    }

    public void close() {
        unload();
    }

    public String getTextureName() {
        return textureName;
    }

    public void displayTexture(double x, double y, double width, double height) {
        throw new java.lang.RuntimeException("Not implemented");
    }

    public int getID() {
        return texID;
    }

    public static void setTexDir(String _texDir) {
        texDir = _texDir;
    }

    private String textureName;

    private int texID;

    private int loadType;

    private int loadType2;

    private static String texDir = "./";

    private boolean wholePath;
}
