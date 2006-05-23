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

import org.codehaus.wadi.OldMessage;
import org.codehaus.wadi.location.impl.LocalPartition;

/**
 * Sent from one peer to another, requesting that it take over ownership of a number of Partitions (enclosed).
 * 
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class PartitionTransferRequest implements OldMessage, Serializable {

	protected long _timeStamp;
	protected LocalPartition[] _partitions;

	public PartitionTransferRequest(long timeStamp, LocalPartition[] partitions) {
		_timeStamp=timeStamp;
		_partitions=partitions;
	}

	protected PartitionTransferRequest() {
		// for deserialisation
	}

	public long getTimeStamp() {
		return _timeStamp;
	}

	public LocalPartition[] getPartitions() {
		return _partitions;
	}

    public String toString() {
    	StringBuffer buffer=new StringBuffer("<PartitionTransferRequest: ");
    	for (int i=0; i<_partitions.length; i++)
    		buffer.append((i==0?"":",")+_partitions[i]);
    	buffer.append(">");
        return buffer.toString();
    }

}
