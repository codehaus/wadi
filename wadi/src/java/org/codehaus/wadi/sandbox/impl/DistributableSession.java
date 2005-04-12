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
package org.codehaus.wadi.sandbox.impl;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.codehaus.wadi.StreamingStrategy;
import org.codehaus.wadi.sandbox.Dirtier;
import org.codehaus.wadi.sandbox.DistributableAttributesConfig;
import org.codehaus.wadi.sandbox.DistributableSessionConfig;
import org.codehaus.wadi.sandbox.ValueHelper;

/**
 * TODO - JavaDoc this type
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public class DistributableSession extends StandardSession implements DistributableAttributesConfig {

    public DistributableSession(DistributableSessionConfig config) {super(config);}

    public Dirtier getDirtier() {return ((DistributableSessionConfig)_config).getDirtier();}
    public StreamingStrategy getStreamer() {return ((DistributableSessionConfig)_config).getStreamer();}
    
    public void readContent(ObjectInput oi) throws IOException, ClassNotFoundException {
        super.readContent(oi);
        _attributes.readContent(oi);
    }
    
    public void writeContent(ObjectOutput oo) throws IOException {
        super.writeContent(oo);
        _attributes.writeContent(oo);
    }
    
    public byte[] getBytes() throws Exception {return Utils.getContent(this, getStreamer());}
    public void setBytes(byte[] bytes) throws IOException, ClassNotFoundException {Utils.setContent(this, bytes, getStreamer());}
    
    public ValueHelper findHelper(Class type){return ((DistributableSessionConfig)_config).findHelper(type);}
    public boolean hasAttributeListeners(){return ((DistributableSessionConfig)_config).hasAttributeListeners();}
}
