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
import org.codehaus.wadi.servicespace.ServiceSpace;
import org.codehaus.wadi.servicespace.ServiceSpaceName;

/**
 * 
 * @version $Revision: $
 */
public class ServiceSpaceEnvelopeHelper {
    private static final String PROPERTY_SERVICE_SPACE_NAME = "wadi/ServiceSpaceName";

    private final ServiceSpace serviceSpace;
 
    public static void setServiceSpaceName(ServiceSpaceName serviceSpaceName, Envelope envelope) {
        envelope.setProperty(PROPERTY_SERVICE_SPACE_NAME, serviceSpaceName);
    }

    public static ServiceSpaceName getServiceSpaceNameStatic(Envelope envelope) {
        return (ServiceSpaceName) envelope.getProperty(PROPERTY_SERVICE_SPACE_NAME);
    }
    
    public ServiceSpaceEnvelopeHelper(ServiceSpace serviceSpace) {
        if (null == serviceSpace) {
            throw new IllegalArgumentException("serviceSpace is required");
        }
        this.serviceSpace = serviceSpace;
    }
    
    public ServiceSpaceName getServiceSpaceName(Envelope envelope) {
        return (ServiceSpaceName) envelope.getProperty(PROPERTY_SERVICE_SPACE_NAME);
    }

    public void setServiceSpaceName(Envelope envelope) {
        setServiceSpaceName(serviceSpace.getServiceSpaceName(), envelope);
    }
    
}
