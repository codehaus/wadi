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
import org.codehaus.wadi.group.MessageListener;
import org.codehaus.wadi.servicespace.ServiceSpace;

/**
 * 
 * @version $Revision: $
 */
public class ServiceResponseListener implements MessageListener {
    
    private final ServiceSpace serviceSpace;
    private final Dispatcher dispatcher;
    private final MessageListener next;
    
    public ServiceResponseListener(ServiceSpace serviceSpace, MessageListener next) {
        if (null == serviceSpace) {
            throw new IllegalArgumentException("serviceSpace is required");
        } else if (null == next) {
            throw new IllegalArgumentException("next is required");
        }
        this.serviceSpace = serviceSpace;
        this.next = next;
        
        dispatcher = serviceSpace.getDispatcher();
    }

    public void onMessage(Envelope message) {
        if (EnvelopeServiceHelper.isServiceReply(message)) {
            handleServiceReply(message);
        } else {
            next.onMessage(message);
        }
    }

    private void handleServiceReply(Envelope reply) {
        dispatcher.addRendezVousEnvelope(reply);
    }
    
}
