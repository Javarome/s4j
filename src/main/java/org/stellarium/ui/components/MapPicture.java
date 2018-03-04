package org.stellarium.ui.components;

/**
 * @author Javarome
 * @version 9 mai 2008 17:01:35
 */
public interface MapPicture {
    String getPositionString();

    String getCursorString();

    void setSize(int width, int height);

    double getPointerLatitude();

    double getPointerLongitude();

    int getPointerAltitude();

    void findPosition(double longitude, double latitude);

    void addCity(String name, String state, String country, double decAngle, double decAngle1, float time, int showatzoom, int alt);

    void setPointerPosition(double longitude, double latitude);

    public interface NearestCityListener {
        void execute();
    }
}
