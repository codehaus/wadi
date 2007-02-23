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

import org.codehaus.wadi.Session;
import org.codehaus.wadi.web.Attributes;

/**
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision: 1533 $
 */
public abstract class AbstractSession extends AbstractMotable implements Session {
    protected final Attributes attributes;

    public AbstractSession(Attributes attributes) {
        if (null == attributes) {
            throw new IllegalArgumentException("attributes is required");
        }
        this.attributes = attributes;
    }

    public synchronized void onEndProcessing() {
        newSession = false;
    }
    
    public synchronized byte[] getBodyAsByteArray() throws Exception {
        return Utils.getContent(this, new SimpleStreamer());
    }

}
