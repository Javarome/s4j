package org.stellarium.projector;

import org.stellarium.StellariumException;
import org.stellarium.data.IniFileParser;

import javax.vecmath.Vector3d;
import java.nio.ByteBuffer;

import static java.lang.StrictMath.*;
import static javax.media.opengl.GL.*;
import static org.stellarium.ui.SglAccess.*;

/**
 * @author Jerome Beau
 * @version 24 oct. 2006 00:53:23
 */
public abstract class ViewportDistorter {
    public static enum TYPE {none, fisheye_to_spheric_mirror}

    public abstract TYPE getType();

    public abstract void init(IniFileParser conf);

    public abstract void distort();

    public abstract boolean distortXY(int x, int y);

    static class ViewportDistorterDummy extends ViewportDistorter {
        public TYPE getType() {
            return TYPE.none;
        }

        public void init(IniFileParser conf) {
        }

        public void distort() {
        }

        public boolean distortXY(int x, int y) {
            return true;
        }
    }

    static class ViewportDistorterFisheyeToSphericMirror extends ViewportDistorter {
        public ViewportDistorterFisheyeToSphericMirror(int width, int height, Projector prj) throws StellariumException {
            screenW = width;
            screenH = height;
            transArray = null;

            if (prj.getType() == Projector.TYPE.fisheye) {
                prj.setMaxFov(175.0);
            } else {
                throw new StellariumException("ViewportDistorterFisheyeToSphericMirror: What are you doing? the projection type should be fisheye.");
            }
            viewport_wh = (screenW < screenH) ? screenW : screenH;
            int texture_wh = 1;
            while (texture_wh < viewport_wh) texture_wh <<= 1;
            textureUsed = viewport_wh / (float) texture_wh;

            glGenTextures(1, new int[]{mirrorTexture}, 0);
            glBindTexture(GL_TEXTURE_2D, mirrorTexture);
            glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[0]);// TODO(JBE): 3145728 bytes required here (!)
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, texture_wh, texture_wh, 0, GL_RGB, GL_UNSIGNED_BYTE, byteBuffer);

            //  calc.setParams(Vec3d(0,-2,15),Vec3d(0,0,20),1,25,0.125);
        }

        public TYPE getType() {
            return TYPE.fisheye_to_spheric_mirror;
        }

        public int screenW;

        public int screenH;

        public int mirrorTexture;

        public int viewport_wh;

        public float textureUsed;

        class VertexData {
            float[] color = new float[4];

            float[] xy = new float[2];

            double h;
        }

        VertexData[] transArray;

        int transWidth, transHeight;

        SphericMirrorCalculator calc;

        public void init(IniFileParser conf) {
            calc.init(conf);
            final double gamma = conf.getDouble(IniFileParser.SPHERIC_MIRROR_SECTION, "projector_gamma", 0.45);
            // init transformation
            transWidth = screenW / 16;
            transHeight = screenH / 16;
            transArray = new VertexData[(transWidth + 1) * (transHeight + 1)];
            double max_h = 0;
            for (int j = 0; j <= transHeight; j++) {
                for (int i = 0; i <= transWidth; i++) {
                    VertexData data = transArray[(j * (transWidth + 1) + i)] = new VertexData();
                    Vector3d v = new Vector3d(), v_x = new Vector3d(), v_y = new Vector3d();
                    boolean rc = calc.retransform(
                            (i - (transWidth >> 1)) / (double) transHeight,
                            (j - (transHeight >> 1)) / (double) transHeight,
                            v, v_x, v_y);
                    Vector3d tmp = new Vector3d();
                    tmp.cross(v_x, v_y);
                    data.h = rc ? tmp.length() : 0.0;
                    v.x = -v.x;
                    final double h = v.y;
                    v.y = v.z;
                    v.z = -h;
                    final double oneoverh = 1 / sqrt(v.x * v.x + v.y * v.y);
                    final double a = 0.5 + atan(v.z * oneoverh) / PI;// range: [0..1]
                    double f = a * 180.0 / 175.0;// MAX_FOV=175.0 for fisheye
                    f *= oneoverh;
                    double x = (0.5 + v.x * f);
                    double y = (0.5 + v.y * f);
                    if (x < 0.0) {
                        x = 0.0;
                        data.h = 0;
                    } else if (x > 1.0) {
                        x = 1.0;
                        data.h = 0;
                    }
                    if (y < 0.0) {
                        y = 0.0;
                        data.h = 0;
                    } else if (y > 1.0) {
                        y = 1.0;
                        data.h = 0;
                    }
                    data.xy[0] = (float) (x * textureUsed);
                    data.xy[1] = (float) (y * textureUsed);
                    if (data.h > max_h) max_h = data.h;
                }
            }
            for (int j = 0; j <= transHeight; j++) {
                for (int i = 0; i <= transWidth; i++) {
                    VertexData data = transArray[(j * (transWidth + 1) + i)];
                    data.color[0] = data.color[1] = data.color[2] =
                            (float) ((data.h <= 0.0) ? 0.0 : exp(gamma * log(data.h / max_h)));
                    data.color[3] = 1.0f;
                }
            }
        }

        protected void finalize() {
            glDeleteTextures(1, new int[]{mirrorTexture}, 0);
        }

        public boolean distortXY(int x, int y) {
            // linear interpolation:
            y = screenH - 1 - y;
            float dx = (x & 15) / 16.0f;
            int i = x >> 4;
            float dy = (y & 15) / 16.0f;
            int j = y >> 4;
            float f00 = (1.0f - dx) * (1.0f - dy);
            float f01 = (dx) * (1.0f - dy);
            float f10 = (1.0f - dx) * (dy);
            float f11 = (dx) * (dy);

            // TODO: FRED Remove this huge array manipulation and use iterators
            int v = j * (transWidth + 1) + i;
            VertexData[] vp = transArray;
            x = ((screenW - viewport_wh) >> 1)
                    + (int) floor(0.5f +
                    (vp[v].xy[0] * f00
                            + vp[v + 1].xy[0] * f01
                            + vp[v + transWidth + 1].xy[0] * f10
                            + vp[v + transWidth + 2].xy[0] * f11) * viewport_wh / textureUsed);
            y = ((screenH - viewport_wh) >> 1)
                    + (int) floor(0.5f +
                    ((vp[v].xy[1] * f00
                            + vp[v + 1].xy[1] * f01
                            + vp[v + transWidth + 1].xy[1] * f10
                            + vp[v + transWidth + 2].xy[1] * f11) * viewport_wh / textureUsed));
            y = screenH - 1 - y;
            return true;
        }

        public void distort() {
            glViewport(0, 0, screenW, screenH);
            glMatrixMode(GL_PROJECTION);// projection matrix mode
            glPushMatrix();// store previous matrix
            glLoadIdentity();
            gluOrtho2D(0, screenW, 0, screenH);// set a 2D orthographic projection
            glMatrixMode(GL_MODELVIEW);// modelview matrix mode
            glPushMatrix();
            glLoadIdentity();


            glBindTexture(GL_TEXTURE_2D, mirrorTexture);
            glCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0,
                    (screenW - viewport_wh) >> 1, (screenH - viewport_wh) >> 1,
                    viewport_wh, viewport_wh);

            glEnable(GL_TEXTURE_2D);

            float[] color = {1, 1, 1, 1};
            glColor4fv(color, 0);
            glDisable(GL_BLEND);
            glBindTexture(GL_TEXTURE_2D, mirrorTexture);

            /*
              glBegin(GL_QUADS);
              glTexCoord2f(0.0, 0.0); glVertex3f(screenW,0, 0.0);
              glTexCoord2f(textureUsed, 0.0); glVertex3f(0,0, 0.0);
              glTexCoord2f(textureUsed, textureUsed); glVertex3f(0, screenH, 0.0);
              glTexCoord2f(0.0, textureUsed); glVertex3f(screenW, screenH, 0.0);
              glEnd();
            */

            for (int j = 0; j < transHeight; j++) {
                glBegin(GL_QUAD_STRIP);
                VertexData[] v = transArray;
                int v0 = j * (transWidth + 1);
                int v1 = v0 + (transWidth + 1);
                for (int i = 0; i <= transWidth; i++) {
                    glColor4fv(v[v0 + i].color, 0);
                    glTexCoord2fv(v[v0 + i].xy, 0);
                    glVertex3f(i * 16f, j * 16f, 0.0f);
                    glColor4fv(v[v1 + i].color, 0);
                    glTexCoord2fv(v[v1 + i].xy, 0);
                    glVertex3f(i * 16f, (j + 1) * 16f, 0.0f);
                }
                glEnd();
            }

            glMatrixMode(GL_PROJECTION);// Restore previous matrix
            glPopMatrix();
            glMatrixMode(GL_MODELVIEW);
            glPopMatrix();
        }
    }

    public static ViewportDistorter create(TYPE type, int width, int height, Projector prj) throws StellariumException {
        if (TYPE.fisheye_to_spheric_mirror == type) {
            return new ViewportDistorterFisheyeToSphericMirror(width, height, prj);
        }
        return new ViewportDistorterDummy();
    }
}
