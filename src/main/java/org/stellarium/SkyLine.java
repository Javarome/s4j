package org.stellarium;

import org.stellarium.projector.Projector;
import static org.stellarium.ui.SglAccess.*;
import org.stellarium.ui.fader.LinearFader;
import org.stellarium.ui.render.SFontIfc;

import javax.media.opengl.GL;
import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;
import java.awt.*;
import static java.lang.StrictMath.*;

/**
 * @author <a href="mailto:rr0@rr0.org">Jerome Beau</a>, Fred Simon
 * @version 0.8.2
 */
public class SkyLine {

    enum TYPE {
        EQUATOR(0),
        ECLIPTIC(23.4392803055555555556),
        LOCAL(0),
        MERIDIAN(90);

        private double inclinaison;

        TYPE(double inclinaison) {
            this.inclinaison = inclinaison;
        }

        public double getInclinaison() {
            return inclinaison;
        }
    }

    SkyLine(TYPE lineType) {
        this(lineType, 1., 48);
    }

    SkyLine(TYPE lineType, double radius, int nbSegment) {
        this.radius = radius;
        this.nbSegment = nbSegment;
        this.color = new Color(0.f, 0.f, 1.f);
        this.font = null;
        this.lineType = lineType;
        double inclinaison = lineType.getInclinaison();

        Matrix4d r = new Matrix4d();
        r.rotX(Math.toRadians(inclinaison));

        // Ecliptic month labels need to be redone
        // correct for month labels
        // TODO: can make this more accurate
        //	if(line_type == ECLIPTIC ) r = r * Mat4f::zrotation(-77.9*M_PI/180.);

        // Points to draw along the circle
        points = new Point3d[this.nbSegment + 1];
        for (int i = 0; i < this.nbSegment + 1; ++i) {
            Point3d v = points[i] = new Point3d();
            StelUtility.spheToRect(i / (this.nbSegment) * 2.f * PI, 0.d, v);
            v.scale(this.radius);
            //points[i].transfo4d(r);
            r.transform(v);
        }
    }

    void draw(Projector prj, NavigatorIfc nav) {
        if (!fader.hasInterstate())
            return;

        Point3d pt1 = new Point3d();
        Point3d pt2 = new Point3d();

        glColor4f(color.getRed(), color.getGreen(), color.getBlue(), fader.getInterstate());
        glDisable(GL.GL_TEXTURE_2D);
        glEnable(GL.GL_BLEND);
        // Normal transparency mode
        glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

        prj.setOrthographicProjection();// set 2D coordinate

        if (lineType == TYPE.ECLIPTIC) {
            // special drawing of the ecliptic line
            Matrix4d m = nav.getHomePlanet().getRotEquatorialToVsop87();
            m.transpose();
            boolean drawLabels =
                    (nav.getHomePlanet().getEnglishName().equals("Earth") && font != null);
            // start labeling from the vernal equinox
            double corr = drawLabels ? (atan2(m.m01, m.m00) - (PI / 2)) : 0.0;
            Point3d point = new Point3d(radius * cos(corr), radius * sin(corr), 0.0);
            m.transform(point);
            boolean prevOnScreen = prj.projectEarthEqu(point, pt1);
            for (int i = 1; i < nbSegment + 1; ++i) {
                double phi = corr + 2 * i * PI / nbSegment;
                point = new Point3d(radius * cos(phi), radius * sin(phi), 0.0);
                m.transform(point);
                boolean onScreen = prj.projectEarthEqu(point, pt2);
                if (onScreen && prevOnScreen) {
                    double dx = pt2.x - pt1.x;
                    double dy = pt2.x - pt1.y;
                    double dq = dx * dx + dy * dy;
                    if (dq < 1024 * 1024) {
                        glBegin(GL.GL_LINES);
                        glVertex2d(pt2.x, pt2.y);
                        glVertex2d(pt1.x, pt1.y);
                        glEnd();
                    }
                    if (drawLabels && (i + 2) % 4 == 0) {

                        double d = sqrt(dq);

                        double angle = acos((pt1.y - pt2.y) / d);
                        if (pt1.x < pt2.x) {
                            angle *= -1;
                        }

                        // draw text label
                        String oss = "" + (i + 3) / 4;

                        glPushMatrix();
                        glTranslated(pt2.x, pt2.y, 0);
                        glRotatef((float) (Math.toDegrees(-90 + angle)), 0f, 0f, -1f);

                        glEnable(GL.GL_TEXTURE_2D);

                        font.print(0, -2, oss);
                        glPopMatrix();
                        glDisable(GL.GL_TEXTURE_2D);
                    }
                }
                prevOnScreen = onScreen;
                pt1 = pt2;
            }
        } else {
            Projector.ProjFunc projFunc = getProjFunc(prj);
            for (int i = 0; i < nbSegment; ++i) {
                if (projFunc.execute(points[i], pt1) &&
                        projFunc.execute(points[i + 1], pt2)) {
                    double dx = pt1.x - pt2.x;
                    double dy = pt1.y - pt2.y;
                    double dq = dx * dx + dy * dy;
                    if (dq < 1024 * 1024) {

                        double angle;

                        // TODO: allow for other numbers of meridians and parallels without
                        // screwing up labels?

                        glBegin(GL.GL_LINES);
                        glVertex2d(pt1.x, pt1.y);
                        glVertex2d(pt2.x, pt2.y);
                        glEnd();


                        if (lineType == TYPE.MERIDIAN) {
                            double d = sqrt(dq);

                            angle = acos((pt1.y - pt2.y) / d);
                            if (pt1.x < pt2.x) {
                                angle *= -1;
                            }

                            // draw text label
                            String oss;

                            if (i <= 8) oss = "" + (i + 1) * 10;
                            else if (i <= 16) {
                                oss = "" + (17 - i) * 10;
                                angle += PI;
                            } else oss = "";

                            glPushMatrix();
                            glTranslated(pt2.x, pt2.x, 0);
                            glRotatef((float) (Math.toDegrees(180 + angle)), 0, 0, -1);

                            glBegin(GL.GL_LINES);
                            glVertex2d(-3, 0);
                            glVertex2d(3, 0);
                            glEnd();
                            glEnable(GL.GL_TEXTURE_2D);

                            if (font != null) font.print(2, -2, oss);
                            glPopMatrix();
                            glDisable(GL.GL_TEXTURE_2D);

                        }


                        if (lineType == TYPE.EQUATOR && (i + 1) % 2 == 0) {

                            double d = sqrt(dq);

                            angle = acos((pt1.y - pt2.y) / d);
                            if (pt1.x < pt2.x) {
                                angle *= -1;
                            }

                            // draw text label
                            String oss;

                            if ((i + 1) / 2 == 24) oss = "0h";
                            else oss = "" + (i + 1) / 2 + "h";

                            glPushMatrix();
                            glTranslated(pt2.x, pt2.y, 0);
                            glRotatef((float) (Math.toDegrees(180 + angle)), 0, 0, -1);

                            glBegin(GL.GL_LINES);
                            glVertex2d(-3, 0);
                            glVertex2d(3, 0);
                            glEnd();
                            glEnable(GL.GL_TEXTURE_2D);

                            if (font != null) font.print(2, -2, oss);
                            glPopMatrix();
                            glDisable(GL.GL_TEXTURE_2D);

                        }

                        // Draw months on ecliptic
                        /*
                        if(line_type == ECLIPTIC && (i+3) % 4 == 0) {

                            double d = sqrt(dq);

                            angle = acos((pt1[1]-pt2[1])/d);
                            if( pt1[0] < pt2[0] ) {
                                angle *= -1;
                            }

                            // draw text label
                            std::ostringstream oss;

                            oss << (i+3)/4;

                            glPushMatrix();
                            glTranslatef(pt2[0],pt2[1],0);
                            glRotatef(-90+angle*180./M_PI,0,0,-1);

                            glEnable(GL_TEXTURE_2D);

                            if(font) font.print(0,-2,oss.str());
                            glPopMatrix();
                            glDisable(GL_TEXTURE_2D);

                        }
                        */

                    }

                }
            }
        }

        prj.resetPerspectiveProjection();
    }

    void setColor(Color c) {
        color = new Color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
    }

    public Color getColor() {
        return color;
    }

    public void update(long deltaTime) {
        //        fader.update(deltaTime);
    }

    public void setFadeDuration(float duration) {
        fader.setDuration((int) (duration * 1000.f));
    }

    public void setFlagshow(boolean b) {
        fader.set(b);
    }

    public boolean getFlagshow() {
        return fader.getState();
    }

    public void setFont(SFontIfc someFont) throws StellariumException {
        font = someFont;
    }

    private double radius;

    private int nbSegment;

    TYPE lineType;

    Color color;

    private Point3d[] points;

    LinearFader fader = new LinearFader();

    SFontIfc font;

    private Projector.ProjFunc getProjFunc(Projector prj) {
        switch (lineType) {
            case LOCAL:
            case MERIDIAN:
                return prj.getProjectLocalFunc();
            case ECLIPTIC:
                return prj.getProjectJ2000Func();
            case EQUATOR:
                return prj.getProjectEarthEquFunc();
        }
        return prj.getProjectEarthEquFunc();
    }
}
