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


/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision: 1885 $
 */
public class StandardSessionFactory implements SessionFactory {

    protected final AttributesFactory attributesFactory;
    private Manager manager;

    public StandardSessionFactory(AttributesFactory attributesFactory) {
        if (null == attributesFactory) {
            throw new IllegalArgumentException("attributesFactory is required");
        }
        this.attributesFactory = attributesFactory;
    }

    public void setManager(Manager manager) {
        this.manager = manager;
    }

    public Session create() {
        return new StandardSession(newAttributes(), getManager());
    }

    protected Attributes newAttributes() {
        return attributesFactory.create();
    }

    protected Manager getManager() {
        if (null == manager) {
            throw new IllegalStateException("Manager is not set");
        }
        return manager;
    }
    
}
