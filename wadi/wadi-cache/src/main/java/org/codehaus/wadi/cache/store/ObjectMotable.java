/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.codehaus.wadi.cache.store;

import org.codehaus.wadi.cache.basic.ObjectInfoEntry;
import org.codehaus.wadi.cache.basic.SessionUtil;
import org.codehaus.wadi.core.motable.AbstractMotable;
import org.codehaus.wadi.core.motable.Motable;
import org.codehaus.wadi.core.session.Session;

/**
 *
 * @version $Rev:$ $Date:$
 */
public class ObjectMotable extends AbstractMotable {
    private final ObjectInfoEntry objectInfoEntry;
    
    public ObjectMotable(ObjectInfoEntry objectInfoEntry) {
        if (null == objectInfoEntry) {
            throw new IllegalArgumentException("objectInfoEntry is required");
        }
        this.objectInfoEntry = objectInfoEntry;
        
        getAbstractMotableMemento().setName(objectInfoEntry.getKey());
    }

    public ObjectInfoEntry getObjectInfoEntry() {
        return objectInfoEntry;
    }
    
    public synchronized void mote(Motable recipient) throws Exception {
        if (!(recipient instanceof Session)) {
            throw new IllegalArgumentException("recipient must be an instance of " + Session.class.getName());
        }
        Session session = (Session) recipient;
        SessionUtil.setObjectInfoEntry(session, objectInfoEntry);
    }

    public byte[] getBodyAsByteArray() throws Exception {
        throw new UnsupportedOperationException();
    }

    public void setBodyAsByteArray(byte[] bytes) throws Exception {
        throw new UnsupportedOperationException();
    }

}
