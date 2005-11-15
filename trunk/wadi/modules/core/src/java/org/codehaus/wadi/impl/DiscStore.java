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
package org.codehaus.wadi.impl;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.DiscMotableConfig;
import org.codehaus.wadi.Store;
import org.codehaus.wadi.StoreMotable;
import org.codehaus.wadi.Streamer;

public class DiscStore implements Store, DiscMotableConfig {

    protected final Log _log=LogFactory.getLog(getClass());
    protected final Streamer _streamer;
    protected final File _dir;
    protected final boolean _useNIO;
    protected final DirectByteBufferCache _cache=new DirectByteBufferCache();
    protected final boolean _reusingStore;

    public DiscStore(Streamer streamer, File dir, boolean useNIO, boolean reusingStore) throws Exception {
        _streamer=streamer;
        _dir=dir;
        _useNIO=useNIO;
        _reusingStore=reusingStore;
        
        try {
        	File.createTempFile("DiscStore_WriteTest",null , _dir).delete();
        } catch (IOException e) {
            if ( _log.isErrorEnabled() ) {

                _log.error("bad directory: " + _dir, e);
            }
        	throw e;
        }
        	
        // TODO - for use by a SharedStoreContextualiser we need to figure out concurrency issues...
    }
    
    public void clean() {
        File[] files=_dir.listFiles();
        int l=files.length;
        for (int i=0; i<l; i++) {
            files[i].delete();
        }
        if ( _log.isInfoEnabled() ) {

            _log.info("removed (exclusive disc): " + l + " files");
        }
    }
    
    public void load(Putter putter, boolean accessOnLoad) {
        // if our last incarnation suffered a catastrophic failure there may be some sessions
        // in our directory - FIXME - if replicating, we may not want to reload these...
        long time=System.currentTimeMillis();
        String[] list=_dir.list();
        int l=list.length;
        int suffixLength=".".length()+_streamer.getSuffix().length();
        for (int i=0; i<l; i++) {
            String name=list[i];
            String id=name.substring(0, name.length()-suffixLength);
            DiscMotable motable=new DiscMotable();
            try {
                motable.init(this, id);
                if (accessOnLoad) {
                    motable.setLastAccessedTime(time);
                } else {
                    if (motable.getTimedOut(time)) {
                        if (_log.isWarnEnabled()) _log.warn("LOADED DEAD SESSION: "+motable.getName());
                        // TODO - something cleverer...
                    }
                }
                putter.put(id, motable);
            } catch (Exception e) {
                if (_log.isErrorEnabled()) _log.error("failed to load: "+name);
            }
        }
        if (_log.isInfoEnabled())_log.info("loaded (exclusive disc): "+list.length);
    }
    
    public StoreMotable create() {
        return new DiscMotable();
    }
    
    public String getStartInfo() {return _dir.toString();}
    public String getDescription() {return "exclusive disc";}
    
    // ExclusiveDiscMotableConfig
    
    public File getDirectory() {return _dir;}
    public String getSuffix() {return _streamer.getSuffixWithDot();}
    public boolean getUseNIO() {return _useNIO;}
    public ByteBuffer take(int size) {return _cache.take(size);}
    public void put(ByteBuffer buffer) {_cache.put(buffer);}
    
    public boolean getReusingStore() {
    	return _reusingStore;
    }
    
}