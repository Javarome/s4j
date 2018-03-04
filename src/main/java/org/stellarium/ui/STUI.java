package org.stellarium.ui;

/*
* Stellarium for Java
* Copyright (c) 2005 Jerome Beau
*
* Java adaptation of <a href="http://www.stellarium.org">Stellarium</a>
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

import org.stellarium.StellariumException;

import javax.vecmath.Vector3d;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.util.LinkedList;
import java.util.Map;

/**
 * Class which manages a Text User Interface "widgets"
 */
public class STUI {

    enum S_TUI_VALUE {
        S_TUI_PRESSED,
        S_TUI_RELEASED
    }

    public static final char START_ACTIVE = '\22';

    public static final char STOP_ACTIVE = '\21';

    static class Component extends KeyAdapter {
        /**
         * white is hilight
         */

        public String getString() {
            return new String();
        }

        /**
         *
         */
        public void keyPressed(KeyEvent e) {
            e.consume();
        }

        boolean isEditable() {
            return false;
        }

        void setActive(boolean a) {
            active = a;
        }

        boolean getActive() {
            return active;
        }

        /**
         * Same function as getString but cleaned of every color informations
         */
        String getCleanString() {
            String result = "", s = getString();
            for (int i = 0; i < s.length(); ++i) {
                if (s.charAt(i) != START_ACTIVE && s.charAt(i) != STOP_ACTIVE) {
                    result += s.charAt(i);
                }
            }
            return result;
        }

        protected boolean active;
    }

    /**
     * Base class. Note that the method boolean isEditable() has to be overrided by returning true
     * for all the non passives components.
     * Store a Callback on a function taking no parameters
     */
    static class CallbackComponent extends Component {
        void setOnChangeCallback(Callback c) {
            onChangeCallback = c;
        }

        protected Callback onChangeCallback;

        public void keyTyped(KeyEvent e) {
        }

        public void keyReleased(KeyEvent e) {
        }
    }

    static class Container extends CallbackComponent {

        // tui Return Values:

        void addComponent(Component c) {
            childs.add(c);
        }

        public String getString() {
            String s = "";
            for (Component iter : childs) {
                s += iter.getString();
            }
            return s;
        }

        public void keyPressed(KeyEvent e) {
            for (Component iter : childs) {
                iter.keyPressed(e);
                if (e.isConsumed()) {
                    e.consume();// The signal has been intercepted
                    return;
                }
            }
        }

        protected LinkedList<Component> childs;
    }

    /**
     * Component which manages 2 states
     */
    static class Bistate extends CallbackComponent {
        public Bistate(boolean initState) {
            this.state = initState;
        }

        public String getString() {
            return state ? string_activated : string_disabled;
        }

        boolean getValue() {
            return state;
        }

        void setValue(boolean s) {
            state = s;
        }

        protected
        String string_activated;

        String string_disabled;

        boolean state;
    }

    static class Branch extends Container {

        Branch() {
            current = childs.getFirst();
        }

        public String getString() {
            if (current == null) {
                return new String();
            } else {
                return getCurrent().getString();
            }
        }

        void addComponent(Component c) {
            super.addComponent(c);
            if (childs.size() == 1) current = childs.getFirst();
        }

        boolean setValue(String s) {
            for (Component c : childs) {
                System.out.println(c.getCleanString());
                if (c.getCleanString().equals(s)) {
                    current = c;
                    return true;
                }
            }
            return false;
        }

        boolean setValueSpecialSlash(String s) {
            for (Component c : childs) {
                String cs = c.getCleanString();
                int pos = cs.indexOf('/');
                String ccs = cs.substring(0, pos);
                if (ccs.equals(s)) {
                    current = c;
                    return true;
                }
            }
            return false;
        }

        public void keyPressed(KeyEvent e) {
            if (current != null) {
                super.keyPressed(e);
                if (!e.isConsumed()) {
                    int k = e.getKeyCode();
                    switch (k) {
                        case KeyEvent.VK_UP:
                            if (current != childs.getFirst()) current = childs.get(childs.indexOf(current) - 1);
                            else current = childs.getLast();
                            e.consume();
                            break;
                        case KeyEvent.VK_DOWN:
                            if (current != childs.getLast()) current = childs.get(childs.indexOf(current) + 1);
                            else current = childs.getFirst();
                            e.consume();
                            break;
                    }
                }
            }
        }

        Component getCurrent() {
            if (current == childs.getLast()) return null;
            else return current;
        }

        protected Component current;
    }

    static class MenuBranch extends Branch {

        public MenuBranch(String label) {
            this.label = label;
        }

        public void keyPressed(KeyEvent e) {
            if (isNavigating) {
                if (isEditing) {
                    getCurrent().keyPressed(e);
                    if (e.isConsumed()) {
                        return;
                    }
                    int k = e.getKeyCode();
                    if (k == KeyEvent.VK_LEFT || k == KeyEvent.VK_ESCAPE || k == KeyEvent.VK_ENTER) {
                        isEditing = false;
                    }
                    e.consume();
                } else {
                    int k = e.getKeyCode();
                    if (k == KeyEvent.VK_UP) {
                        if (current != childs.getFirst()) current = childs.get(childs.indexOf(current) - 1);
                        e.consume();
                    }
                    if (k == KeyEvent.VK_DOWN) {
                        if (current != childs.getLast()) current = childs.get(childs.indexOf(current) + 1);
                        e.consume();
                    }
                    if (k == KeyEvent.VK_RIGHT || k == KeyEvent.VK_ENTER) {
                        if (current.isEditable()) isEditing = true;
                        e.consume();
                    }
                    if (k == KeyEvent.VK_LEFT || k == KeyEvent.VK_ESCAPE) {
                        isNavigating = false;
                        e.consume();
                    }
                }
            } else {
                int k = e.getKeyCode();
                if (k == KeyEvent.VK_RIGHT || k == KeyEvent.VK_ENTER) {
                    isNavigating = true;
                    e.consume();
                }
            }
        }

        /**
         * Passive widget which only display text
         */
        class LabelItem extends Component {
            public String getString() {
                return label;
            }

            void setLabel(String s) {
                label = s;
            }

            protected
            String label;
        }

        String MenugetString() {
            if (!isNavigating) return label;
            if (isEditing) getCurrent().setActive(true);
            String s = getString();
            if (isEditing) getCurrent().setActive(false);
            return s;
        }

        boolean isEditable() {
            return true;
        }

        String getLabel() {
            return label;
        }

        protected String label;

        boolean isNavigating;

        boolean isEditing;
    }

    static class MenuBranchItem extends Branch {
        MenuBranchItem(String s) {
            label = s;
        }

        public void keyPressed(KeyEvent e) {
            if (isEditing) {
                getCurrent().keyPressed(e);
                if (e.isConsumed()) {
                    return;
                }
                int k = e.getKeyCode();
                if (k == KeyEvent.VK_LEFT || k == KeyEvent.VK_ESCAPE || k == KeyEvent.VK_ENTER) {
                    isEditing = false;
                }
                e.consume();
            } else {
                int k = e.getKeyCode();
                if (k == KeyEvent.VK_UP) {
                    if (current != childs.getFirst()) current = childs.get(childs.indexOf(current) - 1);
                    if (onChangeCallback != null) {
                        onChangeCallback.execute();
                    }
                    e.consume();
                }
                if (k == KeyEvent.VK_DOWN) {
                    if (current != childs.getLast()) current = childs.get(childs.indexOf(current) + 1);
                    if (onChangeCallback != null) {
                        onChangeCallback.execute();
                    }
                    e.consume();
                }
                if (k == KeyEvent.VK_RIGHT || k == KeyEvent.VK_ENTER) {
                    if (current.isEditable()) isEditing = true;
                    e.consume();
                }
                if (k == KeyEvent.VK_LEFT || k == KeyEvent.VK_ESCAPE) {
                    //
                }
            }
        }

        public String getString() {
            if (active) {
                current.setActive(true);
            }
            String s = label + getString();
            if (active) {
                current.setActive(false);
            }
            return s;
        }

        boolean isEditable() {
            return true;
        }

        String getLabel() {
            return label;
        }

        protected String label;

        boolean isEditing;
    }

    static class BooleanItem extends Bistate {

        BooleanItem(boolean initState, String _label, String someStringActivated, String someStringDisabled) {
            super(initState);
            this.label = _label;
            string_activated = someStringActivated;
            string_disabled = someStringDisabled;
        }

        public void keyPressed(KeyEvent e) {
            int k = e.getKeyCode();
            if (k == KeyEvent.VK_UP || k == KeyEvent.VK_DOWN) {
                state = !state;
                if (onChangeCallback != null) {
                    onChangeCallback.execute();
                }
                e.consume();
            }
        }

        public String getString() {
            return label + (active ? START_ACTIVE : "") +
                    (state ? string_activated : string_disabled) +
                    (active ? STOP_ACTIVE : "");
        }

        boolean isEditable() {
            return true;
        }

        protected
        String label;
    }

    /**
     * Component which manages integer value
     */
    static class SInteger extends CallbackComponent {
        public SInteger(int initValue) {
            super();
            value = initValue;
        }

        int getValue() {
            return value;
        }

        void setValue(int v) {
            value = v;
        }

        protected int value;
    }

    /**
     * Component which manages decimal (double) value
     */
    static class Decimal extends CallbackComponent {
        public Decimal(double value) {
            this.value = value;
        }

        double getValue() {
            return value;
        }

        void setValue(double v) {
            value = v;
        }

        protected double value;

        public String getString() {
            return (active ? "" + START_ACTIVE : "") + value + (active ? STOP_ACTIVE : "");
        }
    }

    /**
     * SInteger item widget. The Callback function is called when the value is changed
     */
    static class IntegerItem extends SInteger {
        IntegerItem(int someMin, int someMax, int initValue) {
            this(initValue, someMax, someMin, new String());
        }

        IntegerItem(int someMin, int someMax, int initValue, String someLabel) {
            super(initValue);
            mmin = someMin;
            mmax = someMax;
            label = someLabel;
        }

        boolean isEditable() {
            return true;
        }

        protected boolean numInput;

        protected String strInput;

        protected int mmin, mmax;

        protected String label = new String();

        public void keyPressed(KeyEvent e) {
            if (!numInput) {
                int k = e.getKeyCode();
                switch (k) {
                    case KeyEvent.VK_UP:
                        ++value;
                        if (value > mmax) {
                            value = mmax;
                            e.consume();
                            return;
                        }
                        if (onChangeCallback != null) {
                            onChangeCallback.execute();
                        }
                        e.consume();
                        break;
                    case KeyEvent.VK_DOWN:
                        --value;
                        if (value < mmin) {
                            value = mmin;
                            e.consume();
                            return;
                        }
                        if (onChangeCallback != null) {
                            onChangeCallback.execute();
                        }
                        e.consume();
                        break;
                    case KeyEvent.VK_0:
                    case KeyEvent.VK_1:
                    case KeyEvent.VK_2:
                    case KeyEvent.VK_3:
                    case KeyEvent.VK_4:
                    case KeyEvent.VK_5:
                    case KeyEvent.VK_6:
                    case KeyEvent.VK_7:
                    case KeyEvent.VK_8:
                    case KeyEvent.VK_9:
                    case KeyEvent.VK_MINUS:
                        // Start editing with numerical numbers
                        numInput = true;
                        strInput = "" + (char) k;
                        e.consume();
                        break;
                }
            } else {// numInput == true
                int k = e.getKeyCode();
                switch (k) {
                    case KeyEvent.VK_ENTER: {
                        numInput = false;
                        value = Integer.parseInt(strInput);
                        if (value > mmax) value = mmax;
                        if (value < mmin) value = mmin;
                        if (onChangeCallback != null) {
                            onChangeCallback.execute();
                        }
                        e.consume();
                    }
                    break;
                    case KeyEvent.VK_UP: {
                        value = Integer.parseInt(strInput);
                        ++value;
                        if (value > mmax) value = mmax;
                        if (value < mmin) value = mmin;
                        strInput = String.valueOf(value);
                        e.consume();
                    }
                    break;
                    case KeyEvent.VK_DOWN: {
                        value = Integer.parseInt(strInput);
                        --value;
                        if (value > mmax) value = mmax;
                        if (value < mmin) value = mmin;
                        strInput = String.valueOf(value);
                        e.consume();
                    }
                    break;
                    case KeyEvent.VK_0:
                    case KeyEvent.VK_1:
                    case KeyEvent.VK_2:
                    case KeyEvent.VK_3:
                    case KeyEvent.VK_4:
                    case KeyEvent.VK_5:
                    case KeyEvent.VK_6:
                    case KeyEvent.VK_7:
                    case KeyEvent.VK_8:
                    case KeyEvent.VK_9:
                    case KeyEvent.VK_MINUS:
                        // The user was already editing
                        strInput += (char) k;
                        e.consume();
                        break;
                    case KeyEvent.VK_ESCAPE:
                        numInput = false;
                        break;
                    default:
                        e.consume();// Block every other characters
                }
            }
        }

        public String getString() {
            String os;

            if (numInput) os = label + (active ? START_ACTIVE : "") + strInput + (active ? STOP_ACTIVE : "");
            else os = label + (active ? START_ACTIVE : "") + value + (active ? STOP_ACTIVE : "");
            return os;
        }
    }

    static class DecimalItem extends Decimal implements KeyListener {
        public DecimalItem(double min, double max, double initValue, String someLabel, double someDelta) {
            super(initValue);
            this.mmin = min;
            this.mmax = max;
            label = someLabel;
            delta = someDelta;
        }

        public DecimalItem(double min, double max, double initValue, String someLabel) {
            this(min, max, initValue, someLabel, 1);
        }

        public void keyPressed(KeyEvent e) {
            int k = e.getKeyCode();
            if (!numInput) {
                switch (k) {
                    case KeyEvent.VK_UP:
                        value += delta;
                        if (value > mmax) {
                            value = mmax;
                            e.consume();
                            return;
                        }
                        if (onChangeCallback != null) {
                            onChangeCallback.execute();
                        }
                        e.consume();
                        break;
                    case KeyEvent.VK_DOWN:
                        value -= delta;
                        if (value < mmin) {
                            value = mmin;
                            e.consume();
                            return;
                        }
                        if (onChangeCallback != null) {
                            onChangeCallback.execute();
                        }
                        e.consume();
                        break;
                    case KeyEvent.VK_0:
                    case KeyEvent.VK_1:
                    case KeyEvent.VK_2:
                    case KeyEvent.VK_3:
                    case KeyEvent.VK_4:
                    case KeyEvent.VK_5:
                    case KeyEvent.VK_6:
                    case KeyEvent.VK_7:
                    case KeyEvent.VK_8:
                    case KeyEvent.VK_9:
                    case KeyEvent.VK_PERIOD:
                    case KeyEvent.VK_MINUS:
                        // Start editing with numerical numbers
                        numInput = true;
                        strInput = "" + k;
                        e.consume();
                        break;
                }
            } else {// numInput == true
                switch (k) {
                    case KeyEvent.VK_ENTER:
                    case KeyEvent.VK_LEFT: {
                        numInput = false;
                        value = Integer.parseInt(strInput);
                        if (value > mmax) value = mmax;
                        if (value < mmin) value = mmin;
                        if (onChangeCallback != null) {
                            onChangeCallback.execute();
                        }
                    }
                    break;
                    case KeyEvent.VK_UP: {
                        value = Integer.parseInt(strInput);
                        value += delta;
                        if (value > mmax) value = mmax;
                        if (value < mmin) value = mmin;
                        strInput = String.valueOf(value);
                        e.consume();
                    }
                    break;
                    case KeyEvent.VK_DOWN:
                        value = Double.valueOf(strInput);
                        value -= delta;
                        if (value > mmax) value = mmax;
                        if (value < mmin) value = mmin;
                        strInput = String.valueOf(value);
                        e.consume();
                        break;
                    case KeyEvent.VK_0:
                    case KeyEvent.VK_1:
                    case KeyEvent.VK_2:
                    case KeyEvent.VK_3:
                    case KeyEvent.VK_4:
                    case KeyEvent.VK_5:
                    case KeyEvent.VK_6:
                    case KeyEvent.VK_7:
                    case KeyEvent.VK_8:
                    case KeyEvent.VK_9:
                    case KeyEvent.VK_PERIOD:
                    case KeyEvent.VK_MINUS:
                        // The user was already editing
                        strInput += (char) k;
                        e.consume();
                        break;
                    case KeyEvent.VK_ESCAPE:
                        numInput = false;
                        return;
                    default:
                        e.consume();// Block every other characters
                }
            }
        }

        public String getString() {
            String os;

            // Can't directly write value in os because there is a float precision limit bug..
            String vstr = "" + value;

            if (numInput) os = label + (active ? START_ACTIVE : "") + strInput + (active ? STOP_ACTIVE : "");
            else os = label + (active ? START_ACTIVE : "") + vstr + (active ? STOP_ACTIVE : "");
            return os;
        }

        boolean isEditable() {
            return true;
        }

        protected boolean numInput;

        String strInput;

        double mmin, mmax;

        String label;

        double delta = 1;
    }

    static class TimeItem extends CallbackComponent {
        TimeItem(String _label, double _JD) {
            this.label = _label;
            this.JD = _JD;
            y = new IntegerItem(-100000, 100000, 2003);
            m = new IntegerItem(1, 12, 1);
            d = new IntegerItem(1, 31, 1);
            h = new IntegerItem(0, 23, 0);
            mn = new IntegerItem(0, 59, 0);
            s = new IntegerItem(0, 59, 0);
            current_edit = y;
        }

        TimeItem(String _label) {
            this(_label, 2451545.0);
        }

        public void keyPressed(KeyEvent e) {
            int k = e.getKeyCode();
            current_edit.keyPressed(e);
            if (e.isConsumed()) {
                computeJD();
                computeYmdhms();
                if (onChangeCallback != null) {
                    onChangeCallback.execute();
                }
                e.consume();
            } else {
                switch (k) {
                    case KeyEvent.VK_ESCAPE:
                        break;
                    case KeyEvent.VK_RIGHT:
                        if (current_edit == y) current_edit = m;
                        else if (current_edit == m) current_edit = d;
                        else if (current_edit == d) current_edit = h;
                        else if (current_edit == h) current_edit = mn;
                        else if (current_edit == mn) current_edit = s;
                        else if (current_edit == s) current_edit = y;
                        e.consume();
                        break;
                    case KeyEvent.VK_LEFT:
                        if (current_edit == y) current_edit = s;
                        else if (current_edit == m) current_edit = y;
                        else if (current_edit == d) current_edit = m;
                        else if (current_edit == h) current_edit = d;
                        else if (current_edit == mn) current_edit = h;
                        else if (current_edit == s) current_edit = mn;
                        e.consume();
                        break;
                }
            }
        }

        /**
         * Convert Julian day to yyyy/mm/dd hh:mm:ss and return the String
         */
        public String getString() {
            computeYmdhms();

            StringBuffer s1 = new StringBuffer();
            StringBuffer s2 = new StringBuffer();
            if (current_edit == y && active) {
                s1.append(START_ACTIVE);
                s2.append(STOP_ACTIVE);
            }
            if (current_edit == m && active) {
                s1.append(START_ACTIVE);
                s2.append(STOP_ACTIVE);
            }
            if (current_edit == d && active) {
                s1.append(START_ACTIVE);
                s2.append(STOP_ACTIVE);
            }
            if (current_edit == h && active) {
                s1.append(START_ACTIVE);
                s2.append(STOP_ACTIVE);
            }
            if (current_edit == mn && active) {
                s1.append(START_ACTIVE);
                s2.append(STOP_ACTIVE);
            }
            if (current_edit == s && active) {
                s1.append(START_ACTIVE);
                s2.append(STOP_ACTIVE);
            }

            return label +
                    s1.charAt(0) + y.getString() + s2.charAt(0) + "/" +
                    s1.charAt(1) + m.getString() + s2.charAt(1) + "/" +
                    s1.charAt(2) + d.getString() + s2.charAt(2) + " " +
                    s1.charAt(3) + h.getString() + s2.charAt(3) + ":" +
                    s1.charAt(4) + mn.getString() + s2.charAt(4) + ":" +
                    s1.charAt(5) + s.getString() + s2.charAt(5);
        }

        /**
         * Code originally from libnova which appeared to be totally wrong... New code from celestia
         */
        void computeYmdhms() {
            int a = (int) (JD + 0.5);
            double c;
            if (a < 2299161) {
                c = a + 1524;
            } else {
                double b = (int) ((a - 1867216.25) / 36524.25);
                c = a + b - (int) (b / 4) + 1525;
            }

            int dd = (int) ((c - 122.1) / 365.25);
            int e = (int) (365.25 * dd);
            int f = (int) ((c - e) / 30.6001);

            double dday = c - e - (int) (30.6001 * f) + ((JD + 0.5) - (int) (JD + 0.5));

            ymdhms[1] = f - 1 - 12 * (f / 14);
            ymdhms[0] = dd - 4715 - (int) ((7.0 + ymdhms[1]) / 10.0);
            ymdhms[2] = (int) dday;

            double dhour = (dday - ymdhms[2]) * 24;
            ymdhms[3] = (int) dhour;

            double dminute = (dhour - ymdhms[3]) * 60;
            ymdhms[4] = (int) dminute;

            second = (dminute - ymdhms[4]) * 60;

            y.setValue(ymdhms[0]);
            m.setValue(ymdhms[1]);
            d.setValue(ymdhms[2]);
            h.setValue(ymdhms[3]);
            mn.setValue(ymdhms[4]);
            s.setValue((int) Math.round(second));
        }

        /**
         * Code originally from libnova which appeared to be totally wrong... New code from celestia
         */
        void computeJD() {
            ymdhms[0] = y.getValue();
            ymdhms[1] = m.getValue();
            ymdhms[2] = d.getValue();
            ymdhms[3] = h.getValue();
            ymdhms[4] = mn.getValue();
            second = s.getValue();

            int y = ymdhms[0], m = ymdhms[1];
            if (ymdhms[1] <= 2) {
                y = ymdhms[0] - 1;
                m = ymdhms[1] + 12;
            }

            // Correct for the lost days in Oct 1582 when the Gregorian calendar
            // replaced the Julian calendar.
            int B = -2;
            if (ymdhms[0] > 1582 || (ymdhms[0] == 1582 && (ymdhms[1] > 10 || (ymdhms[1] == 10 && ymdhms[2] >= 15)))) {
                B = y / 400 - y / 100;
            }

            JD = (Math.floor(365.25 * y) +
                    Math.floor(30.6001 * (m + 1)) + B + 1720996.5 +
                    ymdhms[2] + ymdhms[3] / 24.0 + ymdhms[4] / 1440.0 + second / 86400.0);
        }

        boolean isEditable() {
            return true;
        }

        double getJDay() {
            return JD;
        }

        void setJDay(double jd) {
            JD = jd;
        }

        protected double JD;

        protected IntegerItem current_edit;// 0 to 5 year to second

        protected String label;

        protected int ymdhms[] = new int[5];

        protected double second;

        protected IntegerItem y, m, d, h, mn, s;
    }

    interface Callback {
        void execute() throws StellariumException;
    }

    /**
     * List item widget. The Callback function is called when the selected item changes
     */
    static class MultiSetItem<T> extends CallbackComponent {
        MultiSetItem(String _label) {
            label = _label;
            current = items.getLast();
        }

        MultiSetItem(MultiSetItem m) {
            label = m.label;
            setCurrent((T) m.getCurrent());
        }

        public String getString() {
            if (current == items.getLast()) return label;
            return label + (active ? START_ACTIVE : "") + current + (active ? STOP_ACTIVE : "");
        }

        boolean isEditable() {
            return true;
        }

        public void keyPressed(KeyEvent e) {
            if (current == items.getLast()) return;
            int k = e.getKeyCode();
            switch (k) {
                case KeyEvent.VK_ENTER:
                    if (onTriggerCallback != null) {
                        onTriggerCallback.execute();
                    }
                    break;
                case KeyEvent.VK_UP:
                    if (current != items.getFirst()) {
                        current = items.get(items.indexOf(current) - 1);
                    } else {
                        current = items.getLast();
                    }
                    if (onChangeCallback != null) {
                        onChangeCallback.execute();
                    }
                    e.consume();
                    break;
                case KeyEvent.VK_DOWN:
                    if (current != items.getLast()) {
                        current = items.get(items.indexOf(current) + 1);
                    } else {
                        current = items.getFirst();
                    }
                    if (onChangeCallback != null) {
                        onChangeCallback.execute();
                    }
                    e.consume();
                    break;
                case KeyEvent.VK_LEFT:
                case KeyEvent.VK_ESCAPE:
                    break;
            }
        }

        void addItem(T newitem) {
            items.add(newitem);
            if (current == items.getLast()) current = items.getFirst();
        }

        void addItemList(String s) {
            BufferedReader reader = new BufferedReader(new StringReader(s));
            T elem;
            try {
                while ((elem = (T) reader.readLine()) != null) {
                    addItem(elem);
                }
            } catch (IOException e) {
                throw new StellariumException("Error while reading item list:" + s, e);
            }
        }

        void replaceItemList(String s, int selection) {
            items.clear();
            addItemList(s);
            for (Object current : items) {
            }
        }

        T getCurrent() {
            if (current == items.getLast()) return emptyT;
            else return current;
        }

        void setCurrent(T i) {
            int index = items.indexOf(i);

            // if not found, set to first item!
            if (index < 0) {
                current = items.getFirst();
                if (onChangeCallback != null) {
                    onChangeCallback.execute();
                }
            } else {
                current = i;
            }
        }

        boolean setValue(T i) {
            int index = items.indexOf(i);
            if (index < 0) {
                return false;
            } else {
                current = i;
            }
            return true;
        }

        String getLabel() {
            return label;
        }

        void set_OnTriggerCallback(Callback c) {
            onTriggerCallback = c;
        }

        protected
        T emptyT;

        LinkedList<T> items;

        T current;

        String label;

        Callback onTriggerCallback;
    }

    /**
     * Widget used to set time zone. Initialized from a file of Type /usr/share/zoneinfo/zone.tab
     */
    static class TimeZoneItem extends CallbackComponent {
        /**
         * Builds a new TimeZoneItem
         *
         * @param zonetabFile The file name
         * @param someLabel   The item's label
         * @throws StellariumException If the file name is empty, or if the file could not be read
         */
        TimeZoneItem(URL zonetabFile, String someLabel) throws StellariumException {
            this.label = someLabel;
            try {
                BufferedReader is = new BufferedReader(new InputStreamReader(zonetabFile.openStream()));
                try {
                    String tzname;
                    String zoneline;
                    int i;

                    while ((zoneline = is.readLine()) != null) {
                        if (zoneline.charAt(0) == '#') continue;
                        tzname = zoneline;
                        i = tzname.indexOf("/");
                        String s = tzname.substring(0, i);
                        String newitem = tzname.substring(i + 1, tzname.length());
                        MultiSetItem multiSetItem = continents.get(s);
                        if (multiSetItem == null) {
                            MultiSetItem value = new MultiSetItem(s);
                            continents.put(s, value);
                            value.addItem(newitem);
                            continentsNames.addItem(s);
                        } else {
                            multiSetItem.addItem(newitem);
                        }
                    }
                } finally {
                    is.close();
                }
                currentEdit = continentsNames;
            } catch (IOException e) {
                throw new StellariumException("Could not create TimeZoneItem due to I/O error: ", e);
            }
        }

        public void keyPressed(KeyEvent e) {
            currentEdit.keyPressed(e);
            if (e.isConsumed()) {
                if (onChangeCallback != null) {
                    onChangeCallback.execute();
                }
            } else {
                int k = e.getKeyCode();
                switch (k) {
                    case KeyEvent.VK_ESCAPE:
                        break;
                    case KeyEvent.VK_RIGHT:
                        if (currentEdit == continentsNames) currentEdit = continents.get(continentsNames.getCurrent());
                        else currentEdit = continentsNames;
                        e.consume();
                        break;
                    case KeyEvent.VK_LEFT:
                        if (currentEdit == continentsNames) return;
                        else currentEdit = continentsNames;
                        e.consume();
                }
            }
        }

        public String getString() {
            StringBuffer s1 = new StringBuffer(), s2 = new StringBuffer();
            if (currentEdit == continentsNames && active) {
                s1.append(START_ACTIVE);
                s2.append(STOP_ACTIVE);
            } else if (currentEdit != continentsNames && active) {
                s1.append(START_ACTIVE);
                s2.append(STOP_ACTIVE);
            }

            return label + s1.charAt(0) + continentsNames.getCurrent() + s2.charAt(0) + "/" + s1.charAt(1) +
                    continents.get(continentsNames.getCurrent()).getCurrent() + s2.charAt(1);
        }

        String gettz() {// should be const but gives a boring error...
            Object current = continentsNames.getCurrent();
            String s = current + "/";
            MultiSetItem multiSetItem = continents.get(current);
            if (multiSetItem != null) {
                s += multiSetItem.getCurrent();
            } else {
                s += "error";
            }
            return s;
        }

        void settz(String tz) {
            int i = tz.indexOf("/");
            continentsNames.setCurrent(tz.substring(0, i));
            continents.get(continentsNames.getCurrent()).setCurrent(tz.substring(i + 1, tz.length()));
        }

        boolean isEditable() {
            return true;
        }

        protected MultiSetItem continentsNames;

        protected Map<String, MultiSetItem> continents;

        protected String label;

        protected MultiSetItem currentEdit;
    }

    /**
     * List item widget with separation between UI keys (will be translated) and code value (never translated).
     * Assumes one-to-one mapping of keys to values
     * The callback function is called when the selected item changes
     */
    public static class MultiSet2Item<T> extends CallbackComponent {
        public MultiSet2Item(String _label) {
            super();
            label = _label;
            current = items.size() - 1;
        }

        MultiSet2Item(MultiSet2Item m) {
            super();
            label = m.label;
            setCurrent((T) m.getCurrent());
        }

        public String getString() {
            if (current == items.size() - 1) {
                return label;
            }
            return label + (active ? START_ACTIVE : "") + current + (active ? STOP_ACTIVE : "");
        }

        boolean isEditable() {
            return true;
        }

        public void keyPressed(KeyEvent e) {
            if (current == items.size() - 1) {
                return;
            }
            int k = e.getKeyCode();
            switch (k) {
                case KeyEvent.VK_ENTER:
                    if (onTriggerCallback != null) {
                        onTriggerCallback.execute();
                    }
                    break;

                case KeyEvent.VK_UP:
                    if (current != 0) {
                        --current;
                    } else {
                        current = items.size() - 2;
                    }
                    if (onChangeCallback != null) {
                        onChangeCallback.execute();
                    }
                    e.consume();
                    break;

                case KeyEvent.VK_DOWN:
                    if (current != items.size() - 2) {
                        ++current;
                    } else {
                        current = 0;
                    }
                    if (onChangeCallback != null) {
                        onChangeCallback.execute();
                    }
                    e.consume();
            }
        }

        void addItem(T newkey, T newvalue) {
            items.add(newkey);
            value.put(newkey, newvalue);
            if (current == items.size() - 1) {
                current = 0;
            }
        }

        void addItemList(String s) {// newline delimited, key and value alternate
            BufferedReader reader = new BufferedReader(new StringReader(s));
            T key, value;
            try {
                while ((key = (T) reader.readLine()) != null && (value = (T) reader.readLine()) != null) {
                    addItem(key, value);
                }
            } catch (IOException e) {
                throw new StellariumException("Error while reading list of key/value items: " + s, e);
            }
        }

        void replaceItemList(String s, int selection) {
            items.clear();
            value.clear();
            addItemList(s);
            current = selection;
        }

        T getCurrent() {
            if (current == items.size() - 1) {
                return emptyT;
            } else {
                return value.get(items.get(current));
            }
        }

        void setCurrent(T i) {// set by value, not key
            boolean found = false;
            current = 0;
            for (T iter : value.values()) {
                if (i == iter) {
                    found = true;
                    break;
                }
                current++;
            }

            if (!found) {
                current = 0;
            }
            if (onChangeCallback != null) {
                onChangeCallback.execute();
            }
        }

        boolean setValue(T i) {
            boolean found = false;
            current = 0;
            for (T iter : items) {
                if (i == value.get(iter)) {
                    found = true;
                    break;
                }
                current++;
            }

            return found;
        }

        String getLabel() {
            return label;
        }

        void set_OnTriggerCallback(STUI.Callback c) {
            onTriggerCallback = c;
        }

        void setLabel(String _label) {
            label = _label;
        }

        protected T emptyT;

        LinkedList<T> items;

        int current;

        String label;

        STUI.Callback onTriggerCallback;

        Map<T, T> value;// hash of key, value pairs
    }

    /**
     * Widget which simply launch the Callback when the user press enter
     */
    static class ActionItem extends CallbackComponent {
        boolean isEditable() {
            return true;
        }

        public ActionItem() {
            this("");
        }

        public ActionItem(String label) {
            this.label = label;
        }

        protected String label;

        protected String stringPrompt1;

        protected String stringPrompt2;

        protected long tempo;

        public String getString() {
            if (System.currentTimeMillis() - tempo > 1000) {
                if (active) {
                    return label + START_ACTIVE + stringPrompt1 + STOP_ACTIVE;
                } else return label + stringPrompt1;
            } else {
                if (active) {
                    return label + START_ACTIVE + stringPrompt2 + STOP_ACTIVE;
                } else return label + stringPrompt2;
            }
        }

        public void keyPressed(KeyEvent e) {
            int k = e.getKeyCode();
            if (k == KeyEvent.VK_ENTER) {
                // Call the Callback if enter is pressed
                if (onChangeCallback != null) {
                    onChangeCallback.execute();
                }
                tempo = System.currentTimeMillis();
                e.consume();
            }
        }
    }

    /**
     * Same as before but ask for a confirmation
     */
    static class ActionConfirmItem extends ActionItem {
        protected boolean isConfirming;

        protected String stringConfirm;

        public ActionConfirmItem() {
            this("");
        }

        public ActionConfirmItem(String stringConfirm) {
            super(stringConfirm);
        }

        public String getString() {
            if (active) {
                if (isConfirming) {
                    return label + START_ACTIVE + stringConfirm + STOP_ACTIVE;
                } else {
                    return label + START_ACTIVE + stringPrompt1 + STOP_ACTIVE;
                }
            } else return label + stringPrompt1;
        }

        public void keyPressed(KeyEvent e) {
            super.keyPressed(e);
            int k = e.getKeyCode();
            switch (k) {
                case KeyEvent.VK_ENTER:
                    if (isConfirming) {
                        // Call the Callback if enter is pressed
                        if (onChangeCallback != null) {
                            onChangeCallback.execute();
                        }
                        isConfirming = false;
                        e.consume();
                    } else {
                        isConfirming = true;
                        e.consume();
                    }
                    break;
                case KeyEvent.VK_ESCAPE:
                case KeyEvent.VK_LEFT:
                    if (isConfirming) {
                        isConfirming = false;
                        e.consume();
                    }
                    break;
                default:
                    e.consume();
            }
        }
    }

    static class VectorItem extends CallbackComponent {
        public VectorItem(String _label, Vector3d _init_vector) {
            label = _label;
            a = new DecimalItem(0, 1, 0, "", 0.05);
            b = new DecimalItem(0, 1, 0, "", 0.05);
            c = new DecimalItem(0, 1, 0, "", 0.05);
            currentEdit = a;
            setVector(_init_vector);
        }

        public void keyPressed(KeyEvent e) {
            currentEdit.keyPressed(e);
            if (e.isConsumed()) {
                if (onChangeCallback != null) {
                    onChangeCallback.execute();
                }
            } else {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_ESCAPE:
                        break;

                    case KeyEvent.VK_RIGHT:
                        if (currentEdit == a) currentEdit = b;
                        else if (currentEdit == b) currentEdit = c;
                        else if (currentEdit == c) currentEdit = a;
                        e.consume();
                        break;

                    case KeyEvent.VK_LEFT:
                        if (currentEdit == a) currentEdit = c;
                        else if (currentEdit == c) currentEdit = b;
                        else if (currentEdit == b) currentEdit = a;
                        e.consume();
                }
            }
        }

        public String getString() {
            String[] s1 = new String[3];
            String[] s2 = new String[3];
            if (currentEdit == a && active) {
                s1[0] = START_ACTIVE + "";
                s2[0] = STOP_ACTIVE + "";
            }
            if (currentEdit == b && active) {
                s1[1] = START_ACTIVE + "";
                s2[1] = STOP_ACTIVE + "";
            }
            if (currentEdit == c && active) {
                s1[2] = START_ACTIVE + "";
                s2[2] = STOP_ACTIVE + "";
            }

            return label +
                    s1[0] + a.getString() + s2[0] + " " +
                    s1[1] + b.getString() + s2[1] + " " +
                    s1[2] + c.getString() + s2[2] + " ";
        }

        boolean isEditable() {
            return true;
        }

        Vector3d getVector() {
            return new Vector3d(a.getValue(), b.getValue(), c.getValue());
        }

        Color getColor() {
            return new Color((float) a.getValue(), (float) b.getValue(), (float) c.getValue());
        }

        void setVector(Vector3d _vector) {
            a.setValue(_vector.x);
            b.setValue(_vector.y);
            c.setValue(_vector.z);
        }

        void setVector(Color _vector) {
            a.setValue(_vector.getRed());
            b.setValue(_vector.getGreen());
            c.setValue(_vector.getBlue());
        }

        void setLabel(String _label) {
            label = _label;
        }

        /**
         * 0 to 2
         */
        protected DecimalItem currentEdit;

        String label;

        DecimalItem a, b, c;
    }
}