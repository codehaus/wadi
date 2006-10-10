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
import org.codehaus.wadi.group.ServiceEndpoint;
import org.codehaus.wadi.servicespace.ServiceSpace;
import org.codehaus.wadi.servicespace.ServiceSpaceName;

/**
 * 
 * @version $Revision: $
 */
public class ServiceSpaceEndpoing implements ServiceEndpoint {
    private final ServiceSpace serviceSpace;
    private final ServiceSpaceMessageHelper messageHelper;
    
    public ServiceSpaceEndpoing(ServiceSpace serviceSpace) {
        if (null == serviceSpace) {
            throw new IllegalArgumentException("serviceSpace is required");
        }
        this.serviceSpace = serviceSpace;
        
        messageHelper = new ServiceSpaceMessageHelper(serviceSpace);
    }

    public void dispatch(Envelope om) throws Exception {
        serviceSpace.getDispatcher().onMessage(om);
    }

    public void dispose(int nbAttemp, long delayMillis) {
        return;
    }

    public boolean testDispatchMessage(Envelope om) {
        ServiceSpaceName serviceSpaceName = messageHelper.getServiceSpaceName(om);
        if (null == serviceSpaceName) {
            return false;
        }
        return serviceSpaceName.equals(serviceSpace.getServiceSpaceName());
    }
    
}