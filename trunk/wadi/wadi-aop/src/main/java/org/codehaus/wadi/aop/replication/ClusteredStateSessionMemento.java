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
package org.codehaus.wadi.aop.replication;

import org.codehaus.wadi.aop.annotation.ClusteredState;
import org.codehaus.wadi.aop.annotation.TrackedMethod;
import org.codehaus.wadi.aop.annotation.TrackingLevel;
import org.codehaus.wadi.core.session.DistributableSessionMemento;
import org.codehaus.wadi.core.session.StandardAttributesMemento;

/**
 * 
 * @version $Revision: 1538 $
 */
@ClusteredState(trackingLevel=TrackingLevel.METHOD)
public class ClusteredStateSessionMemento extends DistributableSessionMemento {
    private String name;
    private long creationTime;
    private long lastAccessedTime;
    private int maxInactiveInterval;
    private ClusteredStateAttributesMemento memento;
    
    @TrackedMethod
    @Override
    public void setName(String name) {
        this.name = name;
        super.setName(name);
    }

    @TrackedMethod
    @Override
    public void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
        super.setCreationTime(creationTime);
    }

    @TrackedMethod
    @Override
    public void setLastAccessedTime(long lastAccessedTime) {
        this.lastAccessedTime = lastAccessedTime;
        super.setLastAccessedTime(lastAccessedTime);
    }
    
    @TrackedMethod
    @Override
    public void setMaxInactiveInterval(int maxInactiveInterval) {
        this.maxInactiveInterval = maxInactiveInterval;
        super.setMaxInactiveInterval(maxInactiveInterval);
    }
    
    @Override
    public StandardAttributesMemento getAttributesMemento() {
        return memento;
    }
    
    @TrackedMethod
    @Override
    public void setAttributesMemento(StandardAttributesMemento attributesMemento) {
        this.memento = (ClusteredStateAttributesMemento) attributesMemento;
        super.setAttributesMemento(attributesMemento);
    }
    
    public void onRestore() {
        setNewSession(false);
        super.setName(name);
        super.setCreationTime(creationTime);
        super.setLastAccessedTime(lastAccessedTime);
        super.setMaxInactiveInterval(maxInactiveInterval);
    }
    
}
