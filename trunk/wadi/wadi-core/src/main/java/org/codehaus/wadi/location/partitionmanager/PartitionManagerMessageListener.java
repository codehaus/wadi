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
package org.codehaus.wadi.location.partitionmanager;

import org.codehaus.wadi.group.Envelope;
import org.codehaus.wadi.location.balancing.PartitionBalancingInfoUpdate;
import org.codehaus.wadi.location.balancing.RetrieveBalancingInfoEvent;
import org.codehaus.wadi.location.partition.PartitionEvacuationRequest;
import org.codehaus.wadi.location.partition.PartitionTransferRequest;

/**
 * 
 * @version $Revision: 1603 $
 */
public interface PartitionManagerMessageListener {
    void onPartitionEvacuationRequest(Envelope om, PartitionEvacuationRequest request);

    void onPartitionTransferRequest(Envelope om, PartitionTransferRequest request);
    
    void onRetrieveBalancingInfoEvent(Envelope om, RetrieveBalancingInfoEvent infoEvent);
    
    void onPartitionBalancingInfoUpdate(Envelope om, PartitionBalancingInfoUpdate infoUpdate);
}