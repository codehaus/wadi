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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import EDU.oswego.cs.dl.util.concurrent.WaitableInt;

/**
 * You have a flock of n Llamas, you [un]tie a knot in your Quipu as each one leaves/enters your pen.
 * When all are in/out, you are free to continue. If the Llamas take too long, you can leave anyway !
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class Quipu extends WaitableInt {

	protected final static Log _log = LogFactory.getLog(Quipu.class);
	
	public Quipu(int numLlammas) {
        super(numLlammas);
        // TODO Auto-generated constructor stub
    }

    public boolean waitFor(long timeout) throws InterruptedException {
        long end=System.currentTimeMillis()+timeout;
        long now=0;
        synchronized(lock_) {
          while (!(value_==0) && (now=System.currentTimeMillis())<end) lock_.wait(end-now);
        }
        return value_==0;
      }
    
    // TODO - consider synchronisation...
    protected final Collection _results=Collections.synchronizedCollection(new ArrayList());
    
    public void putResult(Object result) {
        _results.add(result);
        if (_log.isTraceEnabled()) _log.trace("result arrived: "+result);
        decrement();
    }
    
    public Collection getResults() {
        return _results;
    }
}
