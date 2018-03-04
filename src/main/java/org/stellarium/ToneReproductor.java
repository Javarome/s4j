package org.stellarium;

import static java.lang.StrictMath.*;

public class ToneReproductor {

    ToneReproductor() {
        lda = 50.f;
        lwa = 40000.f;
        maxDL = 100.f;
        gamma = 2.3f;

        // Update alpha_da and beta_da values
        double log10Lwa = log10(lwa);
        alphaWa = 0.4f * log10Lwa + 1.519f;
        betaWa = -0.4f * log10Lwa * log10Lwa + 0.218f * log10Lwa + 6.1642f;

        setDisplayAdaptationLuminance(lda);
        setWorldAdaptationLuminance(lwa);
    }

    /**
     * Set the eye adaptation luminance for the display and precompute what can be
     * Usual luminance range is 1-100 cd/m^2 for a CRT screen
     *
     * @param _Lda
     */
    void setDisplayAdaptationLuminance(double _Lda) {
        lda = _Lda;

        // Update alpha_da and beta_da values
        double log10Lda = log10(lda);
        alphaDa = 0.4f * log10Lda + 1.519f;
        betaDa = -0.4f * log10Lda * log10Lda + 0.218f * log10Lda + 6.1642f;

        // Update terms
        alphaWaOverAlphaDa = alphaWa / alphaDa;
        term2 = pow(10.f, (betaWa - betaDa) / alphaDa) / (PI * 0.0001f);
    }

    /**
     * Set the eye adaptation luminance for the world and precompute what can be
     *
     * @param _Lwa
     */
    public void setWorldAdaptationLuminance(double _Lwa) {
        lwa = _Lwa;

        // Update alpha_da and beta_da values
        double log10Lwa = log10(lwa);
        alphaWa = 0.4f * log10Lwa + 1.519f;
        betaWa = -0.4f * log10Lwa * log10Lwa + 0.218f * log10Lwa + 6.1642f;

        // Update terms
        alphaWaOverAlphaDa = alphaWa / alphaDa;
        term2 = pow(10.f, (betaWa - betaDa) / alphaDa) / (PI * 0.0001f);

    }

    /**
     * Convert from xyY color system to RGB according to the adaptation
     * The Y component is in cd/m^2
     */
    public void xyYToRGB(float[] color) {
        // TODO: Fred the parameter should an SColor object
        // 1. Hue conversion
        float log10Y = (float) log10(color[2]);
        // if log10Y>0.6, photopic vision only (with the cones, colors are seen)
        // else scotopic vision if log10Y<-2 (with the rods, no colors, everything blue),
        // else mesopic vision (with rods and cones, transition state)
        if (log10Y < 0.6) {
            // Compute s, ratio between scotopic and photopic vision
            float s = 0.f;
            if (log10Y > -2.f) {
                float op = (log10Y + 2.f) / 2.6f;
                s = 3.f * op * op - 2 * op * op * op;
            }

            // Do the blue shift for scotopic vision simulation (night vision) [3]
            // The "night blue" is x,y(0.25, 0.25)
            color[0] = (1.f - s) * 0.25f + s * color[0];// Add scotopic + photopic components
            color[1] = (1.f - s) * 0.25f + s * color[1];// Add scotopic + photopic components

            // Take into account the scotopic luminance approximated by V [3] [4]
            double V = color[2] * (1.33f * (1.f + color[1] / color[0] + color[0] * (1.f - color[0] - color[1])) - 1.68f);
            color[2] = (float) (0.4468f * (1.f - s) * V + s * color[2]);
        }

        // 2. Adapt the luminance value and scale it to fit in the RGB range [2]
        color[2] = (float) pow(adaptLuminance(color[2]) / maxDL, 1.d / gamma);

        // Convert from xyY to XZY
        double X = color[0] * color[2] / color[1];
        double Y = color[2];
        double Z = (1.f - color[0] - color[1]) * color[2] / color[1];

        // Use a XYZ to Adobe RGB (1998) matrix which uses a D65 reference white
        color[0] = (float) (2.04148f * X - 0.564977f * Y - 0.344713f * Z);
        color[1] = (float) (-0.969258f * X + 1.87599f * Y + 0.0415557f * Z);
        color[2] = (float) (0.0134455f * X - 0.118373f * Y + 1.01527f * Z);
    }

    /**
     * Set the maximum display luminance : default value = 100 cd/m^2
     * This value is used to scale the RGB range
     */
    void set_max_display_luminance(float _maxdL) {
        maxDL = _maxdL;
    }

    /**
     * Set the display gamma : default value = 2.3
     */
    void setDisplayGamma(float _gamma) {
        gamma = _gamma;
    }

    /**
     * Return adapted luminance from world to display
     */
    public double adaptLuminance(double worldLluminance) {
        return pow(worldLluminance * PI * 0.0001d, alphaWaOverAlphaDa) * term2;
    }

    private double lda;// Display luminance adaptation (in cd/m^2)

    double lwa;// World   luminance adaptation (in cd/m^2)

    double maxDL;// Display maximum luminance (in cd/m^2)

    double gamma;// Screen gamma value

    // Precomputed variables
    double alphaDa;

    double betaDa;

    double alphaWa;

    double betaWa;

    double alphaWaOverAlphaDa;

    double term2;
}