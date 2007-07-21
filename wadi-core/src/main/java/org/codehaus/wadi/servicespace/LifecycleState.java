/**
 * Copyright 2006 The Apache Software Foundation
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
package org.codehaus.wadi.servicespace;

import java.io.ObjectStreamException;
import java.io.Serializable;

/**
 * 
 * @version $Revision: $
 */
public final class LifecycleState implements Serializable {
    public static final LifecycleState STARTING = new LifecycleState("STARTING");
    public static final LifecycleState STARTED = new LifecycleState("STARTED");
    public static final LifecycleState STOPPING = new LifecycleState("STOPPING");
    public static final LifecycleState STOPPED = new LifecycleState("STOPPED");
    public static final LifecycleState FAILED = new LifecycleState("FAILED");
    public static final LifecycleState AVAILABLE = new LifecycleState("AVAILABLE");
    
    public static final LifecycleState[] SUPPORTED_STATES = new LifecycleState[] {
        STARTING, STARTED, STOPPING, STOPPED, FAILED, AVAILABLE 
    };
    
    private final String label;
    
    private LifecycleState(String label) {
        this.label = label;
    }
    
    public String toString() {
        return label;
    }
    
    private Object readResolve () throws ObjectStreamException {
        for (int i = 0; i < SUPPORTED_STATES.length; i++) {
            if (label.equals(SUPPORTED_STATES[i].label)) {
                return SUPPORTED_STATES[i];
            }
        }
        throw new IllegalStateException("No lifecycle state [" + label + "]");
    }
    
}
