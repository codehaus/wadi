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
package org.codehaus.wadi.test;

import java.net.URI;

import org.codehaus.wadi.Router;
import org.codehaus.wadi.core.manager.ClusteredManager;
import org.codehaus.wadi.core.session.DistributableAttributesFactory;
import org.codehaus.wadi.core.session.SessionFactory;
import org.codehaus.wadi.core.session.ValueFactory;
import org.codehaus.wadi.core.session.ValueHelperRegistry;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.impl.SimpleStreamer;
import org.codehaus.wadi.impl.StackContext;
import org.codehaus.wadi.replication.manager.ReplicaterAdapterFactory;
import org.codehaus.wadi.servicespace.ServiceSpace;
import org.codehaus.wadi.servicespace.ServiceSpaceName;
import org.codehaus.wadi.web.BasicWebSessionFactory;
import org.codehaus.wadi.web.impl.DummyRouter;
import org.codehaus.wadi.web.impl.StandardSessionWrapperFactory;

public class MyStack {
    private ClusteredManager _manager;
    private ServiceSpace serviceSpace;

    public MyStack(Dispatcher dispatcher) throws Exception {
        dispatcher.start();
        // Implementation note: we really need to wait some time to have a "stable" Dispatcher. For instance, in the
        // case of ActiveCluster, 
        Thread.sleep(1000);

        StackContext stackContext = new StackContext(new ServiceSpaceName(new URI("Space")), dispatcher) {
            protected Router newRouter() {
                return new DummyRouter();
            }
            
            protected SessionFactory newSessionFactory() {
                ValueHelperRegistry valueHelperRegistry = newValueHelperRegistry();
                ValueFactory valueFactory = newValueFactory(valueHelperRegistry);
                SimpleStreamer streamer = newStreamer();
                return new BasicWebSessionFactory(new DistributableAttributesFactory(valueFactory),
                        streamer,
                        new ReplicaterAdapterFactory(replicationManager),
                        router,
                        new StandardSessionWrapperFactory());
            }
        };
        stackContext.build();
        
        serviceSpace = stackContext.getServiceSpace();
        _manager = stackContext.getManager();
    }

    public void start() throws Exception {
        serviceSpace.start();
    }

    public void stop() throws Exception {
        serviceSpace.stop();
    }

    public ClusteredManager getManager() {
        return _manager;
    }

    public ServiceSpace getServiceSpace() {
        return serviceSpace;
    }

}
