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

import java.io.Serializable;
import java.net.URI;

/**
 * 
 * @version $Revision: 1538 $
 */
public class ServiceSpaceName implements Serializable {
    private final URI uri;

    public ServiceSpaceName(URI uri) {
        if (null == uri) {
            throw new IllegalArgumentException("uri is required");
        }
        String path = uri.getPath();
        if (null == path || path.length() == 0) {
            throw new IllegalArgumentException("uri path is required");
        }
        
        this.uri = uri;
    }
    
    public boolean equals(Object obj) {
        if (!(obj instanceof ServiceSpaceName)) {
            return false;
        }
        ServiceSpaceName other = (ServiceSpaceName) obj;
        return uri.equals(other.uri);
    }
    
    public int hashCode() {
        return uri.hashCode();
    }
    
    public String toString() {
        return uri.toString();
    }
    
}
