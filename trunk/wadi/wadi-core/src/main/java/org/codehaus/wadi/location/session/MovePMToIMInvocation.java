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
package org.codehaus.wadi.location.session;

import java.io.Serializable;
import org.codehaus.wadi.Lease;
import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.location.SessionResponseMessage;

/**
 * Response from PartitionMaster to InvocationMaster, indicating that it should relocate its Invocation to the given Address
 * within the specified time, during which a Lease with the given Handle is in place.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision:1815 $
 */
public class MovePMToIMInvocation implements SessionResponseMessage, Serializable {

    protected Lease.Handle _leaseHandle;
    protected long _leasePeriod;
    protected Address _stateMaster;
    
    public MovePMToIMInvocation(Lease.Handle leaseHandle, long leasePeriod, Address stateMaster) {
        _leaseHandle=leaseHandle;
        _leasePeriod=leasePeriod;
        _stateMaster=stateMaster;
    }
    
    public String toString() {
        return "<MovePMToIMInvocation:"+_stateMaster+">";
    }
    
    public Address getStateMaster() {
        return _stateMaster;
    }
    
    public Lease.Handle getLeaseHandle() {
        return _leaseHandle;
    }
    
    public long getLeasePeriod() {
        return _leasePeriod;
    }
    
}
