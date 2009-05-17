/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.codehaus.wadi.servicespace.admin.commands;

import java.util.Set;

import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.servicespace.ServiceSpace;
import org.codehaus.wadi.servicespace.ServiceSpaceName;
import org.codehaus.wadi.servicespace.basic.ServiceSpaceRegistry;

/**
 * 
 * @version $Revision: 1538 $
 */
public class CountingServiceSpaceEnvelopeCommand extends AbstractCountingCommand {
    private final ServiceSpaceName name;
    
    public CountingServiceSpaceEnvelopeCommand(ServiceSpaceName name) {
        if (null == name) {
            throw new IllegalArgumentException("name is required");
        }
        this.name = name;
    }

    @Override
    protected Dispatcher getDispatcher(Dispatcher underlyingDispatcher,
        LocalPeer localPeer,
        ServiceSpaceRegistry registry) {
        Set<ServiceSpace> serviceSpaces = registry.getServiceSpaces();

        ServiceSpace serviceSpace = null;
        for (ServiceSpace tmpServiceSpace : serviceSpaces) {
            if (name.equals(tmpServiceSpace.getServiceSpaceName())) {
                serviceSpace = tmpServiceSpace;
                break;
            }
        }
        if (null == serviceSpace) {
            return null;
        }
        
        return serviceSpace.getDispatcher();
    }
    
}
