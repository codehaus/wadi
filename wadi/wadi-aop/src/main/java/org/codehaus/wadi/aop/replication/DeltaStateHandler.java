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

import org.codehaus.wadi.aop.tracker.InstanceIdFactory;
import org.codehaus.wadi.aop.tracker.InstanceRegistry;
import org.codehaus.wadi.aop.util.ClusteredStateHelper;
import org.codehaus.wadi.core.WADIRuntimeException;
import org.codehaus.wadi.core.util.Streamer;
import org.codehaus.wadi.core.util.Utils;
import org.codehaus.wadi.replication.manager.basic.SessionStateHandler;


/**
 * 
 * @version $Revision: 1538 $
 */
public class DeltaStateHandler extends SessionStateHandler {
    private final InstanceIdFactory instanceIdFactory;
    private final InstanceRegistry instanceRegistry;

    public DeltaStateHandler(Streamer streamer,
            InstanceIdFactory instanceIdFactory,
            InstanceRegistry instanceRegistry) {
        super(streamer);
        if (null == instanceIdFactory) {
            throw new IllegalArgumentException("instanceIdFactory is required");
        } else if (null == instanceRegistry) {
            throw new IllegalArgumentException("instanceRegistry is required");
        }
        this.instanceIdFactory = instanceIdFactory;
        this.instanceRegistry = instanceRegistry;
    }

    @Override
    protected Externalizable newExtractFullStateExternalizable(Object key, Object target) {
        ClusteredStateSessionMemento memento = extractMemento(target);
        return new FullStateExternalizable(instanceIdFactory, memento);
    }
    
    @Override
    protected Externalizable newExtractUpdatedStateExternalizable(Object key, Object target) {
        ClusteredStateSessionMemento memento = extractMemento(target);
        return new UpdatedStateExternalizable(instanceIdFactory, memento);
    }
    
    @Override
    public void resetObjectState(Object target) {
        ClusteredStateSessionMemento memento = extractMemento(target);
        ClusteredStateHelper.resetTracker(memento);
    }
    
    public Object restoreFromFullState(Object key, byte[] state) {
        return restore(state);
    }

    public Object restoreFromUpdatedState(Object key, byte[] state) {
        return restore(state);
    }

    private Object restore(byte[] state) {
        RestoreStateExternalizable externalizable = new RestoreStateExternalizable(instanceRegistry);
        try {
            Utils.setContent(externalizable, state, streamer);
        } catch (Exception e) {
            throw new WADIRuntimeException(e);
        }
        ClusteredStateSessionMemento memento = externalizable.getMemento();
        
        ClusteredStateSession session = (ClusteredStateSession) sessionFactory.create();
        session.setDistributableSessionMemento(memento);
        return session;
    }
    
    protected ClusteredStateSessionMemento extractMemento(Object target) {
        if (false == target instanceof ClusteredStateSession) {
            throw new IllegalArgumentException(target.getClass().getName() + " is not a " + 
                ClusteredStateSession.class.getName());
        }
        ClusteredStateSession session = (ClusteredStateSession) target;
        return session.getClusteredStateSessionMemento();
    }
    
}
