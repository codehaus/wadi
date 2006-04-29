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
package org.codehaus.wadi.dindex.impl;

import java.io.Serializable;

import org.codehaus.wadi.group.Address;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class PartitionTransfer implements Serializable {

    public Address _address;
    public String _name; // TODO - only here for debugging...
    public int _amount;

    public PartitionTransfer(Address address, String name, int amount) {
        _address=address;
        _name=name;
        _amount=amount;
    }

    protected PartitionTransfer() {
        // for deserialisation...
    }

    public Address getAddress() {
        return _address;
    }

    public int getAmount() {
        return _amount;
    }

    public String toString() {
        return "<transfer: "+_amount+"->"+_name+">";
    }

}
