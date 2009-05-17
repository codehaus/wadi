package org.codehaus.wadi.test;

import junit.framework.Assert;

import org.codehaus.wadi.group.Dispatcher;

public class TestUtil {

    public static void waitForDispatcherSeeOthers(Dispatcher dispatchers[], long timeout) {
        long start = System.currentTimeMillis();
        long end = start + timeout;
        for (int i = 0; i < dispatchers.length; i++) {
            Dispatcher dispatcher = dispatchers[i];
            while (dispatcher.getCluster().getPeerCount() != dispatchers.length) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Assert.fail("Failure due to thread interruption");
                }
                if (end < System.currentTimeMillis()) {
                    Assert.fail("Failure while waiting for [" + dispatchers.length + "] peers");
                }
            }
        }
    }
}
