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
package org.codehaus.wadi.core.contextualiser;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.core.WADIRuntimeException;

/**
 * A lock Collapser that collapses according to hash code
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class HashingCollapser implements Collapser {
	private static final Log _log = LogFactory.getLog(HashingCollapser.class);
    
    private final Lock[] locks;

    public HashingCollapser(int numSyncs, final long timeout) {
        locks = new Lock[numSyncs];
        for (int i = 0; i < locks.length; i++) {
            locks[i] = new ReentrantLock() {
                public void lock() {
                    try {
                        tryLock(timeout, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {
                        throw new WADIRuntimeException(e);
                    }
                }

                public void lockInterruptibly() throws InterruptedException {
                    boolean locked = tryLock(timeout, TimeUnit.MILLISECONDS);
                    if (!locked) {
                        throw new WADIRuntimeException("Cannot acquire lock after " + timeout + "ms");
                    }
                }

                public boolean tryLock() {
                    try {
                        return tryLock(timeout, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {
                        throw new WADIRuntimeException(e);
                    }
                }
            };
        }
    }

    public Lock getLock(Object id) {
        // Jetty seems to generate negative session id hashcodes...
        int index = Math.abs(id.hashCode() % locks.length); 
        if (_log.isTraceEnabled()) {
            _log.trace("collapsed " + id + " to index: " + index);
        }
        return locks[index];
    }
    
}
