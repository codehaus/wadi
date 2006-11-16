package org.codehaus.wadi.tribes;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * <p>Title: </p>
 *
 * <p>Description: </p>
 *
 * <p>Copyright: Copyright (c) 2006</p>
 *
 * <p>Company: </p>
 *
 * @author not attributable
 * @version 1.0
 */

public class TestWadiTribesSuite extends TestCase {

    public TestWadiTribesSuite(String s) {
        super(s);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTestSuite(org.codehaus.wadi.tribes.TestTribesEvacuation.class);
        suite.addTestSuite(org.codehaus.wadi.tribes.TestTribesRelocation.class);
        suite.addTestSuite(org.codehaus.wadi.tribes.TestTribesGroup.class);
        return suite;
    }
}
