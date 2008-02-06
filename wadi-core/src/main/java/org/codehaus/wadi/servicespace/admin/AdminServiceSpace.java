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

import java.net.URI;
import java.net.URISyntaxException;

import org.codehaus.wadi.core.reflect.ClassIndexerRegistry;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.servicespace.ServiceAlreadyRegisteredException;
import org.codehaus.wadi.servicespace.ServiceSpaceName;
import org.codehaus.wadi.servicespace.basic.BasicServiceSpace;

/**
 * 
 * @version $Revision: 1538 $
 */
public class AdminServiceSpace extends BasicServiceSpace {
    public static final ServiceSpaceName NAME;
    
    static {
        ServiceSpaceName name = null;
        try {
            name = new ServiceSpaceName(new URI("WADI/Admin"));
        } catch (URISyntaxException e) {
        }
        NAME = name;
    }
    
    private final CommandEndPoint commandEndPoint;
    
    public AdminServiceSpace(Dispatcher underlyingDispatcher, ClassIndexerRegistry serviceClassIndexerRegistry) {
        super(NAME, underlyingDispatcher, serviceClassIndexerRegistry);
        
        commandEndPoint = new ClusterCommandEndPoint(this);
        
        CommandEndPoint peerCommandEndPoint = new BasicCommandEndPoint(this, getServiceSpaceRegistry());
        try {
            getServiceRegistry().register(CommandEndPoint.NAME, peerCommandEndPoint);
        } catch (ServiceAlreadyRegisteredException e) {
            throw (AssertionError) new AssertionError("Should never happen").initCause(e);
        }
    }

    public CommandEndPoint getCommandEndPoint() {
        return commandEndPoint;
    }

    public void start() throws Exception {
        super.start();
        commandEndPoint.start();
    }
    
    public void stop() throws Exception {
        commandEndPoint.stop();
        super.stop();
    }
    
}
