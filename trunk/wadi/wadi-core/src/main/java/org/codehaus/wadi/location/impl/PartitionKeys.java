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
package org.codehaus.wadi.location.impl;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import org.codehaus.wadi.impl.Utils;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision:1815 $
 */
public class PartitionKeys implements Serializable {

    private transient Set _keys;

    public PartitionKeys(Set keys) {
        assert (keys!=null);
        _keys=new TreeSet();
        _keys.addAll(keys);
    }

    // 'java.lang.Object' API
    
    public boolean equals(Object obj) {
        return _keys.equals(((PartitionKeys)obj)._keys);
    }

    public String toString() {
        return "<"+Utils.basename(getClass())+":"+_keys+">";
    }

    // 'java.io.Serializable' hooks
    
    private void writeObject(ObjectOutputStream os) throws IOException {
        os.write(_keys.size());
        for (Iterator i=_keys.iterator(); i.hasNext(); )
            os.write(((Integer)i.next()).intValue());
    }
    
    private void readObject(ObjectInputStream is) throws IOException, ClassNotFoundException {
        _keys=new TreeSet();
        int size=is.read();
        for (int i=0; i<size; i++)
            _keys.add(new Integer(is.read()));        
    }

    // 'PartitionKeys' API
    
    public int size() {
        return _keys.size();
    }
    
    public Set getKeys() {
        return _keys;
    }

}
