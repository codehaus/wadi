package org.codehaus.wadi.location.balancing;

import org.codehaus.wadi.core.Lifecycle;
import org.codehaus.wadi.servicespace.ServiceName;
import org.codehaus.wadi.servicespace.SingletonService;

public interface PartitionBalancerSingletonService extends Lifecycle, Runnable, SingletonService {
    ServiceName NAME = new ServiceName("PartitionBalancerSingletonService");

    void queueRebalancing();
}