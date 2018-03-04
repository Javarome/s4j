package org.stellarium;

import junit.framework.TestCase;

public class UtilityTest extends TestCase {
    public void testDecAngle() throws Exception {
        assertEquals(48.6, StelUtility.getDecAngle("+48d36'0.00\""));
    }
}
