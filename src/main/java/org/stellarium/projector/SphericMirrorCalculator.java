/*
 * Stellarium
 * Copyright (C) Fabien Chereau
 * Author 2006 Johannes Gajdosik
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
package org.stellarium.projector;

import org.stellarium.data.IniFileParser;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import static java.lang.StrictMath.*;

/**
 * Original Comments:<br/>
 * This code slow and ugly and I know it.
 * Yet it might be useful for playing around.
 *
 * @author Jerome Beau
 * @version 0.8.2
 */
public class SphericMirrorCalculator {

    public SphericMirrorCalculator() {
        setParams(new Vector3d(0, -2, 15), new Vector3d(0, 0, 20), 1, 25, 0.0 / 8.0, 1.0);
    }

    public void init(IniFileParser conf) {
        Vector3d projector_position = new Vector3d(conf.getDouble(IniFileParser.SPHERIC_MIRROR_SECTION, "projector_position_x", 0.0),
                conf.getDouble(IniFileParser.SPHERIC_MIRROR_SECTION, "projector_position_z", -0.2),
                conf.getDouble(IniFileParser.SPHERIC_MIRROR_SECTION, "projector_position_y", 1.0));
        Vector3d mirror_position = new Vector3d(conf.getDouble(IniFileParser.SPHERIC_MIRROR_SECTION, "mirror_position_x", 0.0),
                conf.getDouble(IniFileParser.SPHERIC_MIRROR_SECTION, "mirror_position_z", 0.0),
                conf.getDouble(IniFileParser.SPHERIC_MIRROR_SECTION, "mirror_position_y", 2.0));
        double mirror_radius = conf.getDouble(IniFileParser.SPHERIC_MIRROR_SECTION, "mirror_radius", 0.25);
        double dome_radius = conf.getDouble(IniFileParser.SPHERIC_MIRROR_SECTION, "dome_radius", 2.5);
        double zenith_y = conf.getDouble(IniFileParser.SPHERIC_MIRROR_SECTION, "zenith_y", 0.125);
        double scaling_factor = conf.getDouble(IniFileParser.SPHERIC_MIRROR_SECTION, "scaling_factor", 0.8);
        setParams(projector_position,
                mirror_position,
                mirror_radius,
                dome_radius,
                zenith_y,
                scaling_factor);
    }

    public void setParams(Vector3d projectorPosition, Vector3d mirrorPosition, double mirror_radius,
                          double dome_radius, double zenith_y, double scaling_factor) {
        domeCenter = new Vector3d(mirrorPosition);
        domeCenter.scale(-1 / mirror_radius);
        domeRadius = dome_radius / mirror_radius;
        P = new Vector3d();
        P.sub(projectorPosition, mirrorPosition);
        P.scale(1.0 / mirror_radius);
        pp = P.dot(P);
        zoomFactor = sqrt(pp - 1.0) * scaling_factor;
        lP = sqrt(pp);
        p = new Vector3d();
        p.scale(1.0 / lP, P);
        cosAlpha = 1.0;
        sinAlpha = 0.0;
        Point3d xy = new Point3d();
        transform(new Point3d(0, 1, 0), xy);
        double alpha = atan(xy.y / zoomFactor) - atan(zenith_y / zoomFactor);
        cosAlpha = cos(alpha);
        sinAlpha = sin(alpha);
    }

    public boolean transform(Point3d v, Point3d result) {
        //const Vec3d S = DomeCenter + (v * (DomeRadius/v.length()));
        //const Vec3d SmP = S - P;
        Vector3d S = new Vector3d(v);
        S.scale(domeRadius / S.length());
        S.add(domeCenter);
        Vector3d SmP = new Vector3d(S);
        SmP.sub(P);
        double P_SmP = P.dot(SmP);
        boolean rval = ((pp - 1.0) * SmP.dot(SmP) > P_SmP * P_SmP);

        double lS = S.length();
        Vector3d s = new Vector3d(S);
        s.scale(1 / lS);
        double t_min = 0;
        double t_max = 1;
        Vector3d Q = new Vector3d();
        for (int i = 0; i < 10; i++) {
            double t = 0.5 * (t_min + t_max);
            //Q = p*t + s*(1.0-t);
            Vector3d s1mt = new Vector3d(s);
            s1mt.scale(1.0 - t);
            Q = new Vector3d(p);
            Q.scale(t);
            Q.add(s1mt);
            Q.normalize();
            //Vector3d Qp = P - Q;
            Vector3d Qp = new Vector3d(P);
            Qp.sub(Q);
            Qp.normalize();
            Vector3d Qs = new Vector3d(S);
            Qs.sub(Q);
            Qs.normalize();
            Qp.sub(Qs);
            if (Qp.dot(Q) > 0.0) {
                t_max = t;
            } else {
                t_min = t;
            }
        }
        Vector3d x = new Vector3d(Q);
        x.sub(P);
        double zb = (cosAlpha * x.z + sinAlpha * x.y);

        // rotate
        result.x = zoomFactor * x.x / zb;
        result.y = zoomFactor * (cosAlpha * x.y - sinAlpha * x.z) / zb;

        return rval;
    }

    public boolean retransform(double x, double y, Point3d v) {
        x /= zoomFactor;
        y /= zoomFactor;
        v.x = x;
        v.y = y * cosAlpha + sinAlpha;
        v.z = -y * sinAlpha + cosAlpha;
        Vector3d vvect = new Vector3d(v);
        double vv = vvect.dot(vvect);
        double Pv = P.dot(vvect);
        double discr = Pv * Pv - (P.dot(P) - 1.0) * vv;
        if (discr < 0) {
            return false;
        }
        //Vector3d Q = P + v * ((-Pv - sqrt(discr)) / vv);
        Vector3d Q = calcVectorPolynome(P, vvect, (-Pv - sqrt(discr)) / vv);
        //Vector3d w = v - Q * (2 * v.dot(Q));
        Vector3d w = calcVectorPolynome(vvect, Q, -2 * vvect.dot(Q));
        //Vector3d MQ = Q - domeCenter;
        Vector3d MQ = new Vector3d(Q);
        MQ.sub(domeCenter);
        double f = -MQ.dot(w);
        f += sqrt(f * f - (MQ.dot(MQ) - domeRadius * domeRadius) * vv);
        //Vector3d S = Q + w * (f / vv);
        Vector3d S = calcVectorPolynome(Q, w, f / vv);
        //v = S - DomeCenter;
        v.sub(S, domeCenter);
        v.scale(1.0 / domeRadius);
        return true;
    }

    // for calculating partial derivatives:
    public boolean retransform(double x, double y, Vector3d v, Vector3d vX, Vector3d vY) {
        x /= zoomFactor;
        double dx = 1.0 / zoomFactor;
        y /= zoomFactor;
        double dy = 1.0 / zoomFactor;

        v.x = x;
        v.y = y * cosAlpha + sinAlpha;
        v.z = -y * sinAlpha + cosAlpha;

        vX.x = dx;
        vX.y = 0;
        vX.z = 0;

        vY.x = 0;
        vY.y = dy * cosAlpha;
        vY.z = -dy * sinAlpha;

        double vv = v.dot(v);
        double vvX = 2.0 * v.dot(vX);
        double vvY = 2.0 * v.dot(vY);

        double Pv = P.dot(v);
        double PvX = P.dot(vX);
        double PvY = P.dot(vY);

        double discr = Pv * Pv - (P.dot(P) - 1.0) * vv;
        double discrX = 2.0 * Pv * PvX - (P.dot(P) - 1.0) * vvX;
        double discrY = 2.0 * Pv * PvY - (P.dot(P) - 1.0) * vvY;

        if (discr < 0) {
            return false;
        }
        //Vector3d Q = P + v * ((-Pv - sqrt(discr)) / vv);
        double scalar = (-Pv - sqrt(discr)) / vv;
        Vector3d Q = calcVectorPolynome(P, v, scalar);
        //Vector3d Q_x = vX * ((-Pv - sqrt(discr)) / vv)
        //        + v * ((vv * (-PvX - 0.5 * discrX / sqrt(discr))
        //        - vvX * (-Pv - sqrt(discr))) / (vv * vv));
        double scalar2 = (vv * (-PvX - 0.5 * discrX / sqrt(discr))
                - vvX * (-Pv - sqrt(discr))) / (vv * vv);
        Vector3d Q_x = calcVectorPolynome(null, vX, scalar, v, scalar2);

        //Vector3d Q_y = vY * ((-Pv - sqrt(discr)) / vv)
        //        + v * ((vv * (-PvY - 0.5 * discrY / sqrt(discr))
        //        - vvY * (-Pv - sqrt(discr))) / (vv * vv));
        Vector3d Q_y = calcVectorPolynome(null,
                vY, scalar, v, (vv * (-PvY - 0.5 * discrY / sqrt(discr))
                        - vvY * (-Pv - sqrt(discr))) / (vv * vv));

        //Vector3d w = v - Q * (2 * v.dot(Q));
        Vector3d w = calcVectorPolynome(v, Q, -2 * v.dot(Q));
        //Vector3d w_x = vX - Q_x * (2 * v.dot(Q)) - Q * (2 * (vX.dot(Q) + v.dot(Q_x)));
        Vector3d tmp3 = new Vector3d();
        tmp3.scale(-2 * v.dot(Q), Q_x);
        Vector3d w_x = calcVectorPolynome(vX, Q, -2 * (vX.dot(Q) + v.dot(Q_x)));
        w_x.add(tmp3);
        //Vector3d w_y = vY - Q_y * (2 * v.dot(Q)) - Q * (2 * (vY.dot(Q) + v.dot(Q_y)));
        Vector3d tmp4 = new Vector3d();
        tmp4.scale(-2 * v.dot(Q), Q_y);
        Vector3d w_y = calcVectorPolynome(vY, Q, -2 * (vY.dot(Q) + v.dot(Q_y)));
        w_y.add(tmp4);


        //const Vec3d MQ = Q - DomeCenter;
        Vector3d MQ = new Vector3d(Q);
        MQ.sub(domeCenter);
        // MQ_x = Q_x
        // MQ_y = Q_y

        double f = -MQ.dot(w);
        double f_x = -Q_x.dot(w) - MQ.dot(w_x);
        double f_y = -Q_y.dot(w) - MQ.dot(w_y);

        double f1 = f + sqrt(f * f - (MQ.dot(MQ) - domeRadius * domeRadius) * vv);
        double f1_x = f_x + 0.5 * (2 * f * f_x - (MQ.dot(MQ) - domeRadius * domeRadius) * vvX
                - 2 * MQ.dot(Q_x) * vv)
                / sqrt(f * f - (MQ.dot(MQ) - domeRadius * domeRadius) * vv);
        double f1_y = f_y + 0.5 * (2 * f * f_y - (MQ.dot(MQ) - domeRadius * domeRadius) * vvY
                - 2 * MQ.dot(Q_y) * vv)
                / sqrt(f * f - (MQ.dot(MQ) - domeRadius * domeRadius) * vv);

        //Vector3d S = Q + w * (f1 / vv);
        Vector3d S = calcVectorPolynome(Q, w, f1 / vv);
        //Vector3d S_x = Q_x + w * ((vv * f1_x - vvX * f1) / (vv * vv)) + w_x * (f1 / vv);
        Vector3d S_x = calcVectorPolynome(Q_x, w, ((vv * f1_x - vvX * f1) / (vv * vv)), w_x, f1 / vv);
        //Vector3d S_y = Q_y + w*((vv*f1_y-vv_y*f1)/(vv*vv)) + w_y*(f1/vv);
        Vector3d S_y = calcVectorPolynome(Q_y, w, ((vv * f1_y - vvY * f1) / (vv * vv)), w_y, f1 / vv);

        v.sub(S, domeCenter);
        vX.set(S_x);
        vY.set(S_y);

        v.scale(1.0 / domeRadius);
        vX.scale(1.0 / domeRadius);
        vY.scale(1.0 / domeRadius);

        return true;
    }

    private static Vector3d calcVectorPolynome(Vector3d v0, Vector3d v1, double scalar1, Vector3d v2, double scalar2) {
        Vector3d tmp = calcVectorPolynome(v0, v1, scalar1);
        Vector3d result = calcVectorPolynome(tmp, v2, scalar2);
        return result;
    }

    private static Vector3d calcVectorPolynome(Vector3d v0, Vector3d v1, double scalar1) {
        Vector3d result = new Vector3d(v1);
        result.scale(scalar1);
        if (v0 != null)
            result.add(v0);
        return result;
    }

    /**
     * projector
     */
    private Vector3d P;

    private Vector3d domeCenter;

    private double domeRadius;

    private double pp;

    private double lP;

    private Vector3d p;

    private double cosAlpha, sinAlpha;

    private double zoomFactor;
}
