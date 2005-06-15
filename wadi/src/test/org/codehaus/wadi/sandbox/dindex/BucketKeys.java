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
package org.codehaus.wadi.sandbox.dindex;

import java.io.Serializable;
import java.util.ArrayList;

public class BucketKeys implements Serializable {
    
    protected int[] _keys;
    
    public BucketKeys(BucketFacade[] buckets) {
        ArrayList list=new ArrayList(buckets.length);
        synchronized (buckets) {
            for (int i=0; i<buckets.length; i++) {
                Bucket bucket=buckets[i];
                if (bucket.isLocal())
                    list.add(new Integer(bucket.getKey()));
            }
        }
        _keys=new int[list.size()];
        for (int i=0; i<_keys.length; i++)
            _keys[i]=((Integer)list.get(i)).intValue();
    }
    
    protected BucketKeys() {
        // for deserialisation...
    }
    
    public boolean equals(Object obj) {
        if (obj==this)
            return true;
        
        if (! (obj instanceof BucketKeys))
            return false;
        
        BucketKeys that=(BucketKeys)obj;
        
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
    
}
