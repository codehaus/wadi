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

package org.codehaus.wadi.cache.basic.entry;


/**
 *
 * @version $Rev:$ $Date:$
 */
public enum CacheEntryState {
    READ_ONLY(false, false, false),
    CLEAN(true, true, false),
    INSERTED(false, true, false),
    UPDATED(false, true, true),
    DELETED(true, false, true);
    
    private final boolean insertable;
    private final boolean updatable;
    private final boolean dirty;

    private CacheEntryState(boolean insertable, boolean isUpdatable, boolean dirty) {
        this.insertable = insertable;
        this.updatable = isUpdatable;
        this.dirty = dirty;
    }

    public boolean isInsertable() {
        return insertable;
    }
    
    public boolean isUpdatable() {
        return updatable;
    }

    public boolean isDirty() {
        return dirty;
    }

}
