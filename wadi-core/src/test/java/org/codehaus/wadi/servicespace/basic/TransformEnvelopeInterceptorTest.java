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

package org.codehaus.wadi.servicespace.basic;

import org.codehaus.wadi.group.Envelope;

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 *
 * @version $Rev:$ $Date:$
 */
public class TransformEnvelopeInterceptorTest extends RMockTestCase {

    private ServiceSpaceEnvelopeHelper helper;
    private TransformEnvelopeInterceptor interceptor;
    private Envelope envelope;

    @Override
    protected void setUp() throws Exception {
        helper = (ServiceSpaceEnvelopeHelper) mock(ServiceSpaceEnvelopeHelper.class);
        interceptor = new TransformEnvelopeInterceptor(helper);
        envelope = (Envelope) mock(Envelope.class);
    }
    
    public void testTranformOutboundOnOutboundEnvelope() throws Exception {
        helper.transformOutboundEnvelope(envelope);
        
        startVerification();
        
        Envelope onOutboundEnvelope = interceptor.onOutboundEnvelope(envelope);
        assertSame(envelope, onOutboundEnvelope);
    }
    
    public void testTranformInboundOnInboundEnvelope() throws Exception {
        helper.transformInboundEnvelope(envelope);
        
        startVerification();
        
        Envelope onInboundEnvelope = interceptor.onInboundEnvelope(envelope);
        assertSame(envelope, onInboundEnvelope);
    }
    
}
