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
package org.codehaus.wadi.servicespace.basic;

import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.Envelope;
import org.codehaus.wadi.group.ServiceEndpoint;
import org.codehaus.wadi.group.impl.EnvelopeHelper;
import org.codehaus.wadi.servicespace.ServiceSpace;
import org.codehaus.wadi.servicespace.ServiceSpaceName;

/**
 * 
 * @version $Revision: 1603 $
 */
public class RendezVousEndPoint implements ServiceEndpoint {
    private final Dispatcher dispatcher;
    private final ServiceSpaceName name;
    private final ServiceSpaceEnvelopeHelper envelopeHelper;
    
    public RendezVousEndPoint(ServiceSpace serviceSpace, ServiceSpaceEnvelopeHelper envelopeHelper) {
        if (null == serviceSpace) {
            throw new IllegalArgumentException("serviceSpace is required");
        } else if (null == envelopeHelper) {
            throw new IllegalArgumentException("envelopeHelper is required");
        }
        this.envelopeHelper = envelopeHelper;

        dispatcher = serviceSpace.getDispatcher();
        name = serviceSpace.getServiceSpaceName();
    }

    public void dispatch(Envelope envelope) throws Exception {
        dispatcher.addRendezVousEnvelope(envelope);
    }

    public void dispose(int nbAttemp, long delayMillis) {
    }

    public boolean testDispatchEnvelope(Envelope envelope) {
        ServiceSpaceName targetedServiceSpaceName = envelopeHelper.getServiceSpaceName(envelope);
        if (null == targetedServiceSpaceName) {
            return false;
        }
        if (!targetedServiceSpaceName.equals(name)) {
            return false;
        }
        return EnvelopeHelper.isReply(envelope);
    }
    
}
