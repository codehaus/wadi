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
import java.util.ArrayList;

import org.codehaus.wadi.dindex.Partition;

public class PartitionKeys implements Serializable {

    protected int[] _keys;

    public PartitionKeys(PartitionFacade[] partitions) {
        ArrayList list=new ArrayList(partitions.length);
        for (int i=0; i<partitions.length; i++) {
            Partition partition=partitions[i];
            if (partition.isLocal())
                list.add(new Integer(partition.getKey()));
        }
        _keys=new int[list.size()];
        for (int i=0; i<_keys.length; i++)
            _keys[i]=((Integer)list.get(i)).intValue();
    }

    protected PartitionKeys() {
        // for deserialisation...
    }

    public boolean equals(Object obj) {
        if (obj==this)
            return true;

        if (! (obj instanceof PartitionKeys))
            return false;

        PartitionKeys that=(PartitionKeys)obj;

        if (this._keys.length!=that._keys.length)
            return false;

        for (int i=0; i<_keys.length; i++)
            if (this._keys[i]!=that._keys[i])
                return false;

        return true;
    }

    public String toString() {
        StringBuffer buffer=new StringBuffer();
        buffer.append("{");
        for (int i=0;i<_keys.length; i++) {
            if (i!=0)
                buffer.append(",");
            buffer.append(_keys[i]);
        }
        buffer.append("}");
        return buffer.toString();
    }

    public int size() {
        return _keys.length;
    }

    public int[] getKeys() {
        return _keys;
    }

}
