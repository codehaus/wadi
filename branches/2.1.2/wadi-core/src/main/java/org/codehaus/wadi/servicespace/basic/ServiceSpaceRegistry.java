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

import java.util.Set;

import org.codehaus.wadi.servicespace.ServiceSpace;
import org.codehaus.wadi.servicespace.ServiceSpaceAlreadyRegisteredException;
import org.codehaus.wadi.servicespace.ServiceSpaceName;
import org.codehaus.wadi.servicespace.ServiceSpaceNotFoundException;



/**
 * 
 * @version $Revision: 1538 $
 */
public interface ServiceSpaceRegistry {
    void register(ServiceSpace serviceSpace) throws ServiceSpaceAlreadyRegisteredException;
    
    void unregister(ServiceSpace serviceSpace) throws ServiceSpaceNotFoundException;

    ServiceSpace getServiceSpace(ServiceSpaceName serviceSpaceName) throws ServiceSpaceNotFoundException;

    Set<ServiceSpace> getServiceSpaces();
}