package org.stellarium;

import javax.vecmath.Vector3d;

/**
 * @author <a href="mailto:rr0@rr0.org">Jerome Beau</a>
 * @version $revision$
 */
class CachingOrbit extends Orbit {

    private Vector3d lastPosition;
    double lastTime;

    public Vector3d positionAtTime(double jd) {
        if (jd != lastTime) {
            lastTime = jd;
            lastPosition = computePosition(jd);
        }
        return lastPosition;
    }

    protected Vector3d computePosition(double aPosition) {
        // TODO
        throw new RuntimeException("Not Implemented");
    }

    public void sample(double start, double t, int nSamples, OrbitSampleProc proc) {
        double dt = t / (double) nSamples;
        for (int i = 0; i < nSamples; i++)
            proc.sample(positionAtTime(start + dt * i));
    }
}
