package org.codehaus.wadi.location.balancing;

import org.codehaus.wadi.core.Lifecycle;
import org.codehaus.wadi.servicespace.ServiceName;

public interface PartitionBalancerSingletonService extends Lifecycle, Runnable {
    ServiceName NAME = new ServiceName("PartitionBalancerSingletonService");

    void queueRebalancing();
}