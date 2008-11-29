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

import java.util.Collections;

import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.servicespace.basic.ServiceSpaceRegistry;

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 *
 * @version $Rev:$ $Date:$
 */
public class AbstractCountingCommandTest extends RMockTestCase {

    private Dispatcher underlyingDispatcher;
    private Dispatcher dispatcher;
    private AbstractCountingCommand command;
    private LocalPeer localPeer;

    @Override
    protected void setUp() throws Exception {
        localPeer = (LocalPeer) mock(LocalPeer.class);
        underlyingDispatcher = (Dispatcher) mock(Dispatcher.class);
        dispatcher = (Dispatcher) mock(Dispatcher.class);
        
        command = new AbstractCountingCommand() {
            @Override
            protected Dispatcher getDispatcher(Dispatcher underlyingDispatcher,
                LocalPeer localPeer,
                ServiceSpaceRegistry registry) {
                return dispatcher;
            }
        };
    }
    
    public void testGetDispatherReturnsNullsReturnsNull() throws Exception {
        startVerification();
        
        command = new AbstractCountingCommand() {
            @Override
            protected Dispatcher getDispatcher(Dispatcher underlyingDispatcher,
                LocalPeer localPeer,
                ServiceSpaceRegistry registry) {
                return null;
            }
        };
        assertNull(command.execute(underlyingDispatcher, localPeer, null));
    }

    public void testInitializationReturnsZeroCounts() throws Exception {
        dispatcher.getInterceptors();
        modify().returnValue(Collections.emptyList());
        
        dispatcher.addInterceptor(null);
        modify().args(is.instanceOf(CountingEnvelopeInterceptor.class));
        
        startVerification();
        
        CountingEnvelopeInfo countInfo = (CountingEnvelopeInfo) command.execute(underlyingDispatcher, localPeer, null);
        assertNotNull(countInfo);
        assertEquals(localPeer, countInfo.getHostingPeer());
        assertEquals(0, countInfo.getInboundEnvelopeCpt());
        assertEquals(0, countInfo.getOutboundEnvelopeCpt());
    }
    
    public void testReturnInterceptorCounts() throws Exception {
        CountingEnvelopeInterceptor interceptor = new CountingEnvelopeInterceptor();
        interceptor.onInboundEnvelope(null);
        interceptor.onInboundEnvelope(null);
        interceptor.onOutboundEnvelope(null);
        
        dispatcher.getInterceptors();
        modify().returnValue(Collections.singletonList(interceptor));
        
        startVerification();
        
        CountingEnvelopeInfo countInfo = (CountingEnvelopeInfo) command.execute(underlyingDispatcher, localPeer, null);
        assertNotNull(countInfo);
        assertEquals(localPeer, countInfo.getHostingPeer());
        assertEquals(2, countInfo.getInboundEnvelopeCpt());
        assertEquals(1, countInfo.getOutboundEnvelopeCpt());
    }

}
