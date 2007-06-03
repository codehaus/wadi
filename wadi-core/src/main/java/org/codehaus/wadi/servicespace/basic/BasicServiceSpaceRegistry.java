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
package org.codehaus.wadi.servicespace.basic;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.codehaus.wadi.servicespace.ServiceSpace;
import org.codehaus.wadi.servicespace.ServiceSpaceAlreadyRegisteredException;
import org.codehaus.wadi.servicespace.ServiceSpaceName;
import org.codehaus.wadi.servicespace.ServiceSpaceNotFoundException;


/**
 * 
 * @version $Revision: 1538 $
 */
public class BasicServiceSpaceRegistry implements ServiceSpaceRegistry {
    private final Map nameToServiceSpace;
    
    public BasicServiceSpaceRegistry() {
        nameToServiceSpace = new HashMap();
    }

    public void register(ServiceSpace serviceSpace) throws ServiceSpaceAlreadyRegisteredException {
        synchronized (nameToServiceSpace) {
            ServiceSpaceName serviceSpaceName = serviceSpace.getServiceSpaceName();
            if (null != nameToServiceSpace.get(serviceSpaceName)) {
                throw new ServiceSpaceAlreadyRegisteredException(serviceSpaceName);
            }
            nameToServiceSpace.put(serviceSpaceName, serviceSpace);
        }
    }

    public void unregister(ServiceSpace serviceSpace) throws ServiceSpaceNotFoundException {
        synchronized (nameToServiceSpace) {
            ServiceSpaceName serviceSpaceName = serviceSpace.getServiceSpaceName();
            Object object = nameToServiceSpace.remove(serviceSpaceName);
            if (null == object) {
                throw new ServiceSpaceNotFoundException(serviceSpaceName);
            }
        }
    }

    public Set getServiceSpaces() {
        synchronized (nameToServiceSpace) {
            return new HashSet(nameToServiceSpace.values());
        }
    }
    
}