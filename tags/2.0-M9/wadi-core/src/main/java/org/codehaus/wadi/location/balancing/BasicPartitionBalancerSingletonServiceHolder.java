/**
 * Copyright 2006 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.codehaus.wadi.location.balancing;

import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.servicespace.SingletonServiceHolder;


/**
 * 
 * @version $Revision: 1538 $
 */
public class BasicPartitionBalancerSingletonServiceHolder implements PartitionBalancerSingletonServiceHolder {
    private final SingletonServiceHolder delegate;
    private final PartitionBalancerSingletonService balancerSingletonService;
    
    public BasicPartitionBalancerSingletonServiceHolder(SingletonServiceHolder delegate) {
        if (null == delegate) {
            throw new IllegalArgumentException("delegate is required");
        }
        this.delegate = delegate;
        
        Object service = delegate.getService();
        if (!(service instanceof PartitionBalancerSingletonService)) {
            throw new IllegalArgumentException("Singleton service [" + service + 
                    "] is not a [" + PartitionBalancerSingletonService.class + "]");
        }
        balancerSingletonService = (PartitionBalancerSingletonService) service;
    }

    public Peer getHostingPeer() {
        return delegate.getHostingPeer();
    }

    public boolean isLocal() {
        return delegate.isLocal();
    }

    public void start() throws Exception {
        delegate.start();
    }

    public void stop() throws Exception {
        delegate.stop();
    }
    
    public PartitionBalancerSingletonService getPartitionBalancerSingletonService() {
        return balancerSingletonService;
    }

    public Object getService() {
        return delegate.getService();
    }

    public boolean isStarted() {
        return delegate.isStarted();
    }

}