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

import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.Envelope;
import org.codehaus.wadi.group.ServiceEndpoint;
import org.codehaus.wadi.servicespace.ServiceSpace;

/**
 * 
 * @version $Revision: $
 */
public class ServiceResponseListener implements ServiceEndpoint {
    
    private final Dispatcher dispatcher;
    
    public ServiceResponseListener(ServiceSpace serviceSpace) {
        if (null == serviceSpace) {
            throw new IllegalArgumentException("serviceSpace is required");
        }
        
        dispatcher = serviceSpace.getDispatcher();
    }

    public void dispatch(Envelope envelope) throws Exception {
        dispatcher.addRendezVousEnvelope(envelope);
    }

    public void dispose(int nbAttemp, long delayMillis) {
    }

    public boolean testDispatchEnvelope(Envelope envelope) {
        return EnvelopeServiceHelper.isServiceReply(envelope);
    }
    
}
