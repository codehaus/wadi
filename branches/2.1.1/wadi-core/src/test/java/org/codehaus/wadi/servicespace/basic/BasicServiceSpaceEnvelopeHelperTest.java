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

import java.util.Map;

import org.codehaus.wadi.core.util.SimpleStreamer;
import org.codehaus.wadi.group.Envelope;
import org.codehaus.wadi.group.vm.VMEnvelope;
import org.codehaus.wadi.servicespace.ServiceSpace;

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 *
 * @version $Rev:$ $Date:$
 */
public class BasicServiceSpaceEnvelopeHelperTest extends RMockTestCase {

    private BasicServiceSpaceEnvelopeHelper helper;

    @Override
    protected void setUp() throws Exception {
        ServiceSpace serviceSpace = (ServiceSpace) mock(ServiceSpace.class);
        
        helper = new BasicServiceSpaceEnvelopeHelper(serviceSpace, new SimpleStreamer());
    }
    
    public void testTransform() throws Exception {
        Envelope envelope = new VMEnvelope();

        String propServiceSpaceName = "name";
        envelope.setProperty(BasicServiceSpaceEnvelopeHelper.PROPERTY_SERVICE_SPACE_NAME, propServiceSpaceName);
        String propKey = "key";
        String propValue = "value";
        envelope.setProperty(propKey, propValue);

        String payload = "payload";
        envelope.setPayload(payload);

        helper.transformOutboundEnvelope(envelope);
        
        Map<String, Object> outProps = envelope.getProperties();
        assertEquals(2, outProps.size());
        assertEquals(propServiceSpaceName, outProps .get(BasicServiceSpaceEnvelopeHelper.PROPERTY_SERVICE_SPACE_NAME));
        assertEquals(Boolean.TRUE, outProps.get(BasicServiceSpaceEnvelopeHelper.PROPERTY_TRANSFORMED));
        
        assertTrue(envelope.getPayload() instanceof byte[]);
        
        helper.transformInboundEnvelope(envelope);

        Map<String, Object> inPros = envelope.getProperties();
        assertEquals(2, inPros.size());
        assertEquals(propServiceSpaceName, outProps .get(BasicServiceSpaceEnvelopeHelper.PROPERTY_SERVICE_SPACE_NAME));
        assertEquals(propValue, inPros.get(propKey));
        
        assertEquals(payload, envelope.getPayload());
    }
    
    public void testDoNotTransformedInboundEnvelope() throws Exception {
        Envelope envelope = new VMEnvelope();
        String payload = "value";
        envelope.setPayload(payload);
        helper.transformInboundEnvelope(envelope);
        
        assertEquals(payload, envelope.getPayload());
    }
    
    public void testDoNotTransformedAlreadyTransformedEnvelope() throws Exception {
        Envelope envelope = new VMEnvelope();
        String payload = "value";
        envelope.setPayload(payload);
        envelope.setProperty(BasicServiceSpaceEnvelopeHelper.PROPERTY_TRANSFORMED, Boolean.TRUE);
        helper.transformOutboundEnvelope(envelope);
        
        assertEquals(payload, envelope.getPayload());
    }
    
}
