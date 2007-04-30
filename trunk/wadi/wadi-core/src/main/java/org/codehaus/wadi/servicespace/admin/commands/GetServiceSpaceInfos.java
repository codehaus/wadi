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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.servicespace.InvocationResultCombiner;
import org.codehaus.wadi.servicespace.ServiceSpace;
import org.codehaus.wadi.servicespace.ServiceSpaceInfo;
import org.codehaus.wadi.servicespace.admin.Command;
import org.codehaus.wadi.servicespace.basic.ServiceSpaceRegistry;
import org.codehaus.wadi.servicespace.resultcombiner.SuccessfullSetResultCombiner;

/**
 * 
 * @version $Revision: 1538 $
 */
public class GetServiceSpaceInfos implements Command {

    public Object execute(LocalPeer localPeer, ServiceSpaceRegistry serviceSpaceRegistry) {
        Set serviceSpaces = serviceSpaceRegistry.getServiceSpaces();
        
        Set serviceSpaceInfos = new HashSet();
        for (Iterator iter = serviceSpaces.iterator(); iter.hasNext();) {
            ServiceSpace currServiceSpace = (ServiceSpace) iter.next();
            serviceSpaceInfos.add(new ServiceSpaceInfo(localPeer, currServiceSpace.getServiceSpaceName()));
        }
        return serviceSpaceInfos;
    }
    
    public InvocationResultCombiner getInvocationResultCombiner() {
        return SuccessfullSetResultCombiner.COMBINER;
    }
    
}
