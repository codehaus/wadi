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
import org.codehaus.wadi.servicespace.ServiceSpaceName;


/**
 * 
 * @version $Revision: $
 */
public class ServiceSpaceEndpointTest extends AbstractServiceSpaceTestCase {

    public void testDispatch() throws Exception {
        Envelope message = (Envelope) mock(Envelope.class);
        dispatcher.onEnvelope(message);
        
        startVerification();
        
        ServiceSpaceEndpoint endpoint = new ServiceSpaceEndpoint(serviceSpace, dispatcher);
        endpoint.dispatch(message);
    }

    public void testTestDispatchMessageOK() {
        beginSection(s.ordered("Get ServiceSpaceName and test for equality"));
        Envelope message = (Envelope) mock(Envelope.class);
        ServiceSpaceEnvelopeHelper.getServiceSpaceNameStatic(message);
        modify().returnValue(serviceSpaceName);
        endSection();

        startVerification();
        
        ServiceSpaceEndpoint endpoint = new ServiceSpaceEndpoint(serviceSpace, dispatcher);
        assertTrue(endpoint.testDispatchEnvelope(message));
    }

    public void testTestDispatchMessageServiceSpaceIsDifferent() throws Exception {
        beginSection(s.ordered("Get ServiceSpaceName and test for equality"));
        Envelope message = (Envelope) mock(Envelope.class);
        ServiceSpaceEnvelopeHelper.getServiceSpaceNameStatic(message);
        modify().returnValue(new ServiceSpaceName(new URI("NEW_SPACE")));
        endSection();

        startVerification();
        
        ServiceSpaceEndpoint endpoint = new ServiceSpaceEndpoint(serviceSpace, dispatcher);
        assertFalse(endpoint.testDispatchEnvelope(message));
    }

    public void testTestDispatchMessageServiceSpaceIsUndefined() throws Exception {
        beginSection(s.ordered("Get ServiceSpaceName and test for equality"));
        Envelope message = (Envelope) mock(Envelope.class);
        ServiceSpaceEnvelopeHelper.getServiceSpaceNameStatic(message);
        endSection();

        startVerification();
        
        ServiceSpaceEndpoint endpoint = new ServiceSpaceEndpoint(serviceSpace, dispatcher);
        assertFalse(endpoint.testDispatchEnvelope(message));
    }

}
