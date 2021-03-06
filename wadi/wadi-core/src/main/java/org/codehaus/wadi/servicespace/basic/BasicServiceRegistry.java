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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.core.Lifecycle;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.servicespace.ServiceAlreadyRegisteredException;
import org.codehaus.wadi.servicespace.ServiceHolder;
import org.codehaus.wadi.servicespace.ServiceName;
import org.codehaus.wadi.servicespace.ServiceNotAvailableException;
import org.codehaus.wadi.servicespace.ServiceNotFoundException;
import org.codehaus.wadi.servicespace.ServiceSpace;
import org.codehaus.wadi.servicespace.SingletonServiceHolder;

/**
 * 
 * @version $Revision: $
 */
public class BasicServiceRegistry implements StartableServiceRegistry {
    private static final Log log = LogFactory.getLog(BasicServiceRegistry.class);
    
    private final ServiceSpace serviceSpace;
    private final Dispatcher dispatcher;
    private final LinkedHashMap<ServiceName, ServiceHolder> nameToServiceHolder;
    private final ServiceQueryEndpoint queryEndpoint;
    private volatile boolean started;

    public BasicServiceRegistry(ServiceSpace serviceSpace) {
        if (null == serviceSpace) {
            throw new IllegalArgumentException("serviceSpace is required");
        }
        this.serviceSpace = serviceSpace;
        
        dispatcher = serviceSpace.getDispatcher();
        nameToServiceHolder = new LinkedHashMap<ServiceName, ServiceHolder>();
        queryEndpoint = new ServiceQueryEndpoint(this, serviceSpace);
    }
    
    public List<ServiceName> getServiceNames() {
        synchronized (nameToServiceHolder) {
            return new ArrayList<ServiceName>(nameToServiceHolder.keySet());
        }
    }
    
    public Object getStartedService(ServiceName name) throws ServiceNotFoundException, ServiceNotAvailableException {
        ServiceHolder serviceHolder;
        synchronized (nameToServiceHolder) {
            serviceHolder = nameToServiceHolder.get(name);
        }
        if (null == serviceHolder) {
            throw new ServiceNotFoundException(name);
        } else if (!serviceHolder.isStarted()) {
            throw new ServiceNotAvailableException(name);
        }
        return serviceHolder.getService();
    }
    
    public boolean isServiceStarted(ServiceName name) {
        ServiceHolder serviceHolder;
        synchronized (nameToServiceHolder) {
            serviceHolder = nameToServiceHolder.get(name);
        }
        if (null == serviceHolder) {
            return false;
        }
        return serviceHolder.isStarted();
    }

    public void register(ServiceName name, Object service) throws ServiceAlreadyRegisteredException {
        if (null == name) {
            throw new IllegalArgumentException("name is required");
        } else if (started) {
            throw new IllegalStateException("ServiceRegistry is started. Cannot register service");
        }
        synchronized (nameToServiceHolder) {
            if (nameToServiceHolder.containsKey(name)) {
                throw new ServiceAlreadyRegisteredException(name);
            }
            nameToServiceHolder.put(name, new BasicServiceHolder(serviceSpace, name, service));
        }
    }

    public SingletonServiceHolder registerSingleton(ServiceName name, Object service) throws ServiceAlreadyRegisteredException {
        if (null == name) {
            throw new IllegalArgumentException("name is required");
        } else if (started) {
            throw new IllegalStateException("ServiceRegistry is started. Cannot register service");
        } else if (nameToServiceHolder.containsKey(name)) {
            throw new ServiceAlreadyRegisteredException(name);
        }
        BasicSingletonServiceHolder serviceHolder = new BasicSingletonServiceHolder(serviceSpace, name, service);
        synchronized (nameToServiceHolder) {
            if (nameToServiceHolder.containsKey(name)) {
                throw new ServiceAlreadyRegisteredException(name);
            }
            nameToServiceHolder.put(name, serviceHolder);
        }
        return serviceHolder;
    }
    
    public void unregister(ServiceName name) {
        if (null == name) {
            throw new IllegalArgumentException("name is required");
        } else if (started) {
            throw new IllegalStateException("ServiceRegistry is started. Cannot unregister service");
        }
        Object removed;
        synchronized (nameToServiceHolder) {
            removed = nameToServiceHolder.remove(name);
        }
        if (null == removed) {
            throw new IllegalArgumentException("No service named [" + name + "]");
        }
        
    }

    public void start() throws Exception {
        started = true;
        
        dispatcher.register(queryEndpoint);
        
        Collection<ServiceHolder> services;
        synchronized (nameToServiceHolder) {
            services = new ArrayList<ServiceHolder>(nameToServiceHolder.values());
        }
        Collection<ServiceHolder> startedServices = new ArrayList<ServiceHolder>();
        for (Iterator<ServiceHolder> iter = services.iterator(); iter.hasNext();) {
            ServiceHolder serviceHolder = iter.next();
            try {
                serviceHolder.start();
                startedServices.add(serviceHolder);
            } catch (Exception e) {
                log.error("Error while starting [" + serviceHolder + "]", e);
                stopServices(startedServices);
                throw e;
            }
        }
    }

    public void stop() throws Exception {
        started = false;
        
        dispatcher.unregister(queryEndpoint, 10, 500);

        List services;
        synchronized (nameToServiceHolder) {
            services = new ArrayList(nameToServiceHolder.values());
        }
        Collections.reverse(services);
        stopServices(services);
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
