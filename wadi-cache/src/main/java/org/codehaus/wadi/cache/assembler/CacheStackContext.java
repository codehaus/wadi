/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.codehaus.wadi.cache.assembler;

import org.codehaus.wadi.cache.store.ObjectLoader;
import org.codehaus.wadi.cache.store.ObjectLoaderContextualiser;
import org.codehaus.wadi.cache.store.ObjectWriter;
import org.codehaus.wadi.cache.store.ObjectWriterContextualiser;
import org.codehaus.wadi.core.assembler.StackContext;
import org.codehaus.wadi.core.contextualiser.Contextualiser;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.replication.strategy.BackingStrategyFactory;
import org.codehaus.wadi.servicespace.ServiceSpaceName;

/**
 *
 * @version $Rev:$ $Date:$
 */
public class CacheStackContext extends StackContext {
    private ObjectLoader objectLoader;
    private ObjectWriter objectWriter;
    
    public CacheStackContext(ServiceSpaceName serviceSpaceName, Dispatcher underlyingDispatcher, int sessionTimeout) {
        super(serviceSpaceName, underlyingDispatcher, sessionTimeout);
    }

    public CacheStackContext(ServiceSpaceName serviceSpaceName, Dispatcher underlyingDispatcher) {
        super(serviceSpaceName, underlyingDispatcher);
    }

    public CacheStackContext(ClassLoader cl,
            ServiceSpaceName serviceSpaceName,
            Dispatcher underlyingDispatcher,
            int sessionTimeout,
            int numPartitions,
            int sweepInterval,
            BackingStrategyFactory backingStrategyFactory) {
        super(cl,
                serviceSpaceName,
                underlyingDispatcher,
                sessionTimeout,
                numPartitions,
                sweepInterval,
                backingStrategyFactory);
    }

    public ObjectLoader getObjectLoader() {
        return objectLoader;
    }

    public void setObjectLoader(ObjectLoader objectLoader) {
        this.objectLoader = objectLoader;
    }
    
    public ObjectWriter getObjectWriter() {
        return objectWriter;
    }

    public void setObjectWriter(ObjectWriter objectWriter) {
        this.objectWriter = objectWriter;
    }

    @Override
    protected Contextualiser newSharedStoreContextualiser(Contextualiser next) {
        next = newObjectWriterContextualiser(next);
        next = newObjectLoaderContextualiser(next);
        return next;
    }

    protected Contextualiser newObjectLoaderContextualiser(Contextualiser next) {
        if (null == objectLoader) {
            return next;
        }
        
        return new ObjectLoaderContextualiser(next,
                objectLoader,
                sessionFactory,
                sessionMonitor,
                stateManager,
                replicationManager);
    }

    protected Contextualiser newObjectWriterContextualiser(Contextualiser next) {
        if (null == objectWriter) {
            return next;
        }
        
        return new ObjectWriterContextualiser(next,
                objectWriter,
                sessionFactory,
                stateManager,
                replicationManager);
    }
}