package org.stellarium.ui.render;

/*
* Stellarium
* This file Copyright (C) 2005 Robert Spearman
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

import org.stellarium.NavigatorIfc;
import org.stellarium.StellariumException;
import org.stellarium.projector.Projector;

import javax.media.opengl.GL;
import javax.vecmath.AxisAngle4d;
import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import java.util.logging.Logger;

import static org.stellarium.ui.SglAccess.*;

/**
 * manage an image for display from scripts
 *
 * @author <a href="mailto:javarome@javarome.net"/>Jerome Beau</a>
 * @version Java
 */
public class Image {

    protected final Logger logger;
    protected final STextureFactory textureFactory;

    public enum IMAGE_POSITIONING {
        POS_VIEWPORT,
        POS_HORIZONTAL,
        POS_EQUATORIAL,
        POS_J2000
    }

    public Image(String filename, String name, IMAGE_POSITIONING positionType, Logger parentLogger) throws StellariumException {
        logger = Logger.getLogger(getClass().getName());
        if (parentLogger != null) {
            logger.setParent(parentLogger);
        }
        textureFactory = new STextureFactory(logger);

        imagePosType = positionType;
        imageName = name;

        // load image using alpha channel in image, otherwise no transparency
        // other than through setAlpha method -- could allow alpha load option from command

        imageTex = textureFactory.createTexture(true, filename, STexture.TEX_LOAD_TYPE_PNG_ALPHA, false);// what if it doesn't load?

        int imgW, imgH;
        int[] imgDimensions = imageTex.getDimensions();
        imgW = imgDimensions[0];
        imgH = imgDimensions[1];

        //  cout << "script image: " << imgW << " " << imgH << endl;

        if (imgH == 0) imageRatio = -1;// no image loaded
        else imageRatio = (float) imgW / imgH;
    }

    String getName() {
        return imageName;
    }

    /**
     * was texture loaded from disk?
     *
     * @return
     */
    boolean imageLoaded() {
        return (imageRatio != -1);
    }

    public void setAlpha(float alpha, float duration) {
        flagAlpha = true;

        startAlpha = imageAlpha;
        endAlpha = alpha;

        coefAlpha = 1.0f / (1000.f * duration);
        multAlpha = 0;
    }

    public void setScale(float scale, float duration) {
        flagScale = true;

        startScale = imageScale;
        endScale = scale;

        coefScale = 1.0f / (1000.f * duration);
        multScale = 0;
    }

    public void setRotation(float rotation, float duration) {
        flagRotation = true;

        startRotation = imageRotation;
        endRotation = rotation;

        coefRotation = 1.0f / (1000.f * duration);
        multRotation = 0;
    }

    public void setLocation(float xpos, boolean deltax, float ypos, boolean deltay, float duration) {
        // x and y make sense between -2 and 2 but any reason to check?
        // at x or y = 1, image is centered on projection edge

        flagLocation = true;

        startXPos = imageXPos;
        startYPos = imageYPos;

        // only move if changing value
        if (deltax) endXPos = xpos;
        else endXPos = imageXPos;

        if (deltay) endYPos = ypos;
        else endYPos = imageYPos;

        coefLocation = 1.0f / (1000.f * duration);
        multLocation = 0;
    }

    boolean update(long deltaTime) {
        if (imageRatio < 0) {
            return false;
        }

        if (flagAlpha) {
            multAlpha += coefAlpha * deltaTime;

            if (multAlpha >= 1) {
                multAlpha = 1;
                flagAlpha = false;
            }

            imageAlpha = startAlpha + multAlpha * (endAlpha - startAlpha);
        }

        if (flagScale) {
            multScale += coefScale * deltaTime;

            if (multScale >= 1) {
                multScale = 1;
                flagScale = false;
            }

            // this transition is parabolic for better visual results
            if (startScale > endScale) {
                imageScale = startScale + (1 - (1 - multScale) * (1 - multScale)) * (endScale - startScale);
            } else {
                imageScale = startScale + multScale * multScale * (endScale - startScale);
            }
        }

        if (flagRotation) {
            multRotation += coefRotation * deltaTime;

            if (multRotation >= 1) {
                multRotation = 1;
                flagRotation = false;
            }

            imageRotation = startRotation + multRotation * (endRotation - startRotation);
        }

        if (flagLocation) {
            multLocation += coefLocation * deltaTime;

            if (multLocation >= 1) {
                multLocation = 1;
                flagLocation = false;
            }

            imageXPos = startXPos + multLocation * (endXPos - startXPos);
            imageYPos = startYPos + multLocation * (endYPos - startYPos);
        }

        return true;
    }

    void draw(NavigatorIfc nav, Projector prj) {
        if (imageRatio < 0 || imageAlpha == 0) return;

        int vieww = prj.getViewportWidth();
        int viewh = prj.getViewportHeight();

        glEnable(GL.GL_TEXTURE_2D);
        glEnable(GL.GL_BLEND);

        glColor4f(1.0f, 1.0f, 1.0f, imageAlpha);

        glBindTexture(GL.GL_TEXTURE_2D, imageTex.getID());
        glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

        float cx = vieww / 2.f + prj.getViewportPosX();
        float cy = viewh / 2.f + prj.getViewportPosY();

        // calculations to keep image proportions when scale up to fit view
        float prjRatio = (float) vieww / viewh;

        float xbase, ybase;
        if (imageRatio > prjRatio) {
            xbase = vieww / 2;
            ybase = xbase / imageRatio;
        } else {
            ybase = viewh / 2;
            xbase = ybase * imageRatio;
        }

        float w = imageScale * xbase;
        float h = imageScale * ybase;


        if (imagePosType == IMAGE_POSITIONING.POS_VIEWPORT) {

            //	  cout << "drawing image viewport " << image_name << endl;

            // at x or y = 1, image is centered on projection edge
            // centered in viewport at 0,0

            prj.setOrthographicProjection();// set 2D coordinate

            glTranslatef(cx + imageXPos * vieww / 2, cy + imageYPos * viewh / 2, 0);// rotate around center of image...
            glRotatef(imageRotation, 0, 0, -1);

            glBegin(GL.GL_TRIANGLE_STRIP);
            {
                glTexCoord2i(1, 0);// Bottom Right
                glVertex3f(w, -h, 0);
                glTexCoord2i(0, 0);// Bottom Left
                glVertex3f(-w, -h, 0);
                glTexCoord2i(1, 1);// Top Right
                glVertex3f(w, h, 0);
                glTexCoord2i(0, 1);// Top Left
                glVertex3f(-w, h, 0);
            }
            glEnd();

            prj.resetPerspectiveProjection();

        } else if (imagePosType == IMAGE_POSITIONING.POS_HORIZONTAL) {

            //	  cout << "drawing image horizontal " << image_name << endl;

            // alt az coords
            prj.resetPerspectiveProjection();

            nav.switchToLocal();
            Matrix4d mat = nav.getLocalToEyeMat();

            Vector3d gridpt = new Vector3d();

            //	  printf("%f %f\n", imageXPos, imageYPos);

            // altitude = xpos, azimuth = ypos (0 at North), image top towards zenith when rotation = 0
            Matrix4d temp1 = new Matrix4d();
            Matrix4d temp2 = new Matrix4d();
            Matrix4d temp3 = new Matrix4d();
            temp1.rotZ(Math.toRadians(-1 * (imageYPos - 90)));
            temp2.rotX(Math.toRadians(imageXPos));
            temp3.mul(temp1, temp2);
            Vector3d imagev = new Vector3d(0, 1, 0);
            temp1.transform(imagev);

            Vector3d ortho1 = new Vector3d(1, 0, 0);
            temp1.transform(ortho1);
            Vector3d ortho2 = new Vector3d();
            ortho2.cross(imagev, ortho1);

            int gridSize = (int) (imageScale / 5.);// divisions per row, column
            if (gridSize < 5) {
                gridSize = 5;
            }

            for (int i = 0; i < gridSize; i++) {

                glBegin(GL.GL_QUAD_STRIP);

                for (int j = 0; j <= gridSize; j++) {

                    for (int k = 0; k <= 1; k++) {

                        // TODO: separate x, y scales?
                        if (imageRatio < 1) {
                            // image height is maximum angular dimension
                            temp1.setRotation(new AxisAngle4d(imagev, Math.toRadians(imageRotation + 180)));
                            temp2.setRotation(new AxisAngle4d(ortho1, Math.toRadians(imageScale * (j - gridSize / 2.) / gridSize)));
                            temp3.setRotation(new AxisAngle4d(ortho2, Math.toRadians((imageScale / imageRatio) * ((i + k - gridSize / 2.) / gridSize))));
                            Matrix4d d = new Matrix4d();
                            d.mul(temp1, temp2);
                            d.mul(temp3);
                            gridpt = new Vector3d();
                            d.transform(imagev, gridpt);
                        } else {
                            // image width is maximum angular dimension
                            temp1.setRotation(new AxisAngle4d(imagev, Math.toRadians(imageRotation + 180)));
                            temp2.setRotation(new AxisAngle4d(ortho1, Math.toRadians((imageScale / imageRatio) * ((j - gridSize / 2.) / gridSize))));
                            temp3.setRotation(new AxisAngle4d(ortho2, Math.toRadians(imageScale * ((i + k - gridSize / 2.) / gridSize))));
                            Matrix4d d = new Matrix4d();
                            d.mul(temp1, temp2);
                            d.mul(temp3);
                            gridpt = new Vector3d();
                            d.transform(imagev, gridpt);
                        }
                        glTexCoord2f((i + k) / (float) gridSize, j / (float) gridSize);
                        prj.sVertex3(gridpt.x, gridpt.y, gridpt.z, mat);
                    }
                }

                glEnd();
            }


        } else if (imagePosType == IMAGE_POSITIONING.POS_J2000 || imagePosType == IMAGE_POSITIONING.POS_EQUATORIAL) {

            // equatorial is in current equatorial coordinates
            // j2000 is in J2000 epoch equatorial coordinates (precessed)

            prj.setOrthographicProjection();// 2D coordinate

            Point3d gridpt;
            Point3d onscreen = new Point3d();

            // ypos is right ascension, xpos is declination
            Matrix4d temp1 = new Matrix4d();
            Matrix4d temp2 = new Matrix4d();
            Matrix4d temp3 = new Matrix4d();
            temp1.rotZ(Math.toRadians(imageYPos - 90));
            temp2.rotX(Math.toRadians(imageXPos));
            temp3.mul(temp1, temp2);
            Vector3d imagev = new Vector3d(0, 1, 0);
            temp1.transform(imagev);

            Vector3d ortho1 = new Vector3d(1, 0, 0);
            temp1.transform(ortho1);
            Vector3d ortho2 = new Vector3d();
            ortho2.cross(imagev, ortho1);

            int gridSize = (int) (imageScale / 5.);// divisions per row, column
            if (gridSize < 5) {
                gridSize = 5;
            }

            for (int i = 0; i < gridSize; i++) {

                glBegin(GL.GL_QUAD_STRIP);

                for (int j = 0; j <= gridSize; j++) {

                    for (int k = 0; k <= 1; k++) {

                        // TODO: separate x, y scales?
                        if (imageRatio < 1) {
                            // image height is maximum angular dimension
                            temp1.setRotation(new AxisAngle4d(imagev, Math.toRadians(imageRotation + 180)));
                            temp2.setRotation(new AxisAngle4d(ortho1, Math.toRadians(imageScale * ((j - gridSize / 2.) / gridSize))));
                            temp3.setRotation(new AxisAngle4d(ortho2, Math.toRadians((imageScale / imageRatio) * ((i + k - gridSize / 2.) / gridSize))));
                            Matrix4d d = new Matrix4d();
                            d.mul(temp1, temp2);
                            d.mul(temp3);
                            gridpt = new Point3d();
                            d.transform(new Point3d(imagev), gridpt);
                        } else {
                            // image width is maximum angular dimension
                            temp1.setRotation(new AxisAngle4d(imagev, Math.toRadians(imageRotation + 180)));
                            temp2.setRotation(new AxisAngle4d(ortho1, Math.toRadians((imageScale / imageRatio) * ((j - gridSize / 2.) / gridSize))));
                            temp3.setRotation(new AxisAngle4d(ortho2, Math.toRadians(imageScale * ((i + k - gridSize / 2.) / gridSize))));
                            Matrix4d d = new Matrix4d();
                            d.mul(temp1, temp2);
                            d.mul(temp3);
                            gridpt = new Point3d();
                            d.transform(new Point3d(imagev), gridpt);
                        }

                        if ((imagePosType == IMAGE_POSITIONING.POS_J2000 && prj.projectJ2000(gridpt, onscreen)) ||
                                (imagePosType == IMAGE_POSITIONING.POS_EQUATORIAL && prj.projectEarthEqu(gridpt, onscreen))) {

                            glTexCoord2f((i + k) / gridSize, j / gridSize);

                            glVertex3d(onscreen.x, onscreen.y, 0);

                        }
                    }
                }

                glEnd();
            }

            prj.resetPerspectiveProjection();


        }
    }

    private STexture imageTex;

    private String imageName;

    private IMAGE_POSITIONING imagePosType;

    private float imageScale = 1;   // full size
    private float imageAlpha;       // begin with 0 (not visible)
    private float imageRotation;

    /**
     * In degrees
     */
    private float imageRatio;
    private float imageXPos, imageYPos; // begin with 0 (centered by default)

    private boolean flagAlpha, flagScale, flagRotation, flagLocation;

    private float coefAlpha, coefScale, coefRotation;

    private float multAlpha, multScale, multRotation;

    private float startAlpha, startScale, startRotation;

    private float endAlpha, endScale, endRotation;

    private float coefLocation, multLocation;

    private float startXPos, startYPos, endXPos, endYPos;
}
