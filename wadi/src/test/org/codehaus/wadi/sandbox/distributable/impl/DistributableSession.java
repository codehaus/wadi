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
package org.codehaus.wadi.sandbox.distributable.impl;

import org.codehaus.wadi.sandbox.distributable.Distributer;
import org.codehaus.wadi.sandbox.impl.Manager;
import org.codehaus.wadi.sandbox.impl.Session;

/**
 * TODO - JavaDoc this type
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public class DistributableSession extends Session {

    protected final Distributer _distributer;
    
    /**
     * @param manager
     */
    public DistributableSession(Manager manager, Distributer distributer) {
        super(manager);
        _distributer=distributer;
        _attributes=_distributer.wrap(_attributes);
    }

    public Object getAttribute(String name) {
        return _distributer.getAttribute(_id, _attributes, name);
    }
    
    public Object setAttribute(String name, Object newValue) {
        return _distributer.setAttribute(_id, _attributes, name, newValue);
    }
    
    public Object removeAttribute(String name) {
        return _distributer.removeAttribute(_id, _attributes, name);
    }
}
