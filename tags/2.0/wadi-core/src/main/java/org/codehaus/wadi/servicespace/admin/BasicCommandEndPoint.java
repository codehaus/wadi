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
import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.servicespace.ServiceSpace;
import org.codehaus.wadi.servicespace.basic.ServiceSpaceRegistry;

/**
 * 
 * @version $Revision: 1538 $
 */
public class BasicCommandEndPoint implements CommandEndPoint {
    private final Dispatcher underlyingDispatcher;
    private final ServiceSpaceRegistry serviceSpaceRegistry;
    private final LocalPeer localPeer;
    
    public BasicCommandEndPoint(Dispatcher underlyingDispatcher,
        ServiceSpace serviceSpace,
        ServiceSpaceRegistry serviceSpaceRegistry) {
        if (null == underlyingDispatcher) {
            throw new IllegalArgumentException("underlyingDispatcher is required");
        } else if (null == serviceSpace) {
            throw new IllegalArgumentException("serviceSpace is required");
        } else if (null == serviceSpaceRegistry) {
            throw new IllegalArgumentException("serviceSpaceRegistry is required");
        }
        this.underlyingDispatcher = underlyingDispatcher;
        this.serviceSpaceRegistry = serviceSpaceRegistry;
        
        localPeer = serviceSpace.getLocalPeer();
    }

    public Object execute(Command command) {
        return command.execute(underlyingDispatcher, localPeer, serviceSpaceRegistry);
    }

    public void start() throws Exception {
    }

    public void stop() throws Exception {
    }

}
