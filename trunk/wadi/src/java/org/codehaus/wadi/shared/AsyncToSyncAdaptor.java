/**
 *
 * Copyright 2003-2004 The Apache Software Foundation
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

package org.codehaus.wadi.shared;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import EDU.oswego.cs.dl.util.concurrent.BrokenBarrierException;
import EDU.oswego.cs.dl.util.concurrent.Rendezvous;
import EDU.oswego.cs.dl.util.concurrent.TimeoutException;

/**
 * @author jules
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class AsyncToSyncAdaptor {
    	protected Log _log = LogFactory.getLog(getClass());
    	protected int _nextId=0;
    	protected Map _entries=Collections.synchronizedMap(new HashMap());
    	
    	class ConversationalMessage
    	{
                public String getRvId() {
                    return _rvId;
                }
                public void setRvId(String id) {
                    _rvId = id;
                }
                public long getRvTimeout() {
                    return _rvTimeout;
                }
                public void setRvTimeout(long _timeout) {
                    this._rvTimeout = _timeout;
                }
                
    	    	protected long _rvTimeout=2000L;
    	    	protected String _rvId=null;
    	}
    	
    	public Object send(ConversationalMessage message, long timeout)
    	{
    	    Rendezvous rv=new Rendezvous(2);
    	    String id=""+_nextId++;
    	    _entries.put(id, rv);
    	    message.setRvId(id);
    	    message.setRvTimeout(timeout);

    	    // send message here...
    	    
    	    Object result=null;
    	    try
    	    {
    	        result=rv.attemptRendezvous(null, timeout);
    	    }
    	    catch (TimeoutException e)
    	    {
    	        _log.warn("timed out at rendezvous", e);
    	    }
    	    catch (InterruptedException e)
    	    {
    	        _log.warn("unexpectedly interrupted whilst waiting on rendezvous", e);
    	    }
    	    catch (BrokenBarrierException e)
    	    {
    	        _log.warn("Broken barrier - should never happen - we own this rendezvous", e);
    	    }
    	    finally
    	    {
    	        _entries.remove(id);
    	    }

    	    return result;
    	}
    	
    	public Object receive(ConversationalMessage message)
    	{
    	    long timeout=message.getRvTimeout();
    	    String id=message.getRvId();
    	    Rendezvous rv=(Rendezvous)_entries.get(id);
    	    Object result=null;
    	    
    	    try
    	    {
    	        result=rv.attemptRendezvous(message, timeout);
    	    }
    	    catch (TimeoutException e)
    	    {
    	        _log.warn("rendezvous timed out", e);
    	    }
    	    catch (InterruptedException e)
    	    {
    	        _log.warn("unexpectedly interrupted whilst waiting on rendezvous", e);
    	    }
    	    catch (BrokenBarrierException e)
    	    {
    	        _log.warn("Broken barrier - we must have arrived too late for rendezvous", e);
    	    }
    	    
    	    return result;
    	}
}

