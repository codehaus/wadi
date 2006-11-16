/**
 *
 * Copyright 2003-2005 Core Developers Network Ltd.
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
package org.codehaus.wadi.sandbox.dirtier;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;
import java.util.Set;

import javax.servlet.http.HttpSessionEvent;

import org.codehaus.wadi.Streamer;
import org.codehaus.wadi.impl.SimpleStreamer;
import org.codehaus.wadi.impl.Utils;
import org.codehaus.wadi.web.Attributes;

/**
 * TODO - JavaDoc this type
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public class SimpleAttributes extends HashMap implements Attributes {
    
    protected final static Streamer _streamer=new SimpleStreamer(); // TODO - parameterise 
    
    public byte[] getBytes() {
        return Utils.safeObjectToByteArray(this, _streamer);
    }
    
    public void setBytes(byte[] bytes) {
        HashMap attributes=(HashMap)Utils.safeByteArrayToObject(bytes, _streamer);
        putAll(attributes);
    }
    
    public void setHttpSessionEvent(HttpSessionEvent event) {
        // we don't need it...
    }
    
    // FIXME
    public Set getBindingListenerNames() {return null;}
    public Set getActivationListenerNames() {return null;}

    public void readContent(ObjectInput oi) throws IOException, ClassNotFoundException {
        throw new NotSerializableException();
    }
    
    public void writeContent(ObjectOutput oo) throws IOException {
        throw new NotSerializableException();
    }

}
