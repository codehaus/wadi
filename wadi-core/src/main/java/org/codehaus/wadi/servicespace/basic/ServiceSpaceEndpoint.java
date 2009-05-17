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

import org.codehaus.wadi.group.Envelope;
import org.codehaus.wadi.group.EnvelopeListener;
import org.codehaus.wadi.group.ServiceEndpoint;
import org.codehaus.wadi.servicespace.ServiceSpace;
import org.codehaus.wadi.servicespace.ServiceSpaceName;

/**
 * 
 * @version $Revision: $
 */
public class ServiceSpaceEndpoint implements ServiceEndpoint {
    private final ServiceSpace serviceSpace;
    private final ServiceSpaceEnvelopeHelper envelopeHelper;
    private final EnvelopeListener envelpeListener;
    
    public ServiceSpaceEndpoint(ServiceSpace serviceSpace,
        EnvelopeListener envelpeListener,
        ServiceSpaceEnvelopeHelper envelopeHelper) {
        if (null == serviceSpace) {
            throw new IllegalArgumentException("serviceSpace is required");
        } else if (null == envelpeListener) {
            throw new IllegalArgumentException("envelpeListener is required");
        } else if (null == envelopeHelper) {
            throw new IllegalArgumentException("envelopeHelper is required");
        }
        this.serviceSpace = serviceSpace;
        this.envelpeListener = envelpeListener;
        this.envelopeHelper = envelopeHelper;
    }

    public void dispatch(Envelope envelope) throws Exception {
        envelpeListener.onEnvelope(envelope);
    }

    public void dispose(int nbAttemp, long delayMillis) {
        return;
    }

    public boolean testDispatchEnvelope(Envelope envelope) {
        ServiceSpaceName serviceSpaceName = envelopeHelper.getServiceSpaceName(envelope);
        if (null == serviceSpaceName) {
            return false;
        }
        return serviceSpaceName.equals(serviceSpace.getServiceSpaceName());
    }
    
}