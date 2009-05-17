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
package org.codehaus.wadi.core.session;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Iterator;
import java.util.Map;



/**
 * @version $Revision: 1497 $
 */
public class DistributableAttributesMemento extends StandardAttributesMemento implements Externalizable {

    public synchronized void readExternal(ObjectInput oi) throws IOException, ClassNotFoundException {
        int size = oi.readInt();
        for (int i = 0; i < size; i++) {
            Object key = oi.readObject();
            Object value = oi.readObject();
            attributes.put(key, value);
        }
    }

    public synchronized void writeExternal(ObjectOutput oo) throws IOException {
        oo.writeInt(attributes.size());
        for (Iterator i = attributes.entrySet().iterator(); i.hasNext();) {
            Map.Entry e = (Map.Entry) i.next();
            oo.writeObject(e.getKey());
            oo.writeObject(e.getValue());
        }
    }
    
}
