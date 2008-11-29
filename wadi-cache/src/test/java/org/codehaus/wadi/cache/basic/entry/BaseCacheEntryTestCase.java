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

import org.codehaus.wadi.cache.AcquisitionPolicy;
import org.codehaus.wadi.core.manager.Manager;
import org.codehaus.wadi.core.util.Streamer;

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 *
 * @version $Rev:$ $Date:$
 */
public abstract class BaseCacheEntryTestCase extends RMockTestCase {

    protected AcquisitionPolicy policy;
    protected ReadOnlyCacheEntry prototype;
    protected Manager manager;
    protected AccessListener accessListener;
    protected GlobalObjectStore globalStore;
    protected Streamer streamer;

    @Override
    protected void setUp() throws Exception {
        policy = (AcquisitionPolicy) mock(AcquisitionPolicy.class);

        manager = (Manager) mock(Manager.class);
        accessListener = (AccessListener) mock(AccessListener.class);
        globalStore = (GlobalObjectStore) mock(GlobalObjectStore.class);
        streamer = (Streamer) mock(Streamer.class);
        prototype = new ReadOnlyCacheEntry(manager , accessListener, globalStore , streamer , "key");
    }
    
}
