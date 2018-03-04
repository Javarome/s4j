/*
 * Copyright (C) 2006 Frederic Simon
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
package org.stellarium.data;

import junit.framework.TestCase;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;


public class HipparcosDataReaderTest extends TestCase {
    private ReverseDataInputStream dis;

    private static final String HIPPARCOS_DATA_FILENAME = "resources/data/hipparcos.fab";

    private File hipparcosFile;

    private static final int NB_ELEMENTS = 120417;

    private Map<Integer, HipData> hipTestDataMap;

    private static final HipData[] testHipData = new HipData[]{
            new HipData(2200, 0.46348888, 28.17389, 8.128906, 2, 675.2795),
            new HipData(4400, 0.9406056, 15.010389, 8.738281, 2, 1953.0538),
            new HipData(6600, 1.4128083, 56.200443, 8.457031, 5, 982.40967),
            new HipData(8800, 1.8855306, -61.68311, 8.636719, 5, 774.72687),
            new HipData(11000, 2.362289, -6.879667, 9.0859375, 5, 87.34869),
            new HipData(13200, 2.8314888, -37.067528, 8.808594, 5, 1707.6439),
            new HipData(15400, 3.3096027, -51.29697, 8.019531, 2, 453.63004),
            new HipData(17600, 3.7703223, -24.391027, 8.667969, 4, 478.94272),
            new HipData(19800, 4.2436166, -2.1398056, 7.796875, 5, 4181.5386),
            new HipData(22000, 4.7293973, -10.68225, 7.8164062, 2, 604.0),
            new HipData(24200, 5.195211, -58.985584, 9.199219, 3, 389.2124),
            new HipData(26400, 5.619714, -26.398695, 11.359375, 12, 599.55884),
            new HipData(28600, 6.0364223, 37.737526, 8.808594, 4, 10872.0),
            new HipData(30800, 6.4715056, -13.053083, 8.0078125, 1, 1156.5957),
            new HipData(33000, 6.874872, 49.935333, 8.996094, 3, 501.01382),
            new HipData(35200, 7.274422, 79.47914, 9.5078125, 6, 886.3043),
            new HipData(37400, 7.67925, 23.246529, 8.40625, 2, 1105.6271),
            new HipData(39600, 8.092503, -25.221611, 8.199219, 5, 630.8704),
            new HipData(41800, 8.522675, -65.52278, 7.46875, 2, 732.94385),
            new HipData(44000, 8.959686, 16.233778, 7.9375, 5, 1164.8572),
            new HipData(46200, 9.420073, -52.425472, 8.8984375, 1, 2346.4749),
            new HipData(48400, 9.867537, -69.79119, 11.308594, 12, 279.96567),
            new HipData(50600, 10.3368225, -12.208944, 9.2578125, 5, 2416.0),
            new HipData(52800, 10.794441, -15.243972, 6.9570312, 2, 0.0),
            new HipData(55000, 11.261072, -16.349777, 9.609375, 12, 601.77124),
            new HipData(57200, 11.72978, -29.679417, 8.40625, 2, 774.72687),
            new HipData(59400, 12.185553, 12.471027, 8.7265625, 3, 803.34973),
            new HipData(61600, 12.622822, 12.983611, 8.238281, 4, 2090.7693),
            new HipData(63800, 13.0764, 30.105944, 10.597656, 4, 658.9091),
            new HipData(66000, 13.531167, -6.0146666, 7.8789062, 5, 801.3759),
            new HipData(68200, 13.962627, 11.962361, 7.7460938, 2, 622.44275),
            new HipData(70400, 14.403164, 5.8201113, 5.1171875, 2, 151.28015),
            new HipData(72600, 14.843889, -32.466167, 7.4765625, 3, 147.65053),
            new HipData(74800, 15.287122, -7.8566666, 7.546875, 5, 508.03738),
            new HipData(77000, 15.722086, -21.120222, 9.019531, 4, 271.1222),
            new HipData(79200, 16.164827, -12.885722, 7.4882812, 5, 493.4342),
            new HipData(81400, 16.62498, -11.869778, 8.359375, 3, 414.96182),
            new HipData(83600, 17.087772, -20.191639, 8.2578125, 4, 534.68854),
            new HipData(85800, 17.532219, 32.67489, 8.2890625, 5, 5623.448),
            new HipData(88000, 17.975409, -36.449585, 9.058594, 1, 825.7215),
            new HipData(90200, 18.405067, -44.110195, 5.2578125, 1, 524.373),
            new HipData(92400, 18.829878, 20.844305, 11.308594, 12, 255.61128),
            new HipData(94600, 19.25154, 24.047695, 10.2265625, 1, 14825.454),
            new HipData(96800, 19.67742, 44.789055, 7.1992188, 5, 530.3415),
            new HipData(99000, 20.09904, 22.229334, 8.2578125, 5, 18120.0),
            new HipData(101200, 20.513027, -37.733612, 8.519531, 3, 1190.365),
            new HipData(103400, 20.948225, -0.07986111, 8.8359375, 2, 453.0),
            new HipData(105600, 21.38889, 72.12078, 8.3671875, 4, 526.0645),
            new HipData(107800, 21.83846, 65.889946, 9.0859375, 2, 2051.3208),
            new HipData(110000, 22.28001, -12.831445, 5.359375, 5, 483.9169),
            new HipData(112200, 22.724405, -24.380556, 8.707031, 4, 1101.8918),
            new HipData(114400, 23.168741, -33.909027, 8.097656, 6, 1638.995),
            new HipData(116600, 23.630302, 3.3703055, 7.3164062, 3, 223.7037)
    };

    protected void setUp() throws Exception {
        hipparcosFile = new File(HIPPARCOS_DATA_FILENAME);
        dis = new ReverseDataInputStream(new FileInputStream(hipparcosFile));
        hipTestDataMap = new HashMap<Integer, HipData>(testHipData.length);
        for (HipData hipData : testHipData) {
            hipTestDataMap.put(hipData.hp, hipData);
        }
    }

    protected void tearDown() throws Exception {
        if (dis != null)
            dis.close();
    }

    public void testAllFile() throws Exception {
        testNumberOfEntry();
        HipData hipData = new HipData();
        //for (int i = 0; i < NB_ELEMENTS; i++) {
        for (int i = 0; i < hipTestDataMap.size(); i++) {
            hipData.fillData(i, dis);
            if (hipTestDataMap.containsKey(i)) {
                assertEquals(hipTestDataMap.get(i), hipData);
            }
        }
    }

    public void testNumberOfEntry() throws Exception {
        //int size = readInt();
        int size = dis.readInt();
        assertEquals(NB_ELEMENTS, size);

        // Check from file size
        int sizeFromLength = (int) ((hipparcosFile.length() - 4) / 15);
        assertEquals(size, sizeFromLength);
    }
}
