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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;


/**
 * A DistributableAttributes object needs to be Listener aware. When a Session is invalidated in Serialised
 * state, we only want to deserialise the Attributes that we absolutely have to - in other words, those
 * expecting some kind of notification (activation or unbinding). If the Context has HttpSessionAttributeListeners
 * registered, then we will have to explicitly remove every attribute from every session anyway, so there is no need
 * to keep a separate tally.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision: 1139 $
 */
public class DistributableAttributes extends StandardAttributes implements Externalizable {
    
    public DistributableAttributes(ValueFactory valueFactory) {
        super(valueFactory);
    }

    @Override
    protected StandardAttributesMemento newMemento() {
        return new DistributableAttributesMemento();
    }
    
    public synchronized void readExternal(ObjectInput oi) throws IOException, ClassNotFoundException {
        memento = newMemento();
        ((DistributableAttributesMemento) memento).readExternal(oi);
    }

    public synchronized void writeExternal(ObjectOutput oo) throws IOException {
        ((DistributableAttributesMemento) memento).writeExternal(oo);
    }

}
