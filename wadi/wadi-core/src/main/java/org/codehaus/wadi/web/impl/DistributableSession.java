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
package org.codehaus.wadi.web.impl;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Set;

import org.codehaus.wadi.Streamer;
import org.codehaus.wadi.ValueHelper;
import org.codehaus.wadi.impl.Utils;
import org.codehaus.wadi.web.DistributableAttributesConfig;
import org.codehaus.wadi.web.DistributableSessionConfig;

/**
 * A Standard Session enhanced with functionality associated with
 * [de]serialisation - necessary to allow the movement of the session from jvm
 * to jvm/storage.
 * 
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision: 1725 $
 */
public class DistributableSession extends StandardSession implements DistributableAttributesConfig {
    private final Streamer streamer;
    
    public DistributableSession(DistributableSessionConfig config) {
        super(config);

        streamer = config.getStreamer();
    }

    public Streamer getStreamer() {
        return streamer;
    }

    public void readContent(ObjectInput oi) throws IOException, ClassNotFoundException {
        super.readContent(oi);
        ((DistributableAttributes) attributes).readContent(oi);
    }

    public void writeContent(ObjectOutput oo) throws IOException {
        super.writeContent(oo);
        ((DistributableAttributes) attributes).writeContent(oo);
    }

    public byte[] getBodyAsByteArray() throws Exception {
        return Utils.getContent(this, streamer);
    }

    public void setBodyAsByteArray(byte[] bytes) throws IOException, ClassNotFoundException {
        Utils.setContent(this, bytes, streamer);
    }

    public ValueHelper findHelper(Class type) {
        return ((DistributableSessionConfig) config).findHelper(type);
    }

    public Set getListenerNames() {
        return ((DistributableAttributes) attributes).getListenerNames();
    }

    public boolean getHttpSessionAttributeListenersRegistered() {
        return ((DistributableSessionConfig) config).getHttpSessionAttributeListenersRegistered();
    }

}
