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

import EDU.oswego.cs.dl.util.concurrent.WaitableInt;

public class TimeoutableInt extends WaitableInt {

    public TimeoutableInt(int initialValue) {
        super(initialValue);
        // TODO Auto-generated constructor stub
    }

    public TimeoutableInt(int initialValue, Object lock) {
        super(initialValue, lock);
        // TODO Auto-generated constructor stub
    }

    public void whenEqual(int c, long timeout) throws InterruptedException {
        long end=System.currentTimeMillis()+timeout;
        synchronized(lock_) {
          while (!(value_ == c) && System.currentTimeMillis()<end) lock_.wait(timeout);
        }
      }
    
    // TODO - consider synchronisation...
    protected final Collection _results=Collections.synchronizedCollection(new ArrayList());
    
    public void putResult(Object result) {
        _results.add(result);
        decrement();
    }
    
    public Collection getResults() {
        return _results;
    }
}
