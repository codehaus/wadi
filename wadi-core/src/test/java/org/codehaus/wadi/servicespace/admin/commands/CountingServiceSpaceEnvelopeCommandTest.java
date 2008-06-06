/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.codehaus.wadi.servicespace.admin.commands;

import java.net.URI;
import java.util.Collections;

import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.servicespace.ServiceSpace;
import org.codehaus.wadi.servicespace.ServiceSpaceName;
import org.codehaus.wadi.servicespace.basic.ServiceSpaceRegistry;

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 *
 * @version $Rev:$ $Date:$
 */
public class CountingServiceSpaceEnvelopeCommandTest extends RMockTestCase {

    private Dispatcher serviceSpaceDispatcher;
    private ServiceSpaceRegistry registry;
    private ServiceSpaceName name;

    @Override
    protected void setUp() throws Exception {
        name = new ServiceSpaceName(new URI("name"));
        ServiceSpace serviceSpace = (ServiceSpace) mock(ServiceSpace.class);
        serviceSpace.getServiceSpaceName();
        modify().returnValue(name);
        
        serviceSpaceDispatcher = serviceSpace.getDispatcher();
        modify().multiplicity(expect.from(0));
        
        registry = (ServiceSpaceRegistry) mock(ServiceSpaceRegistry.class);
        registry.getServiceSpaces();
        modify().returnValue(Collections.singleton(serviceSpace));
    }
    
    public void testGetDispatcherReturnsNullWhenNoMatchingServiceSpace() throws Exception {
        startVerification();

        CountingServiceSpaceEnvelopeCommand command = new CountingServiceSpaceEnvelopeCommand(
            new ServiceSpaceName(new URI("name2")));
        assertNull(command.getDispatcher(null, null, registry));
    }
    
    public void testGetDispatcherReturnsMatchingServiceSpaceDispatcher() throws Exception {
        startVerification();
        
        CountingServiceSpaceEnvelopeCommand command = new CountingServiceSpaceEnvelopeCommand(name);
        assertSame(serviceSpaceDispatcher, command.getDispatcher(null, null, registry));
    }
    
}
