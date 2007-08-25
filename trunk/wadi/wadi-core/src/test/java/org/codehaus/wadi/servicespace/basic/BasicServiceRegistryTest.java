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

import java.io.Serializable;

import org.codehaus.wadi.core.Lifecycle;
import org.codehaus.wadi.servicespace.ServiceAlreadyRegisteredException;
import org.codehaus.wadi.servicespace.ServiceName;

import com.agical.rmock.core.match.Expression;


/**
 * 
 * @version $Revision: $
 */
public class BasicServiceRegistryTest extends AbstractServiceSpaceTestCase {

    private ServiceName serviceName1;
    private ServiceName serviceName2;
    private Lifecycle service1;
    private Lifecycle service2;

    protected void setUp() throws Exception {
        super.setUp();
        serviceName1 = new ServiceName("name1");
        serviceName2 = new ServiceName("name2");
        service1 = (Lifecycle) mock(Lifecycle.class);
        service2 = (Lifecycle) mock(Lifecycle.class);
        
        dispatcher.send(null, (Serializable) null);
        modify().args(new Expression[] {is.ANYTHING, is.ANYTHING});
        modify().multiplicity(expect.from(0));
    }

    public void testCannotRegisterTwiceTheSameName() throws Exception {
        startVerification();

        BasicServiceRegistry registry = new BasicServiceRegistry(serviceSpace);
        registry.register(serviceName1, service1);
        try {
            registry.register(serviceName1, service2);
            fail();
        } catch (ServiceAlreadyRegisteredException e) {
        }
    }

    public void testCannotRegisterAfterStart() throws Exception {
        recordStartPhase();
        
        startVerification();
        
        BasicServiceRegistry registry = new BasicServiceRegistry(serviceSpace);
        registry.register(serviceName1, service1);
        registry.register(serviceName2, service2);
        
        registry.start();

        try {
            registry.register(new ServiceName("name3"), service2);
            fail();
        } catch (IllegalStateException e) {
        }
    }

    public void testCannotUnregisterAfterStart() throws Exception {
        recordStartPhase();
        
        startVerification();
        
        BasicServiceRegistry registry = new BasicServiceRegistry(serviceSpace);
        registry.register(serviceName1, service1);
        registry.register(serviceName2, service2);
        
        registry.start();

        try {
            registry.unregister(serviceName1);
            fail();
        } catch (IllegalStateException e) {
        }
    }

    public void testSuccessfulStart() throws Exception {
        recordStartPhase();
        
        startVerification();
        
        BasicServiceRegistry registry = new BasicServiceRegistry(serviceSpace);
        registry.register(serviceName1, service1);
        registry.register(serviceName2, service2);
        
        registry.start();
    }

    public void testFailureUponStart() throws Exception {
        beginSection(s.ordered("EndPoint is registered - service1 starts, service2 fails, service1 stops"));
        dispatcher.register(null);
        modify().args(is.NOT_NULL);
        
        service1.start();
        
        service2.start();
        Exception expectedException = new Exception();
        modify().throwException(expectedException);
        
        service1.stop();
        endSection();
        
        startVerification();
        
        BasicServiceRegistry registry = new BasicServiceRegistry(serviceSpace);
        registry.register(serviceName1, service1);
        registry.register(serviceName2, service2);

        try {
            registry.start();
            fail();
        } catch (Exception e) {
            assertSame(expectedException, e);
        }
    }

    public void testSuccessfulStop() throws Exception {
        recordStartPhase();
        
        beginSection(s.ordered("EndPoint is deregistered - service2 stops - service1 stops"));
        dispatcher.unregister(null, 0, 0);
        modify().args(new Expression[] {is.NOT_NULL, is.ANYTHING, is.ANYTHING});
        service2.stop();
        service1.stop();
        endSection();
        
        startVerification();
        
        BasicServiceRegistry registry = new BasicServiceRegistry(serviceSpace);
        registry.register(serviceName1, service1);
        registry.register(serviceName2, service2);
        
        registry.start();
        registry.stop();
    }

    public void testFailureUponStop() throws Exception {
        recordStartPhase();
        
        beginSection(s.ordered("EndPoint is deregistered - service2 fails - service1 stops"));
        dispatcher.unregister(null, 0, 0);
        modify().args(new Expression[] {is.NOT_NULL, is.ANYTHING, is.ANYTHING});
        service2.stop();
        modify().throwException(new Exception());
        service1.stop();
        endSection();

        startVerification();
        
        BasicServiceRegistry registry = new BasicServiceRegistry(serviceSpace);
        registry.register(serviceName1, service1);
        registry.register(serviceName2, service2);
        
        registry.start();
        registry.stop();
    }

    private void recordStartPhase() throws Exception {
        beginSection(s.ordered("EndPoint is registered - service1 starts - service2 starts"));
        dispatcher.register(null);
        modify().args(is.NOT_NULL);
        service1.start();
        service2.start();
        endSection();
    }
    
}
