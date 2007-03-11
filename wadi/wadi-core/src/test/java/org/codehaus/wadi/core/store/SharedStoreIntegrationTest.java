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
package org.codehaus.wadi.core.store;

import java.net.URI;

import junit.framework.TestCase;

import org.apache.derby.jdbc.EmbeddedDataSource;
import org.codehaus.wadi.core.ConcurrentMotableMap;
import org.codehaus.wadi.core.assembler.StackContext;
import org.codehaus.wadi.core.contextualiser.Contextualiser;
import org.codehaus.wadi.core.contextualiser.SharedStoreContextualiser;
import org.codehaus.wadi.core.manager.Manager;
import org.codehaus.wadi.core.session.Session;
import org.codehaus.wadi.group.vm.VMBroker;
import org.codehaus.wadi.group.vm.VMDispatcher;
import org.codehaus.wadi.servicespace.ServiceSpace;
import org.codehaus.wadi.servicespace.ServiceSpaceName;

/**
 * 
 * @version $Revision: 1538 $
 */
public class SharedStoreIntegrationTest extends TestCase {

    public void testSharedStoreIntegration() throws Exception {
        VMBroker cluster = new VMBroker("TEST");
        cluster.start();

        EmbeddedDataSource dataSource = new EmbeddedDataSource();
        dataSource.setCreateDatabase("create");
        dataSource.setDatabaseName("target/derby/SharedDataStoreDB");
        final Store store = new DatabaseStore(dataSource, "session_table", true, false);

        VMDispatcher dispatcher = new VMDispatcher(cluster, "red", null, 5000);
        dispatcher.start();
        
        StackContext stackContext = new StackContext(new ServiceSpaceName(new URI("Space")), dispatcher) {
            protected Contextualiser newSharedStoreContextualiser(Contextualiser next) {
                return new SharedStoreContextualiser(next, false, store);
            }
            protected Contextualiser newReplicaAwareContextualiser(Contextualiser next) {
                return next;
            }
        };
        stackContext.build();
        ServiceSpace serviceSpace = stackContext.getServiceSpace();
        serviceSpace.start();
        
        Manager manager = stackContext.getManager();
        String name = "name";
        Session session = manager.createWithName(name);
        String key = "key";
        String value = "value";
        session.addState(key, value);
        serviceSpace.stop();

        ConcurrentMotableMap memoryMap = stackContext.getMemoryMap();
        Session memorySession = (Session) memoryMap.acquire(name);
        assertNull(memorySession);
        
        serviceSpace.start();
        memorySession = (Session) memoryMap.acquire(name);
        assertNotNull(memorySession);
        String state =(String) memorySession.getState(key);
        assertEquals(value, state);
    }
    
}
