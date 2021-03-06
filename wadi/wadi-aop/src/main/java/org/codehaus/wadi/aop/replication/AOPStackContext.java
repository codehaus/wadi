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

import org.codehaus.wadi.aop.aspectj.ClusteredStateAspectUtil;
import org.codehaus.wadi.aop.reflect.ClusteredStateMemberFilter;
import org.codehaus.wadi.aop.tracker.InstanceTrackerFactory;
import org.codehaus.wadi.aop.tracker.basic.BasicInstanceIdFactory;
import org.codehaus.wadi.aop.tracker.basic.BasicInstanceRegistry;
import org.codehaus.wadi.aop.tracker.basic.BasicInstanceTrackerFactory;
import org.codehaus.wadi.aop.tracker.basic.BasicWireMarshaller;
import org.codehaus.wadi.aop.tracker.basic.CompoundReplacer;
import org.codehaus.wadi.aop.tracker.basic.InstanceAndTrackerReplacer;
import org.codehaus.wadi.aop.tracker.basic.WireMarshaller;
import org.codehaus.wadi.core.assembler.StackContext;
import org.codehaus.wadi.core.reflect.ClassIndexerRegistry;
import org.codehaus.wadi.core.reflect.jdk.JDKClassIndexerRegistry;
import org.codehaus.wadi.core.session.SessionFactory;
import org.codehaus.wadi.core.session.ValueFactory;
import org.codehaus.wadi.core.session.ValueHelperRegistry;
import org.codehaus.wadi.core.util.Streamer;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.replication.manager.basic.ObjectStateHandler;
import org.codehaus.wadi.replication.strategy.BackingStrategyFactory;
import org.codehaus.wadi.servicespace.ServiceSpaceName;


/**
 * 
 * @version $Revision: 1538 $
 */
public class AOPStackContext extends StackContext {

    private InstanceAndTrackerReplacer replacer;
    private BasicInstanceIdFactory instanceIdFactory;
    private BasicInstanceRegistry instanceRegistry;
    private ClassIndexerRegistry classIndexerRegistry;

    public AOPStackContext(ClassLoader cl,
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
    
    @Override
    public void build() throws Exception {
        replacer = new CompoundReplacer();
        classIndexerRegistry = newClassIndexerRegistry();

        InstanceTrackerFactory instanceTrackerFactory = newInstanceTrackerFactory();
        ClusteredStateAspectUtil.setInstanceTrackerFactory(instanceTrackerFactory);
        
        instanceIdFactory = new BasicInstanceIdFactory(underlyingDispatcher.getCluster().getLocalPeer().getName());
        instanceRegistry = new BasicInstanceRegistry();
        
        super.build();
    }

    protected BasicInstanceTrackerFactory newInstanceTrackerFactory() {
        return new BasicInstanceTrackerFactory(replacer, classIndexerRegistry);
    }
    
    protected ClassIndexerRegistry newClassIndexerRegistry() {
        return new JDKClassIndexerRegistry(new ClusteredStateMemberFilter());
    }

    @Override
    protected ObjectStateHandler newObjectStateHandler(Streamer streamer) {
        WireMarshaller marshaller = new BasicWireMarshaller(streamer, classIndexerRegistry, replacer);
        return new DeltaStateHandler(streamer, marshaller, instanceIdFactory, instanceRegistry);
    }

    @Override
    protected SessionFactory newSessionFactory(Streamer streamer) {
        ValueHelperRegistry valueHelperRegistry = newValueHelperRegistry();
        ValueFactory valueFactory = newValueFactory(valueHelperRegistry);
        return new ClusteredStateSessionFactory(
                new ClusteredStateAttributesFactory(valueFactory),
                streamer,
                replicationManager,
                stateHandler);
    }
    
}
