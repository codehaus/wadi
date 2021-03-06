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

/**
 * You have a flock of n Llamas, you [un]tie a knot in your Quipu as each one leaves/enters your pen.
 * When all are in/out, you are free to continue. If the Llamas take too long, you can leave anyway !
 *
 * @version $Revision: 1346 $
 */
public class Quipu {
	protected final static Log _log = LogFactory.getLog(Quipu.class);

	private int remainingNumberOfResults;
	private final Object lock;
    protected final Collection<Object> results = new ArrayList<Object>();
    protected final String correlationId;
    protected Exception exception;

    public Quipu(int numLlammas, String correlationId) {
        if (null == correlationId) {
            throw new IllegalArgumentException("correlationId is required");
        }
        this.correlationId = correlationId;

        lock = new Object();
        
        remainingNumberOfResults = numLlammas;
    }

    public boolean waitFor(long timeout) throws InterruptedException, QuipuException {
        long end = System.currentTimeMillis() + timeout;
        long now = 0;
        synchronized (lock) {
            while (0 != remainingNumberOfResults && (now = System.currentTimeMillis()) < end) {
                if (null != exception) {
                    throw new QuipuException(exception);
                }
                lock.wait(end - now);
            }
            return 0 == remainingNumberOfResults;
        }
    }

    public void putResult(Object result) {
        synchronized (lock) {
            if (0 == remainingNumberOfResults) {
                return;
            }
            results.add(result);
            remainingNumberOfResults--;
            lock.notifyAll();
        }
    }

    public Collection getResults() {
        synchronized (lock) {
            return new ArrayList(results);
        }
    }

    public String getCorrelationId() {
        return correlationId;
    }
    
    public void putException(Exception exception) {
        synchronized (lock) {
            this.exception = exception;
            lock.notifyAll();
        }
    }
    
    public String toString() {
        return "Quipu [" + correlationId + "]; results [" + results + "]";
    }

}
