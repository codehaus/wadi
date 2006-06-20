/**
 *
 * Copyright 2003-2005 Core Developers Network Ltd.
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
package org.codehaus.wadi.location.messages;

import java.io.Serializable;

import org.codehaus.wadi.location.impl.PartitionTransfer;

/**
 * Sent from Coordinator to a Peer, ordering it to transfer ownership of numbers of Partitions to other Peers.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision:1815 $
 */
public class PartitionTransferCommand implements Serializable {

  protected PartitionTransfer[] _transfers;

  public PartitionTransferCommand(PartitionTransfer[] transfers) {
    _transfers=transfers;
  }

  public PartitionTransfer[] getTransfers() {
    return _transfers;
  }

  public String toString() {
    StringBuffer buffer=new StringBuffer("<PartitionTransferCommand: ");
    for (int i=0; i<_transfers.length; i++)
      buffer.append((i==0?"":",")+_transfers[i]);
    buffer.append(">");
    return buffer.toString();
  }

}
