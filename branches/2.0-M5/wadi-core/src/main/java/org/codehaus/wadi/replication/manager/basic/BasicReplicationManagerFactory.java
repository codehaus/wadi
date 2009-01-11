/**
 * Copyright 2006 The Apache Software Foundation
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
package org.codehaus.wadi.replication.manager.basic;

import org.codehaus.wadi.replication.manager.ReplicationManager;
import org.codehaus.wadi.replication.manager.ReplicationManagerFactory;
import org.codehaus.wadi.replication.strategy.BackingStrategy;
import org.codehaus.wadi.replication.strategy.BackingStrategyFactory;
import org.codehaus.wadi.servicespace.ServiceSpace;

/**
 * 
 * @version $Revision$
 */
public class BasicReplicationManagerFactory implements ReplicationManagerFactory {

    private final ObjectStateHandler stateHandler;
    
    public BasicReplicationManagerFactory(ObjectStateHandler stateHandler) {
        if (null == stateHandler) {
            throw new IllegalArgumentException("stateHandler is required");
        }
        this.stateHandler = stateHandler;
    }

    public ReplicationManager factory(ServiceSpace serviceSpace, BackingStrategyFactory backingStrategyFactory) {
        BackingStrategy backingStrategy = backingStrategyFactory.factory();
        return new BasicReplicationManager(serviceSpace, stateHandler, backingStrategy);
    }

}