/**
 * Copyright 2007 The Apache Software Foundation
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
import java.util.Iterator;


/**
 * @version $Revision: 1563 $
 */
public class StaticDispatcherRegistry implements DispatcherRegistry {
    private static final Collection dispatchers = new ArrayList();
    
    public void register(Dispatcher dispatcher) {
        synchronized (dispatchers) {
            dispatchers.add(dispatcher);
        }
    }

    public void unregister(Dispatcher dispatcher) {
        synchronized (dispatchers) {
            dispatchers.remove(dispatcher);   
        }
    }
    
    public Collection getDispatchers() {
        synchronized (dispatchers) {
            return new ArrayList(dispatchers);
        }
    }
    
    public Dispatcher getDispatcherByClusterName(String clusterName) {
        synchronized (dispatchers) {
            for (Iterator iter = dispatchers.iterator(); iter.hasNext();) {
                Dispatcher dispatcher = (Dispatcher) iter.next();
                if (dispatcher.getCluster().getClusterName().equals(clusterName)) {
                    return dispatcher;
                }
            }
        }
        throw new IllegalStateException("No cluster having the name [" + clusterName + "]");
    }
}
