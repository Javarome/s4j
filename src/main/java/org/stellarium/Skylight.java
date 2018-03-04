package org.stellarium;

import static java.lang.StrictMath.*;

public class Skylight {
    public class SkylightStruct {
        double zenith_angle;// zenith_angle : angular distance to the zenith in radian

        double dist_sun;// dist_sun     : angular distance to the sun in radian

        double color[] = new double[3];// 3 component color, can be RGB or CIE color system
    }

    public static class SkylightStruct2 {
        public double pos[] = new double[3];// Vector to the position (vertical = pos[2])

        public float color[] = new float[3];// 3 component color, can be RGB or CIE color system
    }

    public Skylight() {
    }

    void setParams(float _sun_zenith_angle, float _turbidity) {
        // Set the two Main variables
        thetas = _sun_zenith_angle;
        T = _turbidity;

        // Precomputation of the distribution coefficients and zenith luminances/color
        compute_zenith_luminance();
        compute_zenith_color();
        compute_luminance_distribution_coefs();
        compute_color_distribution_coefs();

        // Precompute everything possible to increase the get_CIE_value() function speed
        double cos_thetas = cos(thetas);
        termX = (float) (zenithColorX / ((1.f + aX * exp(bX)) * (1.f + cX * exp(dX * thetas) + eX * cos_thetas * cos_thetas)));
        termY = (float) (zenith_color_y / ((1.f + Ay * exp(By)) * (1.f + Cy * exp(Dy * thetas) + Ey * cos_thetas * cos_thetas)));
        term_Y = (float) (zenith_luminance / ((1.f + AY * exp(BY)) * (1.f + CY * exp(DY * thetas) + EY * cos_thetas * cos_thetas)));

    }

    public void setParamsV(float[] _sun_pos, float _turbidity) {
        // Store sun position
        sunPos[0] = _sun_pos[0];
        sunPos[1] = _sun_pos[1];
        sunPos[2] = _sun_pos[2];

        // Set the two Main variables
        thetas = (float) (PI / 2 - asin(sunPos[2]));
        T = _turbidity;

        // Precomputation of the distribution coefficients and zenith luminances/color
        compute_zenith_luminance();
        compute_zenith_color();
        compute_luminance_distribution_coefs();
        compute_color_distribution_coefs();

        // Precompute everything possible to increase the get_CIE_value() function speed
        double cos_thetas = sunPos[2];
        termX = (float) (zenithColorX / ((1.f + aX * exp(bX)) * (1.f + cX * exp(dX * thetas) + eX * cos_thetas * cos_thetas)));
        termY = (float) (zenith_color_y / ((1.f + Ay * exp(By)) * (1.f + Cy * exp(Dy * thetas) + Ey * cos_thetas * cos_thetas)));
        term_Y = (float) (zenith_luminance / ((1.f + AY * exp(BY)) * (1.f + CY * exp(DY * thetas) + EY * cos_thetas * cos_thetas)));
    }

    /**
     * Compute CIE luminance for zenith in cd/m^2
     */
    void compute_zenith_luminance() {
        zenith_luminance = (float) (1000.f * ((4.0453f * T - 4.9710f) * tan((0.4444f - T / 120.f) * (PI - 2.f * thetas)) -
                0.2155f * T + 2.4192f));
        if (zenith_luminance <= 0.f) zenith_luminance = 0.00000000001f;
    }

    /**
     * Compute CIE x and y color components
     */
    void compute_zenith_color() {
        thetas2 = thetas * thetas;
        thetas3 = thetas2 * thetas;
        T2 = T * T;

        zenithColorX = (0.00166f * thetas3 - 0.00375f * thetas2 + 0.00209f * thetas) * T2 +
                (-0.02903f * thetas3 + 0.06377f * thetas2 - 0.03202f * thetas + 0.00394f) * T +
                (0.11693f * thetas3 - 0.21196f * thetas2 + 0.06052f * thetas + 0.25886f);

        zenith_color_y = (0.00275f * thetas3 - 0.00610f * thetas2 + 0.00317f * thetas) * T2 +
                (-0.04214f * thetas3 + 0.08970f * thetas2 - 0.04153f * thetas + 0.00516f) * T +
                (0.15346f * thetas3 - 0.26756f * thetas2 + 0.06670f * thetas + 0.26688f);

    }

    /**
     * Compute the luminance distribution coefficients
     */
    void compute_luminance_distribution_coefs() {
        AY = 0.1787f * T - 1.4630f;
        BY = -0.3554f * T + 0.4275f;
        CY = -0.0227f * T + 5.3251f;
        DY = 0.1206f * T - 2.5771f;
        EY = -0.0670f * T + 0.3703f;
    }

    /**
     * Compute the color distribution coefficients
     */
    void compute_color_distribution_coefs() {
        aX = -0.0193f * T - 0.2592f;
        bX = -0.0665f * T + 0.0008f;
        cX = -0.0004f * T + 0.2125f;
        dX = -0.0641f * T - 0.8989f;
        eX = -0.0033f * T + 0.0452f;

        Ay = -0.0167f * T - 0.2608f;
        By = -0.0950f * T + 0.0092f;
        Cy = -0.0079f * T + 0.2102f;
        Dy = -0.0441f * T - 1.6537f;
        Ey = -0.0109f * T + 0.0529f;
    }

    /**
     * Compute the sky color at the given position in the CIE color system and store it in p.color
     * p.color[0] is CIE x color component
     * p.color[1] is CIE y color component
     * p.color[2] is CIE Y color component (luminance)
     */
    void get_xyY_value(SkylightStruct p) {
        double cos_dist_sun = cos(p.dist_sun);
        double one_over_cos_zenith_angle = 1.f / cos(p.zenith_angle);
        p.color[0] = termX * (1.f + aX * exp(bX * one_over_cos_zenith_angle)) * (1.f + cX * exp(dX * p.dist_sun) +
                eX * cos_dist_sun * cos_dist_sun);
        p.color[1] = termY * (1.f + Ay * exp(By * one_over_cos_zenith_angle)) * (1.f + Cy * exp(Dy * p.dist_sun) +
                Ey * cos_dist_sun * cos_dist_sun);
        p.color[2] = term_Y * (1.f + AY * exp(BY * one_over_cos_zenith_angle)) * (1.f + CY * exp(DY * p.dist_sun) +
                EY * cos_dist_sun * cos_dist_sun);
    }

    /**
     * Compute the sky color at the given position in the CIE color system and store it in p.color
     * p.color[0] is CIE x color component
     * p.color[1] is CIE y color component
     * p.color[2] is CIE Y color component (luminance)
     */
    public void get_xyY_valuev(SkylightStruct2 p) {
        //	if (p.pos[2]<0.)
        //	{
        //		p.color[0] = 0.25;
        //		p.color[1] = 0.25;
        //		p.color[2] = 0;
        //		return;
        //	}

        double cosDistSun = sunPos[0] * (p.pos[0]) + sunPos[1] * (p.pos[1]) + sunPos[2] * (p.pos[2]) - 0.0000001f;
        double oneOverCosZenithAngle = 1.f / p.pos[2];
        float distSun = (float) acos(cosDistSun);

        p.color[0] = (float) (termX * (1.f + aX * exp(bX * oneOverCosZenithAngle)) * (1.f + cX * exp(dX * distSun) +
                eX * cosDistSun * cosDistSun));
        p.color[1] = (float) (termY * (1.f + Ay * exp(By * oneOverCosZenithAngle)) * (1.f + Cy * exp(Dy * distSun) +
                Ey * cosDistSun * cosDistSun));
        p.color[2] = (float) (term_Y * (1.f + AY * exp(BY * oneOverCosZenithAngle)) * (1.f + CY * exp(DY * distSun) +
                EY * cosDistSun * cosDistSun));

        if (p.color[2] < 0 || p.color[0] < 0 || p.color[1] < 0) {
            p.color[0] = 0.25f;
            p.color[1] = 0.25f;
            p.color[2] = 0;
        }
    }

    /**
     * Return the current zenith color in xyY color system
     */
    void get_zenith_color(double[] v) {
        v[0] = zenithColorX;
        v[1] = zenith_color_y;
        v[2] = zenith_luminance;
    }

    private float thetas;// angular distance between the zenith and the sun in radian

    float T;// Turbidity : i.e. sky "clarity"

    //  1 : pure air
    //  2 : exceptionnally clear
    //  4 : clear
    //  8 : light haze
    // 25 : haze
    // 64 : thin fog

    // Computed variables depending on the 2 above

    float zenith_luminance;// Y color component of the CIE color at zenith (luminance)

    float zenithColorX;// x color component of the CIE color at zenith

    float zenith_color_y;// y color component of the CIE color at zenith

    double eye_lum_conversion;// luminance conversion for an eye adapted to screen luminance (around 40 cd/m^2)

    double AY, BY, CY, DY, EY;// Distribution coefficients for the luminance distribution function

    float aX, bX, cX, dX, eX;// Distribution coefficients for x distribution function

    double Ay, By, Cy, Dy, Ey;// Distribution coefficients for y distribution function

    float termX;// Precomputed term for x calculation

    float termY;// Precomputed term for y calculation

    float term_Y;// Precomputed term for luminance calculation

    float sunPos[] = new float[3];

    static float thetas2;

    static float thetas3;

    static float T2;
}