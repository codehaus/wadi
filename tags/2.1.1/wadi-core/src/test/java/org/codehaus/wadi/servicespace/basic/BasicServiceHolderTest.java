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
package org.codehaus.wadi.servicespace.basic;

import org.codehaus.wadi.core.Lifecycle;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.servicespace.LifecycleState;
import org.codehaus.wadi.servicespace.ServiceLifecycleEvent;
import org.codehaus.wadi.servicespace.ServiceName;

/**
 * 
 * @version $Revision: 1538 $
 */
public class BasicServiceHolderTest extends AbstractServiceSpaceTestCase {

    private ServiceName serviceName;
    private Lifecycle service;

    protected void setUp() throws Exception {
        super.setUp();
        service = (Lifecycle) mock(Lifecycle.class);
        serviceName = new ServiceName("name");
    }
    
    public void testSuccessfulStart() throws Exception {
        beginSection(s.ordered("STARTING Events - Start Service - STARTED Events"));
        recordSendToCluster(LifecycleState.STARTING);
        service.start();
        recordSendToCluster(LifecycleState.STARTED);
        endSection();
        
        startVerification();
        
        BasicServiceHolder serviceHolder = new BasicServiceHolder(serviceSpace, serviceName, service);
        serviceHolder.start();
    }

    public void testServiceThrowsExceptionDuringStart() throws Exception {
        Exception expectedException = new Exception();
        beginSection(s.ordered("STARTING Events - Start Service - FAILED Events"));
        recordSendToCluster(LifecycleState.STARTING);
        service.start();
        modify().throwException(expectedException);
        recordSendToCluster(LifecycleState.FAILED);
        endSection();
        
        startVerification();

        BasicServiceHolder serviceHolder = new BasicServiceHolder(serviceSpace, serviceName, service);
        try {
            serviceHolder.start();
            fail();
        } catch (Exception e) {
            assertSame(expectedException, e);
        }
    }

    public void testSuccessfulStop() throws Exception {
        beginSection(s.ordered("STOPPING Events - Stop Service - STOPPED Events"));
        recordSendToCluster(LifecycleState.STOPPING);
        service.stop();
        recordSendToCluster(LifecycleState.STOPPED);
        endSection();
        
        startVerification();
        
        BasicServiceHolder serviceHolder = new BasicServiceHolder(serviceSpace, serviceName, service);
        serviceHolder.stop();
    }

    public void testServiceThrowsExceptionDuringStop() throws Exception {
        Exception expectedException = new Exception();
        beginSection(s.ordered("STOPPING Events - Stop Service - FAILED Events"));
        recordSendToCluster(LifecycleState.STOPPING);
        service.stop();
        modify().throwException(expectedException);
        recordSendToCluster(LifecycleState.FAILED);
        endSection();
        
        startVerification();

        BasicServiceHolder serviceHolder = new BasicServiceHolder(serviceSpace, serviceName, service);
        try {
            serviceHolder.stop();
            fail();
        } catch (Exception e) {
            assertSame(expectedException, e);
        }
    }

    private void recordSendToCluster(LifecycleState state) throws MessageExchangeException {
        beginSection(s.unordered(state + " Events"));
        dispatcher.send(clusterAddress, newLifecycleEvent(state));
        endSection();
    }
    
    private ServiceLifecycleEvent newLifecycleEvent(LifecycleState state) {
        return new ServiceLifecycleEvent(serviceSpaceName, serviceName, localPeer, state);
    }
    
}
