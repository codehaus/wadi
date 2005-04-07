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
package org.codehaus.wadi.sandbox.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.StreamingStrategy;
import org.codehaus.wadi.sandbox.Attributes;
import org.codehaus.wadi.sandbox.Dirtier;

// TODO - consider mode where rep is shifted from byte->Object->byte for the duration of any change
// this would be useful for testing that apps were distributable

public class WholeAttributes implements Attributes {
	protected static final Log _log = LogFactory.getLog(WholeAttributes.class);
    
    protected final Dirtier _dirtier;
    protected final StreamingStrategy _streamer;
    protected final boolean _evictObjectRepASAP;
    protected final boolean _evictByteRepASAP;
    
    protected final Map _objectRep;
    protected byte[] _byteRep;

    protected boolean _objectRepValid;
    protected boolean _hasListeners;
    
    public WholeAttributes(Dirtier dirtier, StreamingStrategy streamer, boolean evictObjectRepASAP, boolean evictByteRepASAP) {
        _dirtier=dirtier;
        _streamer=streamer;
        _evictObjectRepASAP=evictObjectRepASAP;
        _evictByteRepASAP=evictByteRepASAP;
        
        _objectRep=new HashMap();
        _objectRepValid=true;
        _byteRep=null;
    }
    
    protected synchronized Map getObjectRep() {
        if (!_objectRepValid) {
            List activationListeners=new ArrayList();
            // convert byte[] to Object rep
            try {
                ByteArrayInputStream bais=new ByteArrayInputStream(_byteRep);
                ObjectInput oi=_streamer.getInputStream(bais); // TODO - ClassLoading ?
                // read number of attributes - there doesn't seem to be a way to presize an existing Map :-(
                int size=oi.readInt();
                // for each attribute:
                for (int i=0; i<size; i++) {
                    //  read key
                    Object key=oi.readObject();
                    //  read val
                    Object val=oi.readObject();
                    //  if it is an activation listener, call didActivate() on it
                    if (val instanceof HttpSessionActivationListener) {
                        activationListeners.add(val);
                    }
                    //  if it is the wrapper class - use this to get the real object
                    // TODO - use wrapper to reincarnate val
                    _objectRep.put(key, val);
                }
                oi.close();
                _objectRepValid=true;
                if (_evictByteRepASAP) _byteRep=null;
                // call activationListeners, now that we have a complete session...
                int l=activationListeners.size();
                for (int i=0; i<l; i++) 
                    ((HttpSessionActivationListener)activationListeners.get(i)).sessionDidActivate(_event);
            } catch (Exception e) {
                _log.error("unexpected problem converting byte[] to Attributes", e);
            }
        }
        return _objectRep;
    }
    
    synchronized byte[] getByteRep() {
        if (null==_byteRep) {
            // convert Object to byte[] rep
            try {
                ByteArrayOutputStream baos=new ByteArrayOutputStream();
                ObjectOutput oo=_streamer.getOutputStream(baos);
                // write the number of attributes
                oo.writeInt(_objectRep.size());
                // for each attribute :
                for (Iterator i=_objectRep.entrySet().iterator(); i.hasNext();) {
                    Map.Entry e=(Map.Entry)i.next();
                    //  write it's key
                    oo.writeObject(e.getKey());
                    //  if it is an activation listener, call willPassivate()
                    Object val=e.getValue();
                    //  if it is either an Activation or Binding listener, set a flag
                    if (val instanceof HttpSessionActivationListener) {
                        ((HttpSessionActivationListener)val).sessionWillPassivate(_event);
                        _hasListeners=true; // the whole session will need deserialising if it times out on e.g. disc...
                    }
                    //  if it is a known non-serialisable type, do the right thing with a serialisable wrapper class
                    // TODO - use a configurable table of wrapper types...
                    //  write it out
                    oo.writeObject(val);
                }
                // don't write the flag - this will form part of our containing session's metadata...
                // extract resulting byte[]...
                oo.close();
                _byteRep=baos.toByteArray();
                if (_evictObjectRepASAP) {
                    _objectRep.clear();
                    _objectRepValid=false;
                }
            } catch (Exception e) {
                _log.error("unexpected problem converting Attributes to byte[]", e);
            }
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
        if (_dirtier.writeAccess()) _byteRep=null; // no need to check for null value - this would become a remove()
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
        _objectRep.clear();
        _objectRepValid=false;
        _byteRep=bytes;
    }
    
    public synchronized void clear() {
        _objectRep.clear();
        _objectRepValid=false;
        _byteRep=null;
    }
    
    protected HttpSessionEvent _event;
    public void setHttpSessionEvent(HttpSessionEvent event) {_event=event;}
    
    // FIXME
    public Set getBindingListenerNames() {return null;}
    public Set getActivationListenerNames() {return null;}

}