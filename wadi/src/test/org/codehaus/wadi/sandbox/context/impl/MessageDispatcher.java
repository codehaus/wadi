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
package org.codehaus.wadi.sandbox.context.impl;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class MessageDispatcher implements MessageListener {
	protected final Log    _log=LogFactory.getLog(getClass());
	protected final Map    _map=new HashMap();
	protected final Object _target;
	
	// cache target.<methodName>(ObjectMessage, <SomeClass>) methods for use with message dispatch...
	public MessageDispatcher(Object target, String methodName) {
		_target=target;
		Method[] ms=target.getClass().getMethods();
		for (int i=ms.length-1; i>=0; i--) {
			Method m=ms[i];
			Class[] pts=null;
			if (methodName.equals(m.getName()) && (pts=m.getParameterTypes()).length==2 && pts[0]==ObjectMessage.class) {
				// return type should be void...
				//_log.info("caching method: "+m+" for class: "+pts[1]);
				_map.put(pts[1], m);
			}
		}
	}
	
	public void onMessage(Message message) {
		//TODO -  Threads should be pooled and reusable
		// any allocs should be cached as ThreadLocals and reused 
		// we need a way of indicating which messages should be threaded and which not...
		// how about a producer/consumer arrangement...
		new DispatchThread(message).start();
	}
	
	class DispatchThread extends Thread {
		protected final Message _message;
		protected final ThreadLocal _pair=new ThreadLocal(){protected Object initialValue() {return new Object[2];}};

		public DispatchThread(Message message) {
			_message=message;
		}
		
		public void run() {
			ObjectMessage om=null;
			Object obj=null;
			Method m;
			
			try {
				if (_message instanceof ObjectMessage && (om=(ObjectMessage)_message)!=null && (obj=om.getObject())!=null && (m=(Method)_map.get(obj.getClass()))!=null) {
					Object[] pair=(Object[])_pair.get();
					pair[0]=om;
					pair[1]=obj;
					m.invoke(_target, pair);
					// if a message is of unrecognised type, we should recurse up its class hierarchy, memoizing the result
					// if we find a class that matches - TODO - This would enable message subtyping...
				}
			} catch (Exception e) {
				_log.error("problem processing location message", e);
			}
		}
	}
}