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
package org.codehaus.wadi.servicespace.admin;

import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.servicespace.basic.ServiceSpaceRegistry;
import org.codehaus.wadi.servicespace.basic.ServiceSpaceRegistryFactory;


/**
 * 
 * @version $Revision: 1538 $
 */
public class AdminServiceSpaceHelper {
    
    public AdminServiceSpace getAdminServiceSpace(Dispatcher dispatcher) {
        ServiceSpaceRegistryFactory factory = newServiceSpaceRegistryFactory();
        ServiceSpaceRegistry registry = factory.getRegistryFor(dispatcher);
        return (AdminServiceSpace) registry.getServiceSpace(AdminServiceSpace.NAME);
    }
    
    protected ServiceSpaceRegistryFactory newServiceSpaceRegistryFactory() {
        return new ServiceSpaceRegistryFactory();
    }

}
