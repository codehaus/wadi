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
package org.codehaus.wadi.group.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.group.Envelope;
import org.codehaus.wadi.group.ServiceEndpoint;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedInt;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision: 1595 $
 */
class BasicMessageDispatcherManager implements MessageDispatcherManager {
    protected static final Log _messageLog = LogFactory.getLog("org.codehaus.wadi.INBOUND_MESSAGES");

    private final AbstractDispatcher _dispatcher;
    private final Log _log = LogFactory.getLog(getClass());
    private final IdentityHashMap _msgDispatchers = new IdentityHashMap();
//    private final Map _runningLinks = new HashMap();
    private final ThreadPool _executor;
	
	public BasicMessageDispatcherManager(AbstractDispatcher dispatcher, ThreadPool executor) {
        _dispatcher = dispatcher;
        _executor = executor;
	}
	
	public void register(ServiceEndpoint msgDispatcher) {
        synchronized (_msgDispatchers) {
            _msgDispatchers.put(msgDispatcher, new ServiceEndpointWrapper(msgDispatcher));            
        }
    }
    
    public void unregister(ServiceEndpoint msgDispatcher, int nbAttemp, long delayMillis) {
        ServiceEndpointWrapper seWrapper;
        synchronized (_msgDispatchers) {
            seWrapper = (ServiceEndpointWrapper) _msgDispatchers.remove(msgDispatcher);
            if (null == seWrapper) {
                throw new IllegalArgumentException(msgDispatcher + " is unknown.");
            }
        }

        // this isn't failproof - if onMessage has not yet been called,
        // the counter may still read 0 - but it's the best we can do...
        for (int i= nbAttemp; seWrapper.getNumberOfCurrentDispatch()>0 && i > 0; i--) {
            try {
                Thread.sleep(delayMillis);
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }
	
    public void onMessage(Envelope message) {
    	Collection targetDispatchers;
    	synchronized (_msgDispatchers) {
    		targetDispatchers = new ArrayList(_msgDispatchers.values());
    	}

    	if (targetDispatchers.size()==0) {
    		if (_log.isWarnEnabled())
    			_log.warn("spurious message received: " + message);
    		return;
    	}

    	// we have a dispatchable message - but is it simply a reiteration of a task/Link that
    	// we are already performing ?
//    	boolean duplicate=false;
//		SynchronizedInt young=new SynchronizedInt(targetDispatchers.size());
//    	synchronized (_runningLinks) {
//    		// ugly, but in the usual case, the message will not be a duplicate and this will only perform
//    		// a single synched operation on th Map...
//    		SynchronizedInt old=(SynchronizedInt)_runningLinks.put(message.getSourceCorrelationId(), young);
//    		if (old!=null) {
//    			duplicate=true;
//    			_runningLinks.put(message.getSourceCorrelationId(), old); // put it back !
//    		}
//    	}
//    	if (duplicate) {
//    		if (_log.isTraceEnabled())
//    			_log.trace("Link already active, duplicate message ignored: "+message);
//    		return;
//    	}

    	try {
    		for (Iterator iter = targetDispatchers.iterator(); iter.hasNext();) {
    			ServiceEndpointWrapper dispatcher = (ServiceEndpointWrapper) iter.next();
    			boolean dispatchMessage = dispatcher.testDispatchMessage(message);
    			if (dispatchMessage) {
    				if (_messageLog.isTraceEnabled()) {
    					_messageLog.trace(message +
    							" {"+ message.getReplyTo() +
    							"->"+ message.getAddress() + 
    							"} - " +
    							message.getTargetCorrelationId() + 
    							"/" +
    							message.getSourceCorrelationId());
    				}
    				dispatcher.beforeDispatch();
    				_executor.execute(new DispatchRunner(dispatcher, message));
    			}
    		}
    	} catch (InterruptedException e) {
    		_log.warn("bad message", e);
    	}
    }
    
    class DispatchRunner implements Runnable {
    	protected final ServiceEndpointWrapper _msgDispatcher;
    	protected final Envelope _message;

    	public DispatchRunner(ServiceEndpointWrapper msgDispatcher, Envelope message) {
    		_msgDispatcher=msgDispatcher;
    		_message=message;
    	}

    	public void run() {
    		try {
    			_dispatcher.hook();
    			_msgDispatcher.dispatch(_message);
    			_msgDispatcher.afterDispatch();
    		} catch (Exception e) {
    			_log.error("problem dispatching message", e);
    		} finally {
    			// ensure that we remove this Link from the list of those being run...
    			// clumsy, because we have to reference count it down to 0 as there may
    			// be more than one dispatcher thread running on its behalf...
//    			SynchronizedInt _count;
//    			synchronized (_runningLinks) {
//    				_count=(SynchronizedInt)_runningLinks.get(_message.getSourceCorrelationId());
//    			}
//				if (_count.decrement()==0) {
//					synchronized (_runningLinks) {
//						_runningLinks.remove(_message.getSourceCorrelationId());
//					}
//				}
    		}
    	}
    }
}
