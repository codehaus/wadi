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
package org.codehaus.wadi.core.session;

import org.codehaus.wadi.core.manager.Manager;
import org.codehaus.wadi.core.motable.AbstractMotable;
import org.codehaus.wadi.impl.SimpleStreamer;
import org.codehaus.wadi.impl.Utils;

/**
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision: 1533 $
 */
public abstract class AbstractSession extends AbstractMotable implements Session {
    protected final Attributes attributes;
    protected final Manager manager;

    public AbstractSession(Attributes attributes, Manager manager) {
        if (null == attributes) {
            throw new IllegalArgumentException("attributes is required");
        } else if (null == manager) {
            throw new IllegalArgumentException("manager is required");
        }
        this.attributes = attributes;
        this.manager = manager;
    }

    public synchronized void destroy() throws Exception {
        super.destroy();
        manager.destroy(this);
        attributes.clear();
    }

    protected synchronized void destroyForMotion() throws Exception {
        super.destroyForMotion();
        attributes.clear();
    }

    public synchronized void onEndProcessing() {
        newSession = false;
    }
    
    public synchronized byte[] getBodyAsByteArray() throws Exception {
        return Utils.getContent(this, new SimpleStreamer());
    }
    
    public synchronized Object addState(String key, Object value) {
        Object oldValue = attributes.put(key, value);
        onAddSate(key, oldValue, value);
        return oldValue;
    }

    public synchronized Object getState(String key) {
        return attributes.get(name);
    }

    public synchronized Object removeState(String key) {
        Object oldValue = attributes.remove(key);
        onRemoveState(key, oldValue);
        return oldValue;
    }
    
    protected void onAddSate(String name, Object oldValue, Object newValue) {
    }

    protected void onRemoveState(String name, Object oldValue) {
    }
}
