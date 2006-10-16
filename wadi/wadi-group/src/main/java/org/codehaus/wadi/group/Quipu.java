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
package org.codehaus.wadi.group;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import EDU.oswego.cs.dl.util.concurrent.WaitableInt;

/**
 * You have a flock of n Llamas, you [un]tie a knot in your Quipu as each one leaves/enters your pen.
 * When all are in/out, you are free to continue. If the Llamas take too long, you can leave anyway !
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision: 1346 $
 */
public class Quipu extends WaitableInt {
	protected final static Log _log = LogFactory.getLog(Quipu.class);

    protected final Collection _results = new ArrayList();
    protected final String correlationId;

    public Quipu(int numLlammas, String correlationId) {
        super(numLlammas);
        if (null == correlationId) {
            throw new IllegalArgumentException("correlationId is required");
        }
        this.correlationId = correlationId;
    }

    public boolean waitFor(long timeout) throws InterruptedException {
        long end = System.currentTimeMillis() + timeout;
        long now = 0;
        synchronized (lock_) {
            while (!(value_ == 0) && (now = System.currentTimeMillis()) < end) {
                lock_.wait(end - now);
            }
            return value_ == 0;
        }
    }

    public void putResult(Object result) {
        synchronized (lock_) {
            _results.add(result);
            if (_log.isTraceEnabled()) {
                _log.trace("result arrived: " + result);
            }
            decrement();
        }
    }

    public Collection getResults() {
        synchronized (lock_) {
            return new ArrayList(_results);
        }
    }

    public String getCorrelationId() {
        return correlationId;
    }
    
    public String toString() {
        return "Quipu [" + correlationId + "]";
    }
    
}
