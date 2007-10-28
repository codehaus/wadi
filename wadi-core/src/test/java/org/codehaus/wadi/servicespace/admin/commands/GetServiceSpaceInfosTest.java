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
package org.codehaus.wadi.servicespace.admin.commands;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.servicespace.ServiceSpace;
import org.codehaus.wadi.servicespace.ServiceSpaceInfo;
import org.codehaus.wadi.servicespace.ServiceSpaceName;
import org.codehaus.wadi.servicespace.basic.ServiceSpaceRegistry;

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 * 
 * @version $Revision: 1538 $
 */
public class GetServiceSpaceInfosTest extends RMockTestCase {

    public void testAggregation() throws Exception {
        LocalPeer localPeer = (LocalPeer) mock(LocalPeer.class);
        ServiceSpaceRegistry serviceSpaceRegistry = (ServiceSpaceRegistry) mock(ServiceSpaceRegistry.class);
        
        Set serviceSpaces = new HashSet(); 
        serviceSpaceRegistry.getServiceSpaces();
        modify().returnValue(serviceSpaces);

        ServiceSpace serviceSpace1 = (ServiceSpace) mock(ServiceSpace.class);
        serviceSpace1.getServiceSpaceName();
        ServiceSpaceName name1 = new ServiceSpaceName(new URI("space1"));
        modify().returnValue(name1);
        serviceSpaces.add(serviceSpace1);
        
        ServiceSpace serviceSpace2 = (ServiceSpace) mock(ServiceSpace.class);
        serviceSpace2.getServiceSpaceName();
        ServiceSpaceName name2 = new ServiceSpaceName(new URI("space2"));
        modify().returnValue(name2);
        serviceSpaces.add(serviceSpace2);
        
        startVerification();
        
        GetServiceSpaceInfos command = new GetServiceSpaceInfos();
        Set actualServiceSpaceNames = (Set) command.execute(localPeer, serviceSpaceRegistry);
        
        Set expectedServiceSpaceInfos = new HashSet();
        expectedServiceSpaceInfos.add(new ServiceSpaceInfo(localPeer, name1));
        expectedServiceSpaceInfos.add(new ServiceSpaceInfo(localPeer, name2));

        assertEquals(expectedServiceSpaceInfos, actualServiceSpaceNames);
    }
    
}
