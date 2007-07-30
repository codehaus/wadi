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

import org.codehaus.wadi.core.eviction.SimpleEvictableMemento;
import org.codehaus.wadi.core.manager.Manager;
import org.codehaus.wadi.core.util.Streamer;
import org.codehaus.wadi.core.util.Utils;

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

    public DistributableSession(DistributableAttributes attributes, Manager manager, Streamer streamer) {
        super(attributes, manager);
        if (null == streamer) {
            throw new IllegalArgumentException("streamer is required");
        }
        this.streamer = streamer;
    }

    @Override
    protected SimpleEvictableMemento newMemento() {
        return new DistributableSessionMemento();
    }
    
    public DistributableSessionMemento getDistributableSessionMemento() {
        return (DistributableSessionMemento) memento;
    }
    
    public void setDistributableSessionMemento(DistributableSessionMemento memento) {
        this.memento = memento;
        attributes.setMemento(memento.getAttributesMemento());
    }
    
    public synchronized byte[] getBodyAsByteArray() throws Exception {
        return Utils.getContent(this, streamer);
    }

    public synchronized void setBodyAsByteArray(byte[] bytes) throws IOException, ClassNotFoundException {
        Utils.setContent(this, bytes, streamer);
    }

}
