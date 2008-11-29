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

package org.codehaus.wadi.cache.basic;

import org.codehaus.wadi.cache.AcquisitionInfo;
import org.codehaus.wadi.core.contextualiser.BasicInvocation;
import org.codehaus.wadi.core.contextualiser.InvocationContext;
import org.codehaus.wadi.core.contextualiser.InvocationException;

/**
 *
 * @version $Rev:$ $Date:$
 */
public class CacheInvocation extends BasicInvocation {

    private ObjectInfoEntry objectInfoEntry;
    
    public CacheInvocation(String sessionKey, AcquisitionInfo acquisitionInfo) {
        super(sessionKey, acquisitionInfo.getCacheEntryAccessWaitTime());
    }

    public ObjectInfoEntry getObjectInfoEntry() {
        if (null == objectInfoEntry) {
            throw new IllegalStateException("objectInfoEntry not set.");
        }
        return objectInfoEntry;
    }

    @Override
    protected void doInvoke(InvocationContext context) throws InvocationException {
        if (null == session) {
            objectInfoEntry = newObjectInfoEntry();
            return;
        }

        objectInfoEntry = SessionUtil.getObjectInfoEntry(session);
        
        if (null == objectInfoEntry) {
            objectInfoEntry = newObjectInfoEntry();
            SessionUtil.setObjectInfoEntry(session, objectInfoEntry);
        }
    }

    protected ObjectInfoEntry newObjectInfoEntry() {
        return new ObjectInfoEntry(sessionKey, new ObjectInfo());
    }

}
