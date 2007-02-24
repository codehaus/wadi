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
package org.codehaus.wadi.core.session;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Set;

import org.codehaus.wadi.Manager;
import org.codehaus.wadi.Streamer;
import org.codehaus.wadi.impl.Utils;

/**
 * A Standard Session enhanced with functionality associated with
 * [de]serialisation - necessary to allow the movement of the session from jvm
 * to jvm/storage.
 * 
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision: 1725 $
 */
public class DistributableSession extends StandardSession {
    
    private final Streamer streamer;

    public DistributableSession(Attributes attributes, Manager manager, Streamer streamer) {
        super(attributes, manager);
        if (null == streamer) {
            throw new IllegalArgumentException("streamer is required");
        }
        this.streamer = streamer;
    }

    public synchronized void readExternal(ObjectInput oi) throws IOException, ClassNotFoundException {
        super.readExternal(oi);
        ((DistributableAttributes) attributes).readExternal(oi);
        onDeserialization();
    }

    public synchronized void writeExternal(ObjectOutput oo) throws IOException {
        super.writeExternal(oo);
        ((DistributableAttributes) attributes).writeExternal(oo);
        onSerialization();
    }

    public synchronized byte[] getBodyAsByteArray() throws Exception {
        return Utils.getContent(this, streamer);
    }

    public synchronized void setBodyAsByteArray(byte[] bytes) throws IOException, ClassNotFoundException {
        Utils.setContent(this, bytes, streamer);
    }

    public synchronized Set getListenerNames() {
        return ((DistributableAttributes) attributes).getListenerNames();
    }
    
    protected void onDeserialization() {
    }
    
    protected void onSerialization() {
    }
}
