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
package org.codehaus.wadi.sandbox.distributable.impl;

import java.util.Set;

import org.codehaus.wadi.StreamingStrategy;
import org.codehaus.wadi.sandbox.Attributes;
import org.codehaus.wadi.sandbox.distributable.Dirtier;
import org.codehaus.wadi.sandbox.impl.Utils;


public class WholeAttributesWrapper implements Attributes {
    
    protected final Dirtier _dirtier;
    protected final StreamingStrategy _streamer;
    protected final boolean _evictObjectRepASAP;
    protected final boolean _evictByteRepASAP;
    
    protected Attributes _objectRep;
    protected byte[] _byteRep;
    
    public WholeAttributesWrapper(Attributes attributes, Dirtier dirtier, StreamingStrategy streamer, boolean evictObjectRepASAP, boolean evictByteRepASAP) {
        _dirtier=dirtier;
        _streamer=streamer;
        _evictObjectRepASAP=evictObjectRepASAP;
        _evictByteRepASAP=evictByteRepASAP;

        _objectRep=attributes;
        _byteRep=null;
    }
    
    protected synchronized Attributes getObjectRep() {
        if (null==_objectRep) {
            // convert byte[] to Object rep
            _objectRep=(Attributes)Utils.safeByteArrayToObject(_byteRep, _streamer);
            if (_evictByteRepASAP) _byteRep=null;
        }
        return _objectRep;
    }
    
    synchronized byte[] getByteRep() {
        if (null==_byteRep) {
            // convert Object to byte[] rep
            _byteRep=Utils.safeObjectToByteArray(_objectRep, _streamer);
            if (_evictObjectRepASAP) _objectRep=null;
        }
        return _byteRep;
    }
    
    public Object get(Object key) {
        Object tmp=getObjectRep().get(key);
        if (tmp!=null && _dirtier.readAccess()) _byteRep=null;
        return tmp;
    }
    
    public Object remove(Object key) {
        Object tmp=getObjectRep().remove(key);
        if (tmp!=null && _dirtier.writeAccess()) _byteRep=null;
        return tmp;
    }
    
    public Object put(Object key, Object value) {
        Object tmp=getObjectRep().put(key, value);
        if (_dirtier.writeAccess()) _byteRep=null;
        return tmp;
    }
    
    public int size() {
        return getObjectRep().size();
    }
    
    public Set keySet() {
        return getObjectRep().keySet();
    }
    
    public byte[] getBytes() {
        return getByteRep();
    }
    
    public synchronized void setBytes(byte[] bytes) {
        _objectRep=null;
        _byteRep=bytes;
    }
    
    public synchronized void clear() {
        // _objectRep.clear(); // later, keep container and just chuck contents...
        _objectRep=null;
        _byteRep=null;
    }
}