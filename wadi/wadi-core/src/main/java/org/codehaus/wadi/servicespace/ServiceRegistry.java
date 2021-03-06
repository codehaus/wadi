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
package org.codehaus.wadi.servicespace;

import java.util.List;


/**
 * 
 * @version $Revision: 1538 $
 */
public interface ServiceRegistry {
    void register(ServiceName name, Object service) throws ServiceAlreadyRegisteredException;

    SingletonServiceHolder registerSingleton(ServiceName name, Object service) throws ServiceAlreadyRegisteredException;

    void unregister(ServiceName name) throws ServiceNotFoundException;
    
    List<ServiceName> getServiceNames();
    
    Object getStartedService(ServiceName name) throws ServiceNotFoundException, ServiceNotAvailableException;

    boolean isServiceStarted(ServiceName serviceName);
}
