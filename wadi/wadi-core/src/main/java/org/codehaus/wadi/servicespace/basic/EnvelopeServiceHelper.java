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
import org.codehaus.wadi.servicespace.ServiceName;


/**
 * 
 * @version $Revision: $
 */
public class EnvelopeServiceHelper {
    private static final String PROPERTY_TARGET_SERVICE_NAME = "wadi/TargetServiceName";
    private static final String PROPERTY_KEY_SERVICE_REPLY = "wadi/ServiceReply";

    public static void setServiceName(ServiceName serviceName, Envelope envelope) {
        envelope.setProperty(PROPERTY_TARGET_SERVICE_NAME, serviceName);
    }

    public static ServiceName getServiceName(Envelope envelope) {
        return (ServiceName) envelope.getProperty(PROPERTY_TARGET_SERVICE_NAME);
    }
    
    public static boolean isServiceReply(Envelope envelope) {
        return null != envelope.getProperty(PROPERTY_KEY_SERVICE_REPLY);
    }
    
    public static void tagAsServiceReply(Envelope envelope) {
        envelope.setProperty(PROPERTY_KEY_SERVICE_REPLY, Boolean.TRUE);
    }

}
