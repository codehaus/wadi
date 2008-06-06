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
package org.codehaus.wadi.core.motable;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.codehaus.wadi.core.eviction.SimpleEvictableMemento;

/**
 * 
 * @version $Revision: 1538 $
 */
public class AbstractMotableMemento extends SimpleEvictableMemento {
    protected String name;
    protected boolean newSession = true;
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public boolean isNewSession() {
        return newSession;
    }
    
    public void setNewSession(boolean newSession) {
        this.newSession = newSession;
    }

    public synchronized void readExternal(ObjectInput oi) throws IOException, ClassNotFoundException {
        super.readExternal(oi);
        name = oi.readUTF();
        newSession = false;
    }

    public synchronized void writeExternal(ObjectOutput oo) throws IOException {
        super.writeExternal(oo);
        oo.writeUTF(name);
    }

}


