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
package org.codehaus.wadi.servicespace.admin.commands;

import java.util.Set;

import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.servicespace.InvocationResultCombiner;
import org.codehaus.wadi.servicespace.ServiceName;
import org.codehaus.wadi.servicespace.ServiceNotAvailableException;
import org.codehaus.wadi.servicespace.ServiceNotFoundException;
import org.codehaus.wadi.servicespace.ServiceSpace;
import org.codehaus.wadi.servicespace.ServiceSpaceName;
import org.codehaus.wadi.servicespace.admin.Command;
import org.codehaus.wadi.servicespace.basic.ServiceSpaceRegistry;
import org.codehaus.wadi.servicespace.resultcombiner.FirstNotNullSuccessThenFailureCombiner;

/**
 * 
 * @version $Revision: 1538 $
 */
public abstract class AbstractServiceCommand implements Command {
    private final ServiceSpaceName name;
    private final ServiceName serviceName;
    
    public AbstractServiceCommand(ServiceSpaceName name, ServiceName serviceName) {
        if (null == name) {
            throw new IllegalArgumentException("name is required");
        } else if (null == serviceName) {
            throw new IllegalArgumentException("serviceName is required");
        }
        this.name = name;
        this.serviceName = serviceName;
    }

    public Object execute(Dispatcher underlyingDispatcher, LocalPeer localPeer, ServiceSpaceRegistry registry) {
        Set<ServiceSpace> serviceSpaces = registry.getServiceSpaces();
        
        for (ServiceSpace serviceSpace : serviceSpaces) {
            if (serviceSpace.getServiceSpaceName().equals(name)) {
                Object service = getService(serviceSpace);
                if (null == service) {
                    return null;
                }
                return execute(localPeer, serviceSpace, service);
            }
        }
        
        return null;
    }

    protected Object getService(ServiceSpace currServiceSpace) {
        try {
            return currServiceSpace.getServiceRegistry().getStartedService(serviceName);
        } catch (ServiceNotFoundException e) {
            return null;
        } catch (ServiceNotAvailableException e) {
            return null;
        }
    }
    
    protected abstract Object execute(LocalPeer localPeer, ServiceSpace serviceSpace, Object service);

    public InvocationResultCombiner getInvocationResultCombiner() {
        return FirstNotNullSuccessThenFailureCombiner.COMBINER;
    }
    
}
