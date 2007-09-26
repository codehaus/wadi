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

import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.location.partitionmanager.PartitionManager;
import org.codehaus.wadi.servicespace.ServiceSpace;
import org.codehaus.wadi.servicespace.ServiceSpaceName;

/**
 * 
 * @version $Revision: 1538 $
 */
public class GetPartitionBalancingInfos extends AbstractServiceCommand {
    
    public GetPartitionBalancingInfos(ServiceSpaceName name) {
        super(name, PartitionManager.NAME);
    }

    protected Object execute(LocalPeer localPeer, ServiceSpace serviceSpace, Object service) {
        return ((PartitionManager) service).getBalancingInfo();
    }

}
