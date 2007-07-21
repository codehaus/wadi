/**
 * Copyright 2006 The Apache Software Foundation
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
package org.codehaus.wadi.activecluster;

import org.codehaus.wadi.evacuation.AbstractTestEvacuation;
import org.codehaus.wadi.group.Dispatcher;

/**
 * 
 * @version $Revision: $
 */
public class TestACTCPEvacuation extends AbstractTestEvacuation {
	
    private final String clusterName = ACTestUtil.CLUSTER_NAME;
    private final String clusterUri = ACTestUtil.CLUSTER_URI_TCP;
    private final long timeout = ACTestUtil.CLUSTER_INACTIVE_TIME;
    private ACTestUtil testUtil;

    protected void setUp() throws Exception {
        super.setUp();
        testUtil = new ACTestUtil();
        testUtil.startTCPService();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        testUtil.stopTCPService();
    }
    
    protected Dispatcher newDispatcher(String name) throws Exception {
        return new ActiveClusterDispatcher(clusterName, name, clusterUri, null, timeout);
    }
    
}

