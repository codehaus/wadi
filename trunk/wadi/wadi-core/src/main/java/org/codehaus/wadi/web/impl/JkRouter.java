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

    public String strip(String session) {
        int i = session.lastIndexOf(".");
        if (i < 0) {
            return session;
        } else {
            return session.substring(0, i);
        }
    }

    public String augment(String id) {
        int i = id.lastIndexOf(".");
        if (i < 0) {
            return id + suffix;
        } else {
            if (id.endsWith(suffix)) {
                // it's our routing info - leave it
                return id;
            } else {
                // it's someone else's - replace it
                return id.substring(0, i) + suffix; 
            }
        }
    }
}
