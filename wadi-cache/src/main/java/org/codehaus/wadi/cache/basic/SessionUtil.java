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

import org.codehaus.wadi.core.session.Session;

/**
 *
 * @version $Rev:$ $Date:$
 */
public class SessionUtil {
    private static final String SESSION_KEY_OBJECT_INFO_ENTRY = "objectInfoEntry";

    private SessionUtil() {
    }

    public static ObjectInfoEntry getObjectInfoEntry(Session session) {
        if (null == session) {
            throw new IllegalArgumentException("session is required");
        }
        
        return (ObjectInfoEntry) session.getState(SESSION_KEY_OBJECT_INFO_ENTRY);
    }
    
    public static void setObjectInfoEntry(Session session, ObjectInfoEntry objectInfoEntry) {
        if (null == session) {
            throw new IllegalArgumentException("session is required");
        } else if (null == objectInfoEntry) {
            throw new IllegalArgumentException("objectInfoEntry is required");
        }

        session.addState(SESSION_KEY_OBJECT_INFO_ENTRY, objectInfoEntry);
    }
    
}
