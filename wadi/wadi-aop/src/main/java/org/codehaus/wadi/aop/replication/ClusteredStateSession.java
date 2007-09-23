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
package org.codehaus.wadi.aop.replication;

import java.io.Externalizable;
import java.io.IOException;

import org.codehaus.wadi.aop.tracker.InstanceIdFactory;
import org.codehaus.wadi.aop.tracker.basic.BasicInstanceRegistry;
import org.codehaus.wadi.aop.util.ClusteredStateHelper;
import org.codehaus.wadi.core.WADIRuntimeException;
import org.codehaus.wadi.core.eviction.SimpleEvictableMemento;
import org.codehaus.wadi.core.manager.Manager;
import org.codehaus.wadi.core.session.AbstractReplicableSession;
import org.codehaus.wadi.core.util.Streamer;
import org.codehaus.wadi.core.util.Utils;
import org.codehaus.wadi.replication.manager.ReplicationManager;

/**
 * 
 * @version $Revision: 1538 $
 */
public class ClusteredStateSession extends AbstractReplicableSession {
    private final InstanceIdFactory instanceIdFactory;
    
    public ClusteredStateSession(ClusteredStateAttributes attributes,
            Manager manager,
            Streamer streamer,
            ReplicationManager replicationManager,
            InstanceIdFactory instanceIdFactory) {
        super(attributes, manager, streamer, replicationManager);
        if (null == instanceIdFactory) {
            throw new IllegalArgumentException("instanceIdFactory is required");
        }
        this.instanceIdFactory = instanceIdFactory;
    }

    @Override
    protected SimpleEvictableMemento newMemento() {
        return new ClusteredStateSessionMemento();
    }

    public ClusteredStateSessionMemento getClusteredStateSessionMemento() {
        return (ClusteredStateSessionMemento) memento;
    }
    
    @Override
    public synchronized byte[] getBodyAsByteArray() throws Exception {
        Externalizable externalizable = new FullStateExternalizable(instanceIdFactory, 
            (ClusteredStateSessionMemento) memento);
        return Utils.getContent(externalizable, streamer);
    }
    
    @Override
    public synchronized void setBodyAsByteArray(byte[] bytes) throws IOException, ClassNotFoundException {
        RestoreStateExternalizable externalizable = new RestoreStateExternalizable(streamer, new BasicInstanceRegistry());
        try {
            Utils.setContent(externalizable, bytes, streamer);
        } catch (Exception e) {
            throw new WADIRuntimeException(e);
        }
        ClusteredStateSessionMemento clusteredMemento = externalizable.getMemento();
        clusteredMemento.onRestore();

        attributes.setMemento(clusteredMemento.getAttributesMemento());

        ClusteredStateHelper.resetTracker(clusteredMemento);
        memento = clusteredMemento;
    }
    
}
