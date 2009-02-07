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
package org.codehaus.wadi.web.impl;

import org.codehaus.wadi.core.manager.Router;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision: 1737 $
 */
public class JkRouter implements Router {
    private final String suffix;

    public JkRouter(String info) {
        if (null == info) {
            throw new IllegalArgumentException("info is required");
        }
        suffix = "." + info;
    }

    public Object strip(Object id) {
        if (!(id instanceof String)) {
            return id;
        }
        String newId = (String) id;
        int i = newId.lastIndexOf(".");
        if (i < 0) {
            return newId;
        } else {
            return newId.substring(0, i);
        }
    }

    public Object augment(Object id) {
        if (!(id instanceof String)) {
            return id;
        }
        String newId = (String) id;
        int i = newId.lastIndexOf(".");
        if (i < 0) {
            return newId + suffix;
        } else {
            if (newId.endsWith(suffix)) {
                // it's our routing info - leave it
                return newId;
            } else {
                // it's someone else's - replace it
                return newId.substring(0, i) + suffix; 
            }
        }
    }
}
