/**
 * Copyright 2007 The Apache Software Foundation
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

import java.net.URI;
import java.util.Set;

import org.codehaus.wadi.servicespace.ServiceSpace;
import org.codehaus.wadi.servicespace.ServiceSpaceAlreadyRegisteredException;
import org.codehaus.wadi.servicespace.ServiceSpaceName;
import org.codehaus.wadi.servicespace.ServiceSpaceNotFoundException;

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 * 
 * @version $Revision: 1538 $
 */
public class BasicServiceSpaceRegistryTest extends RMockTestCase {

    private ServiceSpace serviceSpace1;
    private ServiceSpaceName serviceSpaceName1;
    private ServiceSpace serviceSpace2;
    private ServiceSpaceName serviceSpaceName2;

    protected void setUp() throws Exception {
        serviceSpace1 = (ServiceSpace) mock(ServiceSpace.class);
        serviceSpaceName1 = new ServiceSpaceName(new URI("space1"));
        
        serviceSpace2 = (ServiceSpace) mock(ServiceSpace.class);
        serviceSpaceName2 = new ServiceSpaceName(new URI("space2"));
        
        serviceSpace1.getServiceSpaceName();
        modify().multiplicity(expect.atLeast(0)).returnValue(serviceSpaceName1);

        serviceSpace2.getServiceSpaceName();
        modify().multiplicity(expect.atLeast(0)).returnValue(serviceSpaceName2);
    }
    
    public void testRegistration() throws Exception {
        startVerification();
        
        BasicServiceSpaceRegistry serviceSpaceRegistry = new BasicServiceSpaceRegistry();
        serviceSpaceRegistry.register(serviceSpace1);
        serviceSpaceRegistry.register(serviceSpace2);
        
        Set serviceSpaces = serviceSpaceRegistry.getServiceSpaces();
        assertEquals(2, serviceSpaces.size());
        assertTrue(serviceSpaces.contains(serviceSpace1));
        assertTrue(serviceSpaces.contains(serviceSpace2));
    }
    
    public void testExceptionIfDoubleRegistration() throws Exception {
        startVerification();
        
        BasicServiceSpaceRegistry serviceSpaceRegistry = new BasicServiceSpaceRegistry();
        serviceSpaceRegistry.register(serviceSpace1);
        try {
            serviceSpaceRegistry.register(serviceSpace1);
            fail();
        } catch (ServiceSpaceAlreadyRegisteredException e) {
        }
    }
    
    public void testUnregistrion() throws Exception {
        startVerification();
        
        BasicServiceSpaceRegistry serviceSpaceRegistry = new BasicServiceSpaceRegistry();
        serviceSpaceRegistry.register(serviceSpace1);
        serviceSpaceRegistry.register(serviceSpace2);
        serviceSpaceRegistry.unregister(serviceSpace2);
        
        Set serviceSpaces = serviceSpaceRegistry.getServiceSpaces();
        assertEquals(1, serviceSpaces.size());
        assertTrue(serviceSpaces.contains(serviceSpace1));
    }
    
    public void testExceptionIfUnregistrationOfUnknownServiceSpace() throws Exception {
        startVerification();
        
        BasicServiceSpaceRegistry serviceSpaceRegistry = new BasicServiceSpaceRegistry();
        try {
            serviceSpaceRegistry.unregister(serviceSpace1);
            fail();
        } catch (ServiceSpaceNotFoundException e) {
        }
    }
    
}
