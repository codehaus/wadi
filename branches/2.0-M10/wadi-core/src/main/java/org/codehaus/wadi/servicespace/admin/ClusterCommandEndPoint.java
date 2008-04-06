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

import org.codehaus.wadi.servicespace.InvocationMetaData;
import org.codehaus.wadi.servicespace.ServiceProxy;
import org.codehaus.wadi.servicespace.ServiceProxyFactory;
import org.codehaus.wadi.servicespace.ServiceSpace;

/**
 * 
 * @version $Revision: 1538 $
 */
public class ClusterCommandEndPoint implements CommandEndPoint {
    private final ServiceSpace serviceSpace;
    private ServiceProxyFactory serviceProxyFactory;
    
    public ClusterCommandEndPoint(ServiceSpace serviceSpace) {
        if (null == serviceSpace) {
            throw new IllegalArgumentException("serviceSpace is required");
        }
        this.serviceSpace = serviceSpace;
    }

    public Object execute(Command command) {
        ServiceProxy proxy = serviceProxyFactory.getProxy();
        proxy.getInvocationMetaData().setInvocationResultCombiner(command.getInvocationResultCombiner());
        CommandEndPoint commandEndPointProxy = (CommandEndPoint) proxy;
        return commandEndPointProxy.execute(command);
    }
    
    public void start() throws Exception {
        serviceProxyFactory = serviceSpace.getServiceProxyFactory(CommandEndPoint.NAME,
                                new Class[] { CommandEndPoint.class });
        InvocationMetaData invocationMetaData = serviceProxyFactory.getInvocationMetaData();
        invocationMetaData.setClusterAggregation(true);
    }

    public void stop() throws Exception {
        serviceProxyFactory = null;
    }
    
}
