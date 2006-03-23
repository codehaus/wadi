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
package org.codehaus.wadi.replication.common;

import java.io.ObjectStreamException;
import java.io.Serializable;

/**
 * 
 * @version $Revision$
 */
public class ComponentEventType implements Serializable {
    private static final long serialVersionUID = 7592588413346741132L;

    public static final ComponentEventType JOIN = new ComponentEventType("JOIN");
    public static final ComponentEventType LEAVE = new ComponentEventType("LEAVE");

    private final String name;
    
    protected ComponentEventType(String name) {
        this.name = name;
    }
    
    public final int hashCode() {
        return super.hashCode();
    }
    
    public final boolean equals(Object obj) {
        return super.equals(obj);
    }
    
    public String toString() {
        return "ComponentEventType " + name;
    }
    
    protected Object readResolve() throws ObjectStreamException {
        if (name.equals(JOIN.name)) {
            return JOIN;
        } else if (name.equals(LEAVE.name)) {
            return LEAVE;
        } else {
            throw new AssertionError();
        }
    }
}