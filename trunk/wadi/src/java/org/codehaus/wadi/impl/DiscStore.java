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
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.ExclusiveDiscMotableConfig;
import org.codehaus.wadi.Store;
import org.codehaus.wadi.StoreMotable;
import org.codehaus.wadi.StoreMotableConfig;
import org.codehaus.wadi.Streamer;

public class DiscStore implements Store, ExclusiveDiscMotableConfig {

    protected final Log _log=LogFactory.getLog(getClass());
    protected final Streamer _streamer;
    protected final File _dir;

    public DiscStore(Streamer streamer, File dir) {
        _streamer=streamer;
        assert dir.exists();
        assert dir.isDirectory();
        assert dir.canRead();
        assert dir.canWrite();
        _dir=dir;
    }
    
    public void clean() {
        File[] files=_dir.listFiles();
        int l=files.length;
        for (int i=0; i<l; i++) {
            files[i].delete();
            _log.info("removed (exclusive disc): "+l);
        }
    }
    
    public void load(Map map, boolean accessOnLoad) {
        // if our last incarnation suffered a catastrophic failure there may be some sessions
        // in our directory - FIXME - if replicating, we may not want to reload these...
        long time=System.currentTimeMillis();
        String[] list=_dir.list();
        int l=list.length;
        int suffixLength=".".length()+_streamer.getSuffix().length();
        for (int i=0; i<l; i++) {
            String name=list[i];
            String id=name.substring(0, name.length()-suffixLength);
            ExclusiveDiscMotable motable=new ExclusiveDiscMotable();
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
                map.put(id, motable);
            } catch (Exception e) {
                if (_log.isErrorEnabled()) _log.error("failed to load: "+name);
            }
        }
        if (_log.isInfoEnabled())_log.info("loaded (exclusive disc): "+list.length);
    }
    
    public StoreMotable create() {
        return new ExclusiveDiscMotable();
    }
    
    public String getStartInfo() {return _dir.toString();}
    public String getDescription() {return "exclusive disc";}
    
    // ExclusiveDiscMotableConfig
    
    public File getDirectory() {return _dir;}
    public String getSuffix() {return _streamer.getSuffixWithDot();}
}