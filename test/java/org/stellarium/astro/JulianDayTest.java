package org.stellarium.astro;

import junit.framework.TestCase;

public class JulianDayTest extends TestCase {

    private boolean compare(int a, int b, String what) {
        if (a != b) {
            System.err.println(what + ": " + a + " and " + b + "; delta = " + (b - a));
            return false;
        }
        return true;
    }

    private boolean compare(double a, double b, String what) {
        if (Math.abs(a - b) > 1e-4) {
            System.err.println(what + ": " + a + " and " + b + "; delta = " + (b - a));
            return false;
        }
        return true;
    }

    /**
     * Compares things that were supposed to be identical!
     */
    private void compare(JulianDay x, JulianDay o, String what) {
        boolean r = true;
        r &= compare(x.year(), o.year(), "year");
        r &= compare(x.month(), o.month(), "month");
        r &= compare(x.d(), o.d(), "day (as fraction)");
        r &= compare(x.dayOfMonth(), o.dayOfMonth(), "day");
        r &= compare(x.hour(), o.hour(), "hour");
        r &= compare(x.minute(), o.minute(), "minute");
        r &= compare(x.second(), o.second(), "second");
        r &= compare(x.julianDay(), o.julianDay(), "Julian Day");
        assertTrue(what + " jd1=" + x + ", jd2=" + o, r);
    }

    /**
     * Tests all of the Meeus examples and then does a
     * Monte-Carlo.
     */
    public void test1() throws Exception {

        // from the book....

        // Sputnik 1
        compare(new JulianDay(1957, 10, 4.81), new JulianDay(2436116.31), "Sputnik 1");

        // 333
        compare(new JulianDay(333, 1, 27, 12, 0, 0), new JulianDay(1842713.0), "333");

        // as well as....
        compare(new JulianDay(2000, 1, 1.5), new JulianDay(2451545.0), "Meeus example 1");
        compare(new JulianDay(1999, 1, 1.0), new JulianDay(2451179.5), "Meeus example 2");
        compare(new JulianDay(1987, 1, 27.0), new JulianDay(2446822.5), "Meeus example 3");
        compare(new JulianDay(1987, 6, 19.5), new JulianDay(2446966.0), "Meeus example 4");
        compare(new JulianDay(1988, 1, 27.0), new JulianDay(2447187.5), "Meeus example 5");
        compare(new JulianDay(1988, 6, 19.5), new JulianDay(2447332.0), "Meeus example 6");
        compare(new JulianDay(1900, 1, 1.0), new JulianDay(2415020.5), "Meeus example 7");
        compare(new JulianDay(1600, 1, 1.0), new JulianDay(2305447.5), "Meeus example 8");
        compare(new JulianDay(1600, 12, 31.0), new JulianDay(2305812.5), "Meeus example 9");
        compare(new JulianDay(837, 4, 10.3), new JulianDay(2026871.8), "Meeus example 10");
        compare(new JulianDay(-123, 12, 31.0), new JulianDay(1676496.5), "Meeus example 11");
        compare(new JulianDay(-122, 1, 1.0), new JulianDay(1676497.5), "Meeus example 12");
        compare(new JulianDay(-1000, 7, 12.5), new JulianDay(1356001.0), "Meeus example 13");
        compare(new JulianDay(-1000, 2, 29.0), new JulianDay(1355866.5), "Meeus example 14");
        compare(new JulianDay(-1001, 8, 17.9), new JulianDay(1355671.4), "Meeus example 15");
        compare(new JulianDay(-4712, 1, 1.5), new JulianDay(0.0), "Meeus example 16");

        // and then random conversions back-and-forth....
        /*Random r = new Random();
        for (int i=0; i<5000000; i++) {
            double f = r.nextFloat() * 3e6;
            JulianDay d1 = new JulianDay(f);
            int y = d1.year();
            int mo = d1.month();
            double d = d1.d();
            JulianDay d2 = new JulianDay(y, mo, d);
            int day = d1.dayOfMonth();
            int h = d1.hour();
            int m = d1.minute();
            int s = d1.second();
            JulianDay d3 = new JulianDay(y, mo, day, h, m, s);
            d1.compare(d2, "d1 with d2");
            d1.compare(d3, "d1 with d3");
        }*/
    }
}