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
package org.codehaus.wadi.servicespace.basic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.Lifecycle;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.servicespace.ServiceName;
import org.codehaus.wadi.servicespace.ServiceSpace;

/**
 * 
 * @version $Revision: $
 */
public class BasicServiceRegistry implements StartableServiceRegistry {
    private static final Log log = LogFactory.getLog(BasicServiceRegistry.class);
    
    private final ServiceSpace serviceSpace;
    private final Dispatcher dispatcher;
    private final LinkedHashMap nameToServiceHolder;
    private final ServiceQueryEndpoint queryEndpoint;
    private boolean started;

    public BasicServiceRegistry(ServiceSpace serviceSpace) {
        if (null == serviceSpace) {
            throw new IllegalArgumentException("serviceSpace is required");
        }
        this.serviceSpace = serviceSpace;
        
        dispatcher = serviceSpace.getDispatcher();
        nameToServiceHolder = new LinkedHashMap();
        queryEndpoint = new ServiceQueryEndpoint(this, serviceSpace);
    }
    
    public synchronized Set getServiceNames() {
        synchronized (nameToServiceHolder) {
            return new HashSet(nameToServiceHolder.keySet());
        }
    }

    public synchronized void register(ServiceName name, Lifecycle service) {
        if (null == name) {
            throw new IllegalArgumentException("name is required");
        } else if (nameToServiceHolder.containsKey(name)) {
            throw new IllegalArgumentException("A service has already been bound to [" + name + "]");
        } else if (started) {
            throw new IllegalStateException("ServiceRegistry is started. Cannot register service");
        }
        nameToServiceHolder.put(name, new BasicServiceHolder(serviceSpace, name, service));
    }

    public synchronized void unregister(ServiceName name) {
        if (null == name) {
            throw new IllegalArgumentException("name is required");
        } else if (started) {
            throw new IllegalStateException("ServiceRegistry is started. Cannot unregister service");
        }
        Object removed = nameToServiceHolder.remove(name);
        if (null == removed) {
            throw new IllegalArgumentException("No service named [" + name + "]");
        }
        
    }

    public synchronized void start() throws Exception {
        Collection services = nameToServiceHolder.values();
        Collection startedServices = new ArrayList();
        for (Iterator iter = services.iterator(); iter.hasNext();) {
            BasicServiceHolder serviceHolder = (BasicServiceHolder) iter.next();
            try {
                serviceHolder.start();
                startedServices.add(serviceHolder);
            } catch (Exception e) {
                log.error("Error while starting [" + serviceHolder + "]", e);
                stopServices(startedServices);
                throw e;
            }
        }
        
        dispatcher.register(queryEndpoint);
        started = true;
    }

    public synchronized void stop() throws Exception {
        dispatcher.unregister(queryEndpoint, 10, 500);

        List services = new ArrayList(nameToServiceHolder.values());
        Collections.reverse(services);
        stopServices(services);
        started = false;
    }

    protected void stopServices(Collection services) {
        for (Iterator iter = services.iterator(); iter.hasNext();) {
            Lifecycle service = (Lifecycle) iter.next();
            try {
                service.stop();
            } catch (Exception e) {
                log.error("Error while stopping [" + services + "]", e);
            }
        }
    }

}
