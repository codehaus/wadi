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
package org.codehaus.wadi.location.partition;

import java.io.Serializable;

import org.codehaus.wadi.PartitionResponseMessage;

/**
 * Sent from one peer to another, confirming successful acceptance of ownership of a number of Partitions.
 * 
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision:1952 $
 */
public class PartitionTransferResponse implements PartitionResponseMessage, Serializable {

    protected boolean _success;

    public PartitionTransferResponse(boolean success) {
        _success=success;
    }

    protected PartitionTransferResponse() {
        // used during deserialisation...
    }

    public boolean isSuccess() {
        return _success;
    }

    public String toString() {
        return "<PartitionTransferResponse: "+_success+">";
    }

}
