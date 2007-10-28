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
package org.codehaus.wadi.aop.tracker.visitor;

import java.util.IdentityHashMap;

import org.codehaus.wadi.aop.tracker.InstanceTracker;
import org.codehaus.wadi.aop.tracker.VisitorContext;

/**
 * 
 * @version $Revision: 1538 $
 */
public class BaseVisitorContext implements VisitorContext {
    private final IdentityHashMap<InstanceTracker, Boolean> visited = new IdentityHashMap<InstanceTracker, Boolean>();
    
    public void registerAsVisited(InstanceTracker instanceTracker) {
        visited.put(instanceTracker, Boolean.TRUE);
    }

    public boolean isVisited(InstanceTracker instanceTracker) {
        return visited.containsKey(instanceTracker);
    }
    
}
