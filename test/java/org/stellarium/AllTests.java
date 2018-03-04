package org.stellarium;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.stellarium.astro.ConstellationMgrTest;
import org.stellarium.astro.HipStarMgrTest;
import org.stellarium.astro.JulianDayTest;
import org.stellarium.astro.planet.TestVsop87;
import org.stellarium.data.HipparcosDataReaderTest;
import org.stellarium.i18n.I18NTest;

/**
 * @author <a href="mailto:rr0@rr0.org">Jerome Beau</href>
 * @since 1.0
 */
public class AllTests extends junit.framework.TestSuite {
    public static Test suite() {
        TestSuite suite = new TestSuite("All S4J tests");

        suite.addTestSuite(TestVsop87.class);

        suite.addTestSuite(ConstellationMgrTest.class);
        suite.addTestSuite(HipStarMgrTest.class);
        suite.addTestSuite(JulianDayTest.class);

        suite.addTestSuite(HipparcosDataReaderTest.class);

        suite.addTestSuite(I18NTest.class);

        suite.addTestSuite(CommandLineTest.class);
        //suite.addTestSuite(DateTimeTest.class);
        suite.addTestSuite(UtilityTest.class);

        return suite;
    }
}
