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

import java.net.URI;

import org.codehaus.wadi.group.Envelope;
import org.codehaus.wadi.group.EnvelopeListener;
import org.codehaus.wadi.servicespace.ServiceSpaceName;


/**
 * 
 * @version $Revision: $
 */
public class ServiceSpaceEndpointTest extends AbstractServiceSpaceTestCase {

    private ServiceSpaceEnvelopeHelper helper;
    private Envelope message;
    private EnvelopeListener envelopeListener;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        envelopeListener = (EnvelopeListener) mock(EnvelopeListener.class);
        helper = (ServiceSpaceEnvelopeHelper) mock(ServiceSpaceEnvelopeHelper.class);
        message = (Envelope) mock(Envelope.class);
    }
    
    public void testDispatch() throws Exception {
        beginSection(s.ordered("tranform then dispatch"));
        envelopeListener.onEnvelope(message);
        endSection();
        
        startVerification();
        
        ServiceSpaceEndpoint endpoint = new ServiceSpaceEndpoint(serviceSpace, envelopeListener, helper);
        endpoint.dispatch(message);
    }

    public void testTestDispatchMessageOK() {
        beginSection(s.ordered("Get ServiceSpaceName and test for equality"));
        helper.getServiceSpaceName(message);
        modify().returnValue(serviceSpaceName);
        endSection();

        startVerification();
        
        ServiceSpaceEndpoint endpoint = new ServiceSpaceEndpoint(serviceSpace, envelopeListener, helper);
        assertTrue(endpoint.testDispatchEnvelope(message));
    }

    public void testTestDispatchMessageServiceSpaceIsDifferent() throws Exception {
        beginSection(s.ordered("Get ServiceSpaceName and test for equality"));
        helper.getServiceSpaceName(message);
        modify().returnValue(new ServiceSpaceName(new URI("NEW_SPACE")));
        endSection();

        startVerification();
        
        ServiceSpaceEndpoint endpoint = new ServiceSpaceEndpoint(serviceSpace, envelopeListener, helper);
        assertFalse(endpoint.testDispatchEnvelope(message));
    }

    public void testTestDispatchMessageServiceSpaceIsUndefined() throws Exception {
        beginSection(s.ordered("Get ServiceSpaceName and test for equality"));
        helper.getServiceSpaceName(message);
        endSection();

        startVerification();
        
        ServiceSpaceEndpoint endpoint = new ServiceSpaceEndpoint(serviceSpace, envelopeListener, helper);
        assertFalse(endpoint.testDispatchEnvelope(message));
    }

}
