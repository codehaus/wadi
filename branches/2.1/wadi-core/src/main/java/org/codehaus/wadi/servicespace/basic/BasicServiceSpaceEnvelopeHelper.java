/**
 * Copyright 2008 The Apache Software Foundation
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.codehaus.wadi.core.util.Streamer;
import org.codehaus.wadi.group.Envelope;
import org.codehaus.wadi.servicespace.ServiceSpace;
import org.codehaus.wadi.servicespace.ServiceSpaceName;

/**
 * 
 * @version $Revision: $
 */
public class BasicServiceSpaceEnvelopeHelper implements ServiceSpaceEnvelopeHelper {
    protected static final String PROPERTY_SERVICE_SPACE_NAME = "wadi/ServiceSpaceName";
    protected static final String PROPERTY_TRANSFORMED = "wadi/Transformed";

    private final ServiceSpace serviceSpace;
    private final Streamer streamer;
 
    public static void setServiceSpaceName(ServiceSpaceName serviceSpaceName, Envelope envelope) {
        envelope.setProperty(PROPERTY_SERVICE_SPACE_NAME, serviceSpaceName);
    }

    public static ServiceSpaceName getServiceSpaceNameStatic(Envelope envelope) {
        return (ServiceSpaceName) envelope.getProperty(PROPERTY_SERVICE_SPACE_NAME);
    }
    
    public BasicServiceSpaceEnvelopeHelper(ServiceSpace serviceSpace, Streamer streamer) {
        if (null == serviceSpace) {
            throw new IllegalArgumentException("serviceSpace is required");
        } else if (null == streamer) {
            throw new IllegalArgumentException("streamer is required");
        }
        this.serviceSpace = serviceSpace;
        this.streamer = streamer;
    }
    
    public ServiceSpaceName getServiceSpaceName(Envelope envelope) {
        return (ServiceSpaceName) envelope.getProperty(PROPERTY_SERVICE_SPACE_NAME);
    }

    public void setServiceSpaceName(Envelope envelope) {
        setServiceSpaceName(serviceSpace.getServiceSpaceName(), envelope);
    }

    public void transformOutboundEnvelope(Envelope envelope) throws IOException {
        if (Boolean.TRUE.equals(envelope.getProperty(PROPERTY_TRANSFORMED))) {
            return;
        }

        ByteArrayOutputStream memOut = new ByteArrayOutputStream();
        ObjectOutput oos = streamer.getOutputStream(memOut);

        Map<String, Object> properties = envelope.getProperties();
        int nbProperties = properties.size();
        if (properties.containsKey(PROPERTY_SERVICE_SPACE_NAME)) {
            nbProperties = properties.size() - 1;
        }
        properties = new HashMap<String, Object>(properties);
        oos.writeInt(nbProperties);
        for (Iterator<Map.Entry<String, Object>> iterator = properties.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry<String, Object> entry = iterator.next();
            String key = entry.getKey();
            if (key.equals(PROPERTY_SERVICE_SPACE_NAME)) {
                continue;
            }
            oos.writeUTF(key);
            oos.writeObject(entry.getValue());
            envelope.removeProperty(key);
        }
        
        oos.writeObject(envelope.getPayload());
        oos.close();
        
        envelope.setPayload(memOut.toByteArray());
        
        envelope.setProperty(PROPERTY_TRANSFORMED, Boolean.TRUE);
    }

    public void transformInboundEnvelope(Envelope envelope) throws IOException, ClassNotFoundException {
        if (!Boolean.TRUE.equals(envelope.getProperty(PROPERTY_TRANSFORMED))) {
            return;
        }
        
        Object payload = envelope.getPayload();
        if (!(payload instanceof byte[])) {
            throw new IllegalStateException("Payload is of type [" + payload.getClass().getName() + "]; expected ["
                    + byte[].class + "]");
        }
        ObjectInput ois = streamer.getInputStream(new ByteArrayInputStream((byte[]) payload));

        int nbProperties = ois.readInt();
        for (int i = 0; i < nbProperties; i++) {
            String key = ois.readUTF();
            Object value = ois.readObject();
            envelope.setProperty(key, value);
        }

        Serializable actualPayload = (Serializable) ois.readObject();
        envelope.setPayload(actualPayload);
        
        envelope.removeProperty(PROPERTY_TRANSFORMED);
    }
    
}
