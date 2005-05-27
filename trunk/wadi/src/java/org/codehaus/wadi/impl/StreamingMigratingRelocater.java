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

import java.io.IOException;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.wadi.Immoter;
import org.codehaus.wadi.Location;
import org.codehaus.wadi.RelocaterConfig;
import org.codehaus.wadi.SessionRelocater;
import org.codehaus.wadi.io.Server;

import EDU.oswego.cs.dl.util.concurrent.Sync;

public class StreamingMigratingRelocater implements SessionRelocater {

    public StreamingMigratingRelocater() {
        super();
    }
    
    protected RelocaterConfig _config;

    public void init(RelocaterConfig config) {
        _config=config;
    }

    public void destroy() {
        _config=null;
    }

    public boolean relocate(HttpServletRequest hreq, HttpServletResponse hres, FilterChain chain, String name, Immoter immoter, Sync motionLock, Map locationMap) throws IOException, ServletException {
        // check cache
        Server server=_config.getServer();

        // repeat ...
        
        boolean sessionMoves=true;
        
        Location location;
        if (!sessionMoves && (location=(Location)locationMap.get(name))!=null) {
            // if cache is fresh (i.e. holds a location) && lock required is 'R'
            //   p2p to known location with last known timestamp stating 'R' lock needed
            //   response is one of - don't know, newer location, lock-acquired
            //   if still no success fall through to else...
        }
        
        // else - (cache is stale or 'W' lock required) 
        //   p2n to whole cluster with last known timestamp stating r or w lock needed
        //   responses are one of: newer location, lock-acquired
        
        // until either;
        //   you receive a lock-acquired (success)
        // or:
        //  you cease to be told of fresher locations (fail)
        
        // update our location cache on each iteration...
        
        return false;
    }

}
