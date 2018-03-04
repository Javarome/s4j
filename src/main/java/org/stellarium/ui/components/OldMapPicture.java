/*
 * User: freds
 * Date: Nov 24, 2006
 * Time: 11:59:20 PM
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
package org.stellarium.ui.components;

import org.stellarium.ui.City;
import org.stellarium.ui.CityMgr;
import org.stellarium.vecmath.Vector2i;

import java.awt.*;
import java.awt.event.*;

/**
 * @see <a href="http://stellarium.cvs.sourceforge.net/stellarium/stellarium/src/stel_sdl.cpp?view=markup&pathrev=stellarium-0-8-2">stel_sdl.cpp</a>
 */
public class OldMapPicture extends StelPicture implements MouseListener, MouseMotionListener, MouseWheelListener, MapPicture {
    private static final int POINTER_SIZE = 10;

    private static final int CITY_SIZE = 5;

    private static final Color CITY_SELECT = new Color(1, 0, 0);

    private static final float ZOOM_LIMIT = 100f;

    enum CityType {
        Named(new Color(1f, .4f, 0f)),
        Unnamed(new Color(.8f, .6f, 0f)),
        Hover(new Color(1f, 1f, 0));

        private Color color;

        CityType(Color color) {
            this.color = color;
        }

        public Color getColor() {
            return color;
        }
    }

    public OldMapPicture(String imageTex, String pointerTex, String cityTex) {
        this(imageTex, pointerTex, cityTex, -1, -1);
    }

    public OldMapPicture(String imageTex, String pointerTex, String cityTex, int xsize, int ysize) {
        super(imageTex, xsize, ysize);

        pointer = new StelPicture(pointerTex, POINTER_SIZE, POINTER_SIZE);
        pointer.setImgColor(CITY_SELECT);

        cityPointer = new StelPicture(cityTex, CITY_SIZE, CITY_SIZE);
        cityPointer.setImgColor(CityType.Hover.getColor());

        setShowEdge(true);// draw our own
        cities.setProximity(30);
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
    }

    public void close() {
        if (pointer != null)
            pointer.close();
        if (cityPointer != null)
            cityPointer.close();
    }

    public void paint(Graphics g) {
        if (!sized) {
            originalSize = getSize();
            originalPosX = getX();
            originalPosY = getX();
            sized = true;
        }

        super.paint(g);

        drawCities(g);
        drawNearestCity(g);

/*if (zoom == 1) {
            pointer.setSize((int) zoom * 6, (int) zoom * 6);
        } else {
            pointer.setSize((int) (zoom * 2), (int) (zoom * 2));
        }
        pointer.setLocation((int) (pointerPos.i0 + getX() - pointer.getSize().getWidth() / 2), (int) (pointerPos.i1 + getY() - pointer.getSize().getHeight() / 2));
       pointer.repaint();
*/

//        scissor.pop();
//        glPopMatrix();

//        painter.drawSquareEdge(originalPos, originalSize);
    }

    public void mouseClicked(MouseEvent e) {
        dragging = false;
        int x = e.getX();
        int y = e.getY();
        if (e.getButton() == MouseEvent.BUTTON1) {
            calcPointerPos(x, y, e.getModifiers());
            onPressCallback();
        }
        e.consume();
    }

    public void mouseDragged(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON3) {
            panning = true;
            oldX = e.getX();
            oldY = e.getY();
        } else {
            dragging = true;
            calcPointerPos(e.getX(), e.getY(), e.getModifiers());
            onPressCallback();
        }
        e.consume();
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    protected void onPressCallback() {
        // To be redefined
    }

    public void mouseWheelMoved(MouseWheelEvent e) {
        int notches = e.getWheelRotation();
        zoomInOut(0.3f * notches);
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    /**
     * virtual bool onMove(int, int);
     *
     * @param e
     */
    public void mouseMoved(MouseEvent e) {
        if (!isVisible())
            return;

        int x = e.getX();
        int y = e.getY();
        if (!isIn(x, y))
            return;

        cursorPos.set(x, y);
        int hoverCityIndex = cities.getNearest(getLongitudeFromX(x), getLatitudeFromY(y));
        if (hoverCityIndex != nearestIndex) {
            repaint();
            nearestIndex = hoverCityIndex;
        }

        if (panning) {
            // make sure can't go further than top left corner
            int l = getX() - (oldX - x);
            if (l > 0) {
                l = 0;
            }

            int t = getY() - (oldY - y);
            if (t > 0) {
                t = 0;
            }

            // check bottom corner
            int r = (int) (l + getSize().getWidth());
            if (r < originalPosX + originalSize.getWidth()) {
                l = (int) (originalPosX + originalSize.getWidth() - getSize().getWidth());
            }

            int b = (int) (t + getSize().getHeight());
            if (b < originalPosY + originalSize.getHeight()) {
                t = (int) (originalPosY + originalSize.getHeight() - getSize().getHeight());
            }

            setLocation(l, t);
        }
        oldX = x;
        oldY = y;
        e.consume();
    }

    public boolean isIn(int x, int y) {
        return (originalPosX <= x && (originalSize.getWidth() + originalPosX) >= x && originalPosY <= y && (originalPosY + originalSize.getHeight()) >= y);
    }

    /**
     * bool onKey(Uint16 k, S_GUI_VALUE s);
     *
     * @param e
     */
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_PAGE_UP:
                zoomInOut(1.f);
                e.consume();
                break;
            case KeyEvent.VK_PAGE_DOWN:
                zoomInOut(-1.f);
                e.consume();
                break;
        }
    }

    public double getPointerLongitude() {
        if (exact)
            return exactLongitude;
        else
            return getLongitudeFromX(pointerPos.i0);
    }

    public double getPointerLatitude() {
        if (exact)
            return exactLatitude;
        else
            return getLatitudeFromY(pointerPos.i1);
    }

    public int getPointerAltitude() {
        if (exact)
            return exactAltitude;
        else
            return 0;
    }

    public void setPointerPosition(double longitude, double latitude) {
        pointerPos.i0 = getXFromLongitude(longitude);
        pointerPos.i1 = getyFromLatitude(latitude);
    }

    public void setOnNearestCityListener(NearestCityListener c) {
        onNearestCityListener = c;
    }

    public void setFont(float fontSize, String fontName) {
//        Font font1 = Font.getFont(fontName);
//        this.cityNameFont = font1.deriveFont(fontSize);
        this.fontSize = fontSize;
    }

    void zoomInOut(float step) {
        float lastZoom = zoom;
        Vector2i oldCursorPos = cursorPos;

        zoom += step;
        if (zoom < 1.f) {
            zoom = 1.f;
        } else if (zoom > ZOOM_LIMIT) {
            zoom = ZOOM_LIMIT;
        }

        cursorPos.i0 = (int) ((float) cursorPos.i0 * zoom / lastZoom);
        cursorPos.i1 = (int) ((float) cursorPos.i1 * zoom / lastZoom);
        pointerPos.i0 = (int) ((float) pointerPos.i0 * zoom / lastZoom);
        pointerPos.i1 = (int) ((float) pointerPos.i1 * zoom / lastZoom);
        setSize((int) (originalSize.getWidth() * zoom), (int) (originalSize.getHeight() * zoom));

        int newX = getX(), newY = getY();
        cursorPos.minus(oldCursorPos);
        setLocation(getX() - cursorPos.i0, getY() - cursorPos.i1);

        if (getX() > originalPosX)
            newX = originalPosX;

        if (getY() > originalPosY)
            newY = originalPosY;

        if (getX() + getSize().getWidth() < originalPosX + originalSize.getWidth())
            newX = (int) (originalPosX + originalSize.getWidth() - getSize().getWidth());

        if (getY() + getSize().getHeight() < originalPosY + originalSize.getHeight())
            newY = (int) (originalPosY + originalSize.getHeight() - getSize().getHeight());

        setLocation(newX, newY);
    }

    public void addCity(String name, String state, String country,
                        double longitude, double latitude, float zone, int showatzoom, int altitude) {
        cities.addCity(name, state, country, longitude, latitude, zone, showatzoom, altitude);
    }

    public String getPositionString() {
        if (pointerIndex == -1)
            return SGUI.UNKNOWN_OBSERVATORY;
        else
            return getLocationString(pointerIndex);
    }

    public String getCursorString() {
        if (nearestIndex == -1)
            return "";
        else
            return getLocationString(nearestIndex);
    }

    public void findPosition(double longitude, double latitude) {
        // get 1 second accuracy to locate the city
        cities.setProximity(1.f / 3600);
        pointerIndex = cities.getNearest(longitude, latitude);
        cities.setProximity(-1);
    }

    private String getCity(int index) {
        if (index != -1) return cities.getCity(index).getName();
        else return "";
    }

    private String getState(int index) {
        if (index != -1) return cities.getCity(index).getState();
        else return "";
    }

    private String getCountry(int index) {
        if (index != -1) return cities.getCity(index).getCountry();
        else return "";
    }

    private String getLocationString(int index) {
        if (index == -1)
            return "";

        String city = getCity(index) + ", ";
        String state = getState(index);
        String country = getCountry(index);

        if ("<>".equals(state))
            state = "";
        else
            state += ", ";

        return city + state + country;
    }

    private int getXFromLongitude(double longitude) {
        return (int) ((longitude + 180.f) / 360.f * getSize().getWidth());
    }

    private int getyFromLatitude(double latitude) {
        return (int) (getSize().getHeight() - (int) ((latitude + 90.f) / 180.f * getSize().getHeight()));
    }

    private double getLongitudeFromX(int x) {
        return (double) x / getSize().getWidth() * 360.f - 180.f;
    }

    private double getLatitudeFromY(int y) {
        return (1.f - (float) y / getSize().getHeight()) * 180.f - 90.f;
    }

    private void drawCities(Graphics g) {
        int cityzoom;

        for (int i = 0; i < cities.size(); i++) {
            City city = cities.getCity(i);
            cityzoom = city.getShowatzoom();
            int cityPosX = getX() + getXFromLongitude(city.getLongitude());
            int cityPosY = getY() + getyFromLatitude(city.getLatitude());
            // draw the city name if at least at that zoom level, or it is a selected city
            if (cityzoom != 0 && cityzoom <= (int) zoom || (pointerIndex != -1 && i == pointerIndex)) {
//                drawCity(g, cityPos, CityType.Named);
                if (i == pointerIndex) {
                    g.setColor(CITY_SELECT);
                } else {
                    g.setColor(CityType.Named.getColor());
                }
                drawCityName(g, cityPosX, cityPosY, city.getNameI18());
            } else {
//                drawCity(g, cityPos, CityType.Unnamed);
            }
        }
    }

    private void drawCityName(Graphics g, int cityPosX, int cityPosY, String cityName) {
        int x, y, strLen;

        // don't draw if not in the current view (ok in the y axis!)
        if ((cityPosX > originalPosX + originalSize.getWidth()) || (cityPosX < originalPosX)) {
            return;
        }

        y = cityPosY - (int) (fontSize / 2);
        if (cityNameFont == null) {
            cityNameFont = g.getFont();
        }
//            g.setFont(cityNameFont);
        FontMetrics fontMetrics = g.getFontMetrics();
        strLen = (int) fontMetrics.getStringBounds(cityName, g).getWidth();
        x = (int) (cityPosX + cityPointer.getSize().getWidth() / 2 + 1);
        if (x + strLen + 1 >= originalPosX + originalSize.getWidth()) {
            x = (int) (cityPosX - cityPointer.getSize().getWidth() / 2 - 1 - strLen);
        }
        g.drawString(cityName, x, y);
    }

    private void drawCity(Graphics g, int cityPosX, int cityPosY, CityType ctype) {
        if (zoom == 1) {
            cityPointer.setSize((int) zoom, (int) zoom);
        } else {
            cityPointer.setSize((int) zoom / 3, (int) zoom / 3);
        }

        cityPointer.setImgColor(ctype.getColor());
        cityPointer.setLocation((int) (cityPosX - cityPointer.getSize().getWidth() / 2),
                (int) (cityPosY - cityPointer.getSize().getHeight() / 2));
        cityPointer.repaint();
    }

    private void drawNearestCity(Graphics g) {
        int lastIndex = nearestIndex;

        if ((dragging && nearestIndex == pointerIndex && nearestIndex != -1) || (!dragging && nearestIndex != -1)) {
            int cityPosX = getX() + getXFromLongitude(cities.getCity(nearestIndex).getLongitude());
            int cityPosY = getY() + getyFromLatitude(cities.getCity(nearestIndex).getLatitude());
            cityPointer.setImgColor(CityType.Hover.getColor());
            drawCity(g, cityPosX, cityPosY, CityType.Hover);
            g.setColor(CityType.Hover.getColor());
            drawCityName(g, cityPosX, cityPosY, cities.getCity(nearestIndex).getNameI18());
            if (onNearestCityListener != null) {
                onNearestCityListener.execute();
            }
            return;
        }

        // if dragging - leave the city along
        if (dragging) {
            nearestIndex = -1;
            if (onNearestCityListener != null) {
                onNearestCityListener.execute();
            }
            return;
        }

        if (lastIndex != -1 && nearestIndex == -1)
            if (onNearestCityListener != null) {
                onNearestCityListener.execute();
            }
    }

    private void calcPointerPos(int x, int y, int modifiers) {
        if ((modifiers & KeyEvent.CTRL_MASK) != 0) {
            nearestIndex = cities.getNearest(getLongitudeFromX(x - getX()), getLatitudeFromY(y - getY()));
            if (nearestIndex != -1) {
                //String n = cities.getCity(nearestIndex).getNameI18();
                //double lat, lon;
                exactLongitude = cities.getCity(nearestIndex).getLongitude();
                exactLatitude = cities.getCity(nearestIndex).getLatitude();
                exactAltitude = cities.getCity(nearestIndex).getAltitude();
                //lat = exactLatitude;
                //lon = exactLongitude;
                pointerPos.i0 = getXFromLongitude(exactLongitude);
                pointerPos.i1 = getyFromLatitude(exactLatitude);
                exact = true;

                // lock onto this selected city
                pointerIndex = nearestIndex;
                return;
            }
        }

        // just roaming
        pointerIndex = -1;
        pointerPos.i0 = x - getX();
        pointerPos.i1 = y - getY();
        exact = false;
    }

    private NearestCityListener onNearestCityListener;

    private StelPicture pointer;

    private StelPicture cityPointer;

    private Font cityNameFont;

    private Vector2i pointerPos = new Vector2i();
    private int oldX;
    private int oldY;

    private Dimension originalSize;
    private int originalPosX;
    private int originalPosY;

    private Vector2i cursorPos = new Vector2i();

    private boolean panning, dragging;

    private float zoom = 1.f;

    private boolean sized;

    private CityMgr cities = new CityMgr();

    private int nearestIndex = -1;

    private int pointerIndex = -1;

    private boolean exact;

    private double exactLatitude, exactLongitude;

    private int exactAltitude;

    private float fontSize;
}
