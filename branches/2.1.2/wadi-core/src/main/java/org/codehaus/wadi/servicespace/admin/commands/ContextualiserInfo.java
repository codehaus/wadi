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
package org.codehaus.wadi.servicespace.admin.commands;

import java.io.Serializable;

/**
 * 
 * @version $Revision: 1538 $
 */
public class ContextualiserInfo implements Serializable {
    private final String name;
    private final int index;
    
    public ContextualiserInfo(String name, int index) {
        if (null == name) {
                throw new IllegalArgumentException("name is required");
        } else if (0 > index) {
            throw new IllegalArgumentException("index must be greater than -1");
        }
        this.name = name;
        this.index = index;
    }

    public String getName() {
        return name;
    }

    public int getIndex() {
        return index;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof ContextualiserInfo)) {
            return false;
        }
        ContextualiserInfo other = (ContextualiserInfo) obj;
        return name.equals(other.name);
    }
    
    public int hashCode() {
        return name.hashCode();
    }
    
}
