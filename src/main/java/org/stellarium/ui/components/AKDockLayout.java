package org.stellarium.ui.components;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class AKDockLayout extends BorderLayout {
    private ArrayList north = new ArrayList(1);
    private ArrayList south = new ArrayList(1);
    private ArrayList east = new ArrayList(1);
    private ArrayList west = new ArrayList(1);
    private Component center = null;
    private int northHeight, southHeight, eastWidth, westWidth;

    public Object[] curImbeddedTBRs = new Object[4];
    public static final int TOP = SwingConstants.TOP;
    public static final int BOTTOM = SwingConstants.BOTTOM;
    public static final int LEFT = SwingConstants.LEFT;
    public static final int RIGHT = SwingConstants.RIGHT;

    public AKDockLayout() {
        curImbeddedTBRs[0] = north;
        curImbeddedTBRs[1] = south;
        curImbeddedTBRs[2] = west;
        curImbeddedTBRs[3] = east;
    }

    public void addLayoutComponent(Component c, Object con) {
        synchronized (c.getTreeLock()) {
            if (con != null) {
                String s = con.toString();
                c.setVisible(true);
                if (s.equals(NORTH)) {
                    north.add(c);
                } else if (s.equals(SOUTH)) {
                    south.add(c);
                } else if (s.equals(EAST)) {
                    east.add(c);
                } else if (s.equals(WEST)) {
                    west.add(c);
                } else if (s.equals(CENTER)) {
                    center = c;
                }
                c.getParent().validate();
            }
        }
    }

    public void removeLayoutComponent(Component c) {
        north.remove(c);
        south.remove(c);
        east.remove(c);
        west.remove(c);
        if (c == center) {
            center = null;
        }
        flipSeparators(c, SwingConstants.VERTICAL);
        c.getParent().validate();
    }

    public void layoutContainer(Container target) {
        synchronized (target.getTreeLock()) {
            Insets insets = target.getInsets();
            int top = insets.top;
            int bottom = target.getHeight() - insets.bottom;
            int left = insets.left;
            int right = target.getWidth() - insets.right;

            northHeight = getPreferredDimension(north).height;
            southHeight = getPreferredDimension(south).height;
            eastWidth = getPreferredDimension(east).width;
            westWidth = getPreferredDimension(west).width;

            placeComponents(target, north, left, top, right - left,
                    northHeight, TOP);
            top += (northHeight + getVgap());

            placeComponents(target, south, left, bottom - southHeight,
                    right - left, southHeight, BOTTOM);
            bottom -= (southHeight + getVgap());

            placeComponents(target, east, right - eastWidth, top,
                    eastWidth, bottom - top, RIGHT);
            right -= (eastWidth + getHgap());

            placeComponents(target, west, left, top, westWidth,
                    bottom - top, LEFT);
            left += (westWidth + getHgap());

            if (center != null) {
                center.setBounds(left, top,
                        right - left,
                        bottom - top);
            }
        }
    }

    /**
     * Returns the ideal width for a vertically oriented toolbar
     * and the ideal height for a horizontally oriented tollbar:
     */
    private Dimension getPreferredDimension(ArrayList comps) {
        int w = 0, h = 0;

        for (int i = 0; i < comps.size(); i++) {
            Component c = (Component) (comps.get(i));
            Dimension d = c.getPreferredSize();
            w = Math.max(w, d.width);
            h = Math.max(h, d.height);
        }

        return new Dimension(w, h);
    }

    private void placeComponents(Container target, ArrayList comps,
                                 int x, int y, int w, int h, int orientation) {
        int offset = 0;
        Component c = null;

        if (orientation == TOP || orientation == BOTTOM) {

            offset = x;

            int totalWidth = 0;
            int cwidth = 0;
            int num = comps.size();
            for (int i = 0; i < num; i++) {
                c = (Component) (comps.get(i));
                flipSeparators(c, SwingConstants.VERTICAL);
                int widthSwap = totalWidth;
                int cwidthSwap = cwidth;
                cwidth = c.getPreferredSize().width;
                totalWidth += cwidth;
                if (w < totalWidth && i != 0) {
                    Component c0 = (Component) (comps.get(i - 1));
                    Rectangle rec = c0.getBounds();
                    c0.setBounds(rec.x, rec.y, w - widthSwap + cwidthSwap, rec.height);
                    offset = x;
                    if (orientation == TOP) {
                        y += h;
                        northHeight += h;
                    } else if (orientation == BOTTOM) {
                        southHeight += h;
                        y -= h;
                    }
                    totalWidth = cwidth;
                }
                if (i + 1 == num) {
                    c.setBounds(x + offset, y, w - totalWidth + cwidth, h);
                } else {
                    c.setBounds(x + offset, y, cwidth, h);
                    offset += cwidth;
                }
            }
            flipSeparators(c, SwingConstants.VERTICAL);
        } else {
            int totalHeight = 0;
            int cheight = 0;
            int num = comps.size();
            for (int i = 0; i < num; i++) {
                c = (Component) (comps.get(i));
                int heightSwap = totalHeight;
                int cheightSwap = cheight;
                cheight = c.getPreferredSize().height;
                totalHeight += cheight;
                if (h < totalHeight && i != 0) {
                    Component c0 = (Component) (comps.get(i - 1));
                    Rectangle rec = c0.getBounds();
                    c0.setBounds(rec.x, rec.y, rec.width, h - heightSwap + cheightSwap);
                    //offset = y;
                    if (orientation == LEFT) {
                        x += w;
                        westWidth += w;
                    } else if (orientation == RIGHT) {
                        eastWidth += w;
                        x -= w;
                    }
                    totalHeight = cheight;
                    offset = 0;
                }
                if (totalHeight > h) {
                    //cheight = cheight-2;
                }
                if (i + 1 == num) {
                    c.setBounds(x, y + offset, w, h - totalHeight + cheight);
                } else {
                    c.setBounds(x, y + offset, w, cheight);
                    offset += cheight;
                }
            }
            flipSeparators(c, SwingConstants.HORIZONTAL);
        }
    }

    private void flipSeparators(Component c, int orientn) {
        if (c != null && c instanceof JToolBar &&
                UIManager.getLookAndFeel().getName().toLowerCase().indexOf("windows") != -1) {
            JToolBar jtb = (JToolBar) c;
            Component comps[] = jtb.getComponents();
            if (comps != null && comps.length > 0) {
                for (int i = 0; i < comps.length; i++) {
                    try {
                        Component component = comps[i];
                        if (component != null) {
                            if (component instanceof JSeparator) {
                                boolean isVisi = component.isVisible();
                                jtb.remove(component);
                                JSeparator separ = new JSeparator();
                                separ.setVisible(isVisi);
                                if (orientn == SwingConstants.VERTICAL) {
                                    separ.setOrientation(SwingConstants.VERTICAL);
                                    separ.setMinimumSize(new Dimension(2, 6));
                                    separ.setPreferredSize(new Dimension(2, 6));
                                    separ.setMaximumSize(new Dimension(2, 100));
                                } else {
                                    separ.setOrientation(SwingConstants.HORIZONTAL);
                                    separ.setMinimumSize(new Dimension(6, 2));
                                    separ.setPreferredSize(new Dimension(6, 2));
                                    separ.setMaximumSize(new Dimension(100, 2));
                                }
                                jtb.add(separ, i);
                            }
                        }
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        //Gen.propChngSup.firePropertyChange("flipem","",new Double(Math.random()));
    }

    /**
     */
    public boolean containsImbeddedComp(Component c) {
        for (int i = 0; i < this.curImbeddedTBRs.length; i++) {
            if (((ArrayList) curImbeddedTBRs[i]).contains(c)) return true;
        }
        return false;
    }

    /**
     * Description:
     * (SwingConstants top,left,bottom,right):
     * top:1, left:2, bottom:3, right:4
     */
    public boolean containsImbeddedComp(Component c, int inx) {
        if (inx > 0 && inx < 5 && ((ArrayList) curImbeddedTBRs[inx + 1]).contains(c)) return true;
        return false;
    }

}