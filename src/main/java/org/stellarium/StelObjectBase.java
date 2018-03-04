package org.stellarium;

import org.stellarium.projector.Projector;
import org.stellarium.ui.render.STexture;
import org.stellarium.ui.render.STextureFactory;

import javax.vecmath.Point3d;
import java.awt.*;
import java.util.logging.Logger;

/**
 * @author Jerome Beau
 * @version 21 aug 2006 09:09:18
 */
public abstract class StelObjectBase extends StelObject {

    public static final Color DEFAULT_COLOR = Color.WHITE;

    protected final STextureFactory textureFactory;
    protected final Logger logger;

    private static STexture pointerTelescope;

    public StelObjectBase(Logger parentLogger) {
        logger = Logger.getLogger(getClass().getName());
        if (parentLogger != null) {
            logger.setParent(parentLogger);
        }
        textureFactory = new STextureFactory(logger);
        if (pointerTelescope == null) {
            pointerTelescope = textureFactory.createTexture("pointeur2.png");
        }
    }

    public void retain() {
    }

    public void release() {
    }

    /**
     * Tree.Return object's type
     */
    public abstract StelObject.TYPE getType();

    /**
     * Return object's name
     */
    public String getEnglishName() {
        return "";
    }

    public String getNameI18n() {
        return "";
    }

    /**
     * Get position in earth equatorial frame
     */
    public Point3d getEarthEquPos(NavigatorIfc nav) {
        return null;
    }

    /**
     * observer centered J2000 coordinates
     * TODO: Fred Should be abstract method no?
     */
    public Point3d getObsJ2000Pos(NavigatorIfc nav) {
        return null;
    }

    /**
     * @return object's magnitude
     */
    public float getMag(NavigatorIfc nav) {
        return 0;
    }

    /**
     * Method overloading for default null navigator.
     *
     * @return object's magnitude
     */
    public float getMag() {
        return getMag(null);
    }

    /**
     * Get object main color, used to display infos
     */
    public Color getRGB() {
        return DEFAULT_COLOR;
    }

    public StelObject getBrightestStarInConstellation() {
        return StelObject.getUninitializedObject();
    }

    /**
     * Tree.Return the best FOV in degree to use for a close view of the object
     */
    public double getCloseFOV(NavigatorIfc nav) {
        return 10;
    }

    //** Return the best FOV in degree to use for a global view of the object satellite system (if there are satellites) */
    public double getSatellitesFOV(NavigatorIfc nav) {
        return -1;
    }

    public double getParentSatellitesFOV(NavigatorIfc nav) {
        return -1;
    }

    public double getOnScreenSize(Projector prj, NavigatorIfc nav) {
        return 0;
    }
}
