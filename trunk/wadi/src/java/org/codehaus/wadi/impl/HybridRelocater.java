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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.Contextualiser;
import org.codehaus.wadi.Immoter;
import org.codehaus.wadi.Relocater;
import org.codehaus.wadi.RelocaterConfig;

import EDU.oswego.cs.dl.util.concurrent.Sync;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedBoolean;

/**
 * Combine various RelocationStrategies to produce a cleverer one
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class HybridRelocater extends AbstractRelocater {

    protected final Log _log=LogFactory.getLog(getClass());

    protected SynchronizedBoolean _shuttingDown;
    
    public void init(RelocaterConfig config) {
        super.init(config);
        _shuttingDown=_config.getShuttingDown();
    }
    
    public boolean relocate(HttpServletRequest hreq, HttpServletResponse hres, FilterChain chain, String name, Immoter immoter, Sync motionLock, Map locationMap) throws IOException, ServletException {
        if (_log.isTraceEnabled()) _log.trace("sending location request: "+name);
        String sessionName=name;
        String nodeName=_config.getNodeName();
        boolean sessionOrRequestPreferred=getSessionOrRequestPreferred();
        boolean shuttingDown=_shuttingDown.get();
        long lastKnownTime=0L;
        String lastKnownPlace=null;
        long requestHandOverTimeout=2000;// TODO - parameterise
        MessageDispatcher.Settings settingsInOut=new MessageDispatcher.Settings();
        settingsInOut.from=_config.getLocation().getDestination();
        settingsInOut.to=_config.getDispatcher().getCluster().getDestination();
        settingsInOut.correlationId=nodeName+"-"+sessionName+"-"+System.currentTimeMillis();
        RelocationRequest request=new RelocationRequest(sessionName, nodeName, sessionOrRequestPreferred, shuttingDown, lastKnownTime, lastKnownPlace, requestHandOverTimeout);
        //RelocationResponse response=(RelocationResponse)_config.getDispatcher().exchangeMessages(name, _rvMap, request, settingsInOut, _timeout);
	    
		return false;
	}

	public void setTop(Contextualiser top){/* NYI */}
	public Contextualiser getTop(){return null;}
    
    boolean getSessionOrRequestPreferred() {
        return RelocationRequest._RELOCATE_REQUEST_PREFERRED;
        // check out LB's capabilities during init()....
    }
}
