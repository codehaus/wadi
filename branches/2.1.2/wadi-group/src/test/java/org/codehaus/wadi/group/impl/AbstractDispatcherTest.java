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
package org.codehaus.wadi.group.impl;

import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.Envelope;
import org.codehaus.wadi.group.EnvelopeInterceptor;

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 * 
 * @version $Revision: 1538 $
 */
public class AbstractDispatcherTest extends RMockTestCase {

    private AbstractDispatcher dispatcher;
    private Address address;
    private EnvelopeInterceptor interceptor1;
    private EnvelopeInterceptor interceptor2;
    private Envelope envelope1;

    protected void setUp() throws Exception {
        ThreadPool threadPool = (ThreadPool) mock(ThreadPool.class);
        dispatcher = (AbstractDispatcher) intercept(AbstractDispatcher.class, new Object[] { threadPool }, "disp");
        address = (Address) mock(Address.class);
        interceptor1 = (EnvelopeInterceptor) mock(EnvelopeInterceptor.class);
        interceptor2 = (EnvelopeInterceptor) mock(EnvelopeInterceptor.class);
        envelope1 = (Envelope) mock(Envelope.class);
    }
    
    public void testOnOutboundEnvelopeEnvelopeListenersAreNotified() throws Exception {
        interceptor1.registerLoopbackEnvelopeListener(dispatcher);
        interceptor2.registerLoopbackEnvelopeListener(dispatcher);
        
        Envelope envelope2 = interceptor1.onOutboundEnvelope(envelope1);
        Envelope envelope3 = interceptor2.onOutboundEnvelope(envelope2);
        
        dispatcher.doSend(address, envelope3);
        
        startVerification();
        
        dispatcher.addInterceptor(interceptor1);
        dispatcher.addInterceptor(interceptor2);
        
        dispatcher.send(address, envelope1);
    }

    public void testOnOutboundEnvelopeEnvelopeListenerFilterOutEnvelope() throws Exception {
        interceptor1.registerLoopbackEnvelopeListener(dispatcher);
        interceptor2.registerLoopbackEnvelopeListener(dispatcher);
        
        Envelope envelope2 = interceptor1.onOutboundEnvelope(envelope1);
        interceptor2.onOutboundEnvelope(envelope2);
        modify().returnValue(null);
        startVerification();
        
        dispatcher.addInterceptor(interceptor1);
        dispatcher.addInterceptor(interceptor2);
        
        dispatcher.send(address, envelope1);
    }

    public void testOnInboundEnvelopeEnvelopeListenersAreNotified() throws Exception {
        interceptor1.registerLoopbackEnvelopeListener(dispatcher);
        interceptor2.registerLoopbackEnvelopeListener(dispatcher);
        
        Envelope envelope2 = interceptor1.onInboundEnvelope(envelope1);
        Envelope envelope3 = interceptor2.onInboundEnvelope(envelope2);
        
        dispatcher.doOnEnvelope(envelope3);
        
        startVerification();
        
        dispatcher.addInterceptor(interceptor1);
        dispatcher.addInterceptor(interceptor2);
        
        dispatcher.onEnvelope(envelope1);
    }

    public void testOnInboundEnvelopeEnvelopeListenerFilterOutEnvelope() throws Exception {
        interceptor1.registerLoopbackEnvelopeListener(dispatcher);
        interceptor2.registerLoopbackEnvelopeListener(dispatcher);
        
        Envelope envelope2 = interceptor1.onInboundEnvelope(envelope1);
        interceptor2.onInboundEnvelope(envelope2);
        modify().returnValue(null);
        startVerification();
        
        dispatcher.addInterceptor(interceptor1);
        dispatcher.addInterceptor(interceptor2);
        
        dispatcher.onEnvelope(envelope1);
    }
}
