/*
 * Copyright (C) 2006 Frederic Simon
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
package org.stellarium.ui;

import com.sun.opengl.util.GLUT;
import org.stellarium.StelUtility;
import org.stellarium.vecmath.Rectangle4i;

import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;
import javax.media.opengl.glu.GLUquadric;
import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;
import javax.vecmath.Tuple3d;
import java.nio.Buffer;
import java.util.logging.Logger;

/**
 * @author Fred Simon
 * @version Java
 */
public class SglAccess {
    private static GL gl;

    private static GLU glu;

    private static final boolean inverseMatrix = false;

    private static GLUT glut;

    private static Logger logger;

    public static void setGl(GL gl, Logger parentLogger) {
        logger = Logger.getLogger(SglAccess.class.getName());
        if (parentLogger != null) {
            logger.setParent(parentLogger);
        }

        SglAccess.gl = gl;
        glu = new GLU();
        glut = new GLUT();
        logger.config("INIT GL IS: " + gl.getClass().getName());
        logger.config("GL_VENDOR: " + gl.glGetString(GL.GL_VENDOR));
        logger.config("GL_RENDERER: " + gl.glGetString(GL.GL_RENDERER));
        logger.config("GL_VERSION: " + gl.glGetString(GL.GL_VERSION));
    }

    // Special methods for wrapping standards GL calls

    public static void glCircle(Tuple3d pos, double radius) {
        glCircle(pos, radius, 1);
    }

    public static void glutBitmapString(double x, double y, String str) {
        gl.glRasterPos2d(x, y);
        glut.glutBitmapString(GLUT.BITMAP_TIMES_ROMAN_10, str);
    }

    public static void glCircle(Tuple3d pos, double radius, float lineWidth) {
        double angle, facets;
        boolean lastState = glIsEnabled(GL.GL_TEXTURE_2D);

        glDisable(GL.GL_TEXTURE_2D);
        if (lineWidth == 0) {
            lineWidth = 1;
        }
        glLineWidth(lineWidth);
        glBegin(GL.GL_LINE_LOOP);

        if (radius < 2) {
            facets = 6;
        } else {
            facets = (int) (radius * 3);
        }

        for (int i = 0; i < facets; i++) {
            angle = 2 * Math.PI * i / facets;
            glVertex3d(pos.x + radius * Math.sin(angle), pos.y + radius * Math.cos(angle), 0);
        }
        glEnd();

        if (lastState) {
            glEnable(GL.GL_TEXTURE_2D);
        }
        glLineWidth(1);
    }

    public static void glEllipse(Tuple3d pos, double radius, double yRatio) {
        glEllipse(pos, radius, yRatio, 1);
    }

    public static void glEllipse(Tuple3d pos, double radius, double yRatio, float lineWidth) {
        double angle, facets;
        boolean lastState = glIsEnabled(GL.GL_TEXTURE_2D);

        glDisable(GL.GL_TEXTURE_2D);
        if (lineWidth == 0)
            lineWidth = 1;
        glLineWidth(lineWidth);
        glBegin(GL.GL_LINE_LOOP);

        if (radius < 2) {
            facets = 6;
        } else {
            facets = (int) (radius * 3);
        }

        for (int i = 0; i < facets; i++) {
            angle = 2 * Math.PI * i / facets;
            glVertex3d(pos.x + radius * Math.sin(angle), pos.y + yRatio * radius * Math.cos(angle), 0);
        }
        glEnd();

        if (lastState) glEnable(GL.GL_TEXTURE_2D);
        glLineWidth(1);
    }

    // All Methods delegating to gl

    public static void glBegin(int i) {
        if (gl != null) gl.glBegin(i);
    }

    public static void glBindTexture(int i, int i1) {
        if (gl != null) gl.glBindTexture(i, i1);
    }

    public static void glColor3f(float v, float v1, float v2) {
        if (gl != null) gl.glColor3f(v, v1, v2);
    }

    public static void glColor3fv(float[] floats, int i) {
        if (gl != null) gl.glColor3fv(floats, i);
    }

    public static void glEnable(int i) {
        if (gl != null) gl.glEnable(i);
    }

    public static void glEnd() {
        if (gl != null) gl.glEnd();
    }

    public static void glTranslated(double v, double v1, double v2) {
        if (gl != null) gl.glTranslated(v, v1, v2);
    }

    public static void glTexCoord2f(float v, float v1) {
        if (gl != null) gl.glTexCoord2f(v, v1);
    }

    public static void glVertex3f(float v, float v1, float v2) {
        if (gl != null) gl.glVertex3f(v, v1, v2);
    }

    public static void glRotatef(float v, float v1, float v2, float v3) {
        if (gl != null) gl.glRotatef(v, v1, v2, v3);
    }

    public static void glTranslatef(float v, float v1, float v2) {
        if (gl != null) gl.glTranslatef(v, v1, v2);
    }

    public static void glTexParameteri(int i, int i1, int i2) {
        if (gl != null) gl.glTexParameteri(i, i1, i2);
    }

    public static void glDeleteTextures(int i, int[] ints, int i1) {
        if (gl != null) gl.glDeleteTextures(i, ints, i1);
    }

    public static void glGetTexLevelParameteriv(int i, int i1, int i2, int[] ints, int i3) {
        if (gl != null) {
            gl.glGetTexLevelParameteriv(i, i1, i2, ints, i3);
        } else {
            ints[0] = 512;
        }
    }

    public static void glGetTexImage(int i, int i1, int i2, int i3, Buffer buffer) {
        if (gl != null) gl.glGetTexImage(i, i1, i2, i3, buffer);
    }

    public static void glBlendFunc(int i, int i1) {
        if (gl != null) gl.glBlendFunc(i, i1);
    }

    public static void glTexCoord2i(int i, int i1) {
        if (gl != null) gl.glTexCoord2i(i, i1);
    }

    public static void glVertex2d(double v, double v1) {
        if (gl != null) gl.glVertex2d(v, v1);
    }

    public static void glDisable(int i) {
        if (gl != null) gl.glDisable(i);
    }

    public static void glPointSize(float v) {
        if (gl != null) gl.glPointSize(v);
    }

    public static void glVertex3d(double v, double v1, double v2) {
        if (gl != null) gl.glVertex3d(v, v1, v2);
    }

    public static void glColor4f(float v, float v1, float v2, float v3) {
        if (gl != null) gl.glColor4f(v, v1, v2, v3);
    }

    public static int glGenLists(int i) {
        return gl.glGenLists(i);
    }

    public static void glNewList(int i, int i1) {
        if (gl != null) gl.glNewList(i, i1);
    }

    public static void glEndList() {
        if (gl != null) gl.glEndList();
    }

    public static void glPushMatrix() {
        if (gl != null) gl.glPushMatrix();
    }

    public static void glScalef(float v, float v1, float v2) {
        if (gl != null) gl.glScalef(v, v1, v2);
    }

    public static void glListBase(int i) {
        if (gl != null) gl.glListBase(i);
    }

    public static void glCallLists(int i, int i1, Buffer buffer) {
        if (gl != null) gl.glCallLists(i, i1, buffer);
    }

    public static void glPopMatrix() {
        if (gl != null) gl.glPopMatrix();
    }

    public static void glCallList(int i) {
        if (gl != null) gl.glCallList(i);
    }

    public static void glGetFloatv(int i, float[] floats, int i1) {
        if (gl != null) gl.glGetFloatv(i, floats, i1);
    }

    public static void glColor3d(double v, double v1, double v2) {
        if (gl != null) gl.glColor3d(v, v1, v2);
    }

    public static void glGenTextures(int i, int[] ints, int i1) {
        if (gl != null) gl.glGenTextures(i, ints, i1);
    }

    public static void glVertex2i(int i, int i1) {
        if (gl != null) gl.glVertex2i(i, i1);
    }

    public static void glViewport(int i, int i1, int i2, int i3) {
        if (gl != null) gl.glViewport(i, i1, i2, i3);
    }

    public static void glMatrixMode(int i) {
        if (gl != null) gl.glMatrixMode(i);
    }

    public static void glScissor(int v0, int i, int v2, int v3) {
        if (gl != null) gl.glScissor(v0, i, v2, v3);
    }

    public static void glVertex2f(float v, float v1) {
        if (gl != null) gl.glVertex2f(v, v1);
    }

    public static void glLoadIdentity() {
        if (gl != null) gl.glLoadIdentity();
    }

    public static void glLoadMatrixd(Matrix4d mat) {
        if (gl != null) gl.glLoadMatrixd(toArray(mat), 0);
    }

    public static void glNormal3d(double v, double v1, double v2) {
        if (gl != null) gl.glNormal3d(v, v1, v2);
    }

    public static void glTexCoord2d(double v, double v1) {
        if (gl != null) gl.glTexCoord2d(v, v1);
    }

    public static void glTexCoord2s(short i, short i1) {
        if (gl != null) gl.glTexCoord2s(i, i1);
    }

    public static void glNormal3f(float v, float v1, float v2) {
        if (gl != null) gl.glNormal3f(v, v1, v2);
    }

    public static void glCullFace(int i) {
        if (gl != null) gl.glCullFace(i);
    }

    public static void glVertex3dv(double[] doubles, int i) {
        if (gl != null) gl.glVertex3dv(doubles, i);
    }

    public static void glGetBooleanv(int i, byte[] bytes, int i1) {
        if (gl != null) gl.glGetBooleanv(i, bytes, i1);
    }

    public static void glGetLightfv(int i, int i1, float[] floats, int i2) {
        if (gl != null) gl.glGetLightfv(i, i1, floats, i2);
    }

    public static void glLightfv(int i, int i1, float[] floats, int i2) {
        if (gl != null) gl.glLightfv(i, i1, floats, i2);
    }

    public static void glMaterialfv(int i, int i1, float[] floats, int i2) {
        if (gl != null) gl.glMaterialfv(i, i1, floats, i2);
    }

    public static void glClear(int i) {
        if (gl != null) gl.glClear(i);
    }

    public static void glClearStencil(int i) {
        if (gl != null) gl.glClearStencil(i);
    }

    public static void glStencilFunc(int i, int i1, int i2) {
        if (gl != null) gl.glStencilFunc(i, i1, i2);
    }

    public static void glStencilOp(int i, int i1, int i2) {
        if (gl != null) gl.glStencilOp(i, i1, i2);
    }

    public static void glColor3dv(double[] doubles, int i) {
        if (gl != null) gl.glColor3dv(doubles, i);
    }

    public static boolean glIsEnabled(int i) {
        return gl != null && gl.glIsEnabled(i);
    }

    public static void glLineWidth(float v) {
        if (gl != null) gl.glLineWidth(v);
    }

    public static void glColor4d(double v, double v1, double v2, double v3) {
        if (gl != null) gl.glColor4d(v, v1, v2, v3);
    }

    public static void glLineStipple(int i, short s) {
        if (gl != null) gl.glLineStipple(i, s);
    }

    public static void glTexImage2D(int i, int i1, int i2, int i3, int i4, int i5, int i6, int i7, Buffer buffer) {
        if (gl != null) gl.glTexImage2D(i, i1, i2, i3, i4, i5, i6, i7, buffer);
    }

    public static void glPixelStorei(int i, int i1) {
        if (gl != null) gl.glPixelStorei(i, i1);
    }

    public static void glCopyTexSubImage2D(int i, int i1, int i2, int i3, int i4, int i5, int i6, int i7) {
        if (gl != null) gl.glCopyTexSubImage2D(i, i1, i2, i3, i4, i5, i6, i7);
    }

    public static void glColor4fv(float[] floats, int i) {
        if (gl != null) gl.glColor4fv(floats, i);
    }

    public static void glTexCoord2fv(float[] floats, int i) {
        if (gl != null) gl.glTexCoord2fv(floats, i);
    }

    public static void glFrontFace(int i) {
        if (gl != null) gl.glFrontFace(i);
    }

    public static void glGetIntegerv(int i, int[] ints, int i1) {
        if (gl != null) gl.glGetIntegerv(i, ints, i1);
    }

    public static void glClearColor(float v, float v1, float v2, float v3) {
        if (gl != null) gl.glClearColor(v, v1, v2, v3);
    }

    // All methods delegating to glu
    public static GLUquadric gluNewQuadric() {
        if (glu == null)
            return null;
        return glu.gluNewQuadric();
    }

    public static void gluDisk(GLUquadric glUquadric, double v, double v1, int i, int i1) {
        if (glu != null) glu.gluDisk(glUquadric, v, v1, i, i1);
    }

    public static void gluDeleteQuadric(GLUquadric glUquadric) {
        if (glu != null) glu.gluDeleteQuadric(glUquadric);
    }

    public static void gluOrtho2D(double v, double v1, double v2, double v3) {
        if (glu != null) glu.gluOrtho2D(v, v1, v2, v3);
    }

    public static void gluQuadricTexture(GLUquadric glUquadric, boolean b) {
        if (glu != null) glu.gluQuadricTexture(glUquadric, b);
    }

    public static void gluQuadricOrientation(GLUquadric glUquadric, int i) {
        if (glu != null) glu.gluQuadricOrientation(glUquadric, i);
    }

    public static void gluSphere(GLUquadric glUquadric, double v, int i, int i1) {
        if (glu != null) glu.gluSphere(glUquadric, v, i, i1);
    }

    public static void gluCylinder(GLUquadric glUquadric, double v, double v1, double v2, int i, int i1) {
        if (glu != null) glu.gluCylinder(glUquadric, v, v1, v2, i, i1);
    }

    public static void gluProject(Tuple3d v, Matrix4d mat, Matrix4d matProjection, Rectangle4i vecViewport, Tuple3d win) {
        if (glu == null)
            return;
        double[] winArray = new double[3];
        glu.gluProject(v.x, v.y, v.z, toArray(mat), 0, toArray(matProjection), 0, vecViewport.toArray(), 0, winArray, 0);
        win.set(winArray);
    }

    public static void gluUnProject(Tuple3d win, Matrix4d mat, Matrix4d matProjection, Rectangle4i vecViewport, Point3d v) {
        double[] vValues = StelUtility.toArray(v);
        glu.gluUnProject(win.x, win.y, win.z, toArray(mat), 0, toArray(matProjection), 0, vecViewport.toArray(), 0, vValues, 0);
        v.set(vValues);
    }

    public static int gluBuild2DMipmaps(int i, int i1, int i2, int i3, int i4, int i5, Buffer buffer) {
        if (glu == null)
            return 0;
        return glu.gluBuild2DMipmaps(i, i1, i2, i3, i4, i5, buffer);
    }

    private static final float[] currentColor = new float[4];

    public static float[] getCurrentColor() {
        gl.glGetFloatv(GL.GL_CURRENT_COLOR, currentColor, 0);
        return currentColor;
    }

    /**
     * Private method interchanging from Java matrix to C++ expected matrix
     *
     * @param m
     * @return
     */
    private static double[] toArray(Matrix4d m) {
        double[] tmp = new double[16];

        if (inverseMatrix) {
            tmp[0] = m.m00;
            tmp[1] = m.m10;
            tmp[2] = m.m20;
            tmp[3] = m.m30;

            tmp[4] = m.m01;
            tmp[5] = m.m11;
            tmp[6] = m.m21;
            tmp[7] = m.m31;

            tmp[8] = m.m02;
            tmp[9] = m.m12;
            tmp[10] = m.m22;
            tmp[11] = m.m32;

            tmp[12] = m.m03;
            tmp[13] = m.m13;
            tmp[14] = m.m23;
            tmp[15] = m.m33;
        } else {
            tmp[0] = m.m00;
            tmp[1] = m.m01;
            tmp[2] = m.m02;
            tmp[3] = m.m03;

            tmp[4] = m.m10;
            tmp[5] = m.m11;
            tmp[6] = m.m12;
            tmp[7] = m.m13;

            tmp[8] = m.m20;
            tmp[9] = m.m21;
            tmp[10] = m.m22;
            tmp[11] = m.m23;

            tmp[12] = m.m30;
            tmp[13] = m.m31;
            tmp[14] = m.m32;
            tmp[15] = m.m33;
        }

        return tmp;
    }
}
