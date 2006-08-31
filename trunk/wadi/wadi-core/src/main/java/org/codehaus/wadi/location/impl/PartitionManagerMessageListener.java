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
package org.codehaus.wadi.location.impl;

import org.codehaus.wadi.group.Message;
import org.codehaus.wadi.location.partition.PartitionEvacuationRequest;
import org.codehaus.wadi.location.partition.PartitionRepopulateRequest;
import org.codehaus.wadi.location.partition.PartitionTransferRequest;
import org.codehaus.wadi.partition.PartitionBalancingInfoUpdate;
import org.codehaus.wadi.partition.RetrieveBalancingInfoEvent;

/**
 * 
 * @version $Revision: 1603 $
 */
public interface PartitionManagerMessageListener {
    // a node wants to shutdown...
    void onPartitionEvacuationRequest(Message om, PartitionEvacuationRequest request);

    // a node wants to rebuild a lost partition
    void onPartitionRepopulateRequest(Message om, PartitionRepopulateRequest request);

    // receive a transfer of partitions
    void onPartitionTransferRequest(Message om, PartitionTransferRequest request);
    
    void onPartitionManagerJoiningEvent(Message om, PartitionManagerJoiningEvent joiningEvent);
    
    void onRetrieveBalancingInfoEvent(Message om, RetrieveBalancingInfoEvent infoEvent);
    
    void onPartitionBalancingInfoUpdate(Message om, PartitionBalancingInfoUpdate infoUpdate);
}