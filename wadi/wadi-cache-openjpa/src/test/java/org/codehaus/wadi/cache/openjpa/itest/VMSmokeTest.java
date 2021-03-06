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

package org.codehaus.wadi.cache.openjpa.itest;

import java.net.URI;

import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.vm.VMBroker;
import org.codehaus.wadi.group.vm.VMDispatcher;
import org.codehaus.wadi.web.impl.URIEndPoint;

/**
 *
 * @version $Rev:$ $Date:$
 */
public class VMSmokeTest extends AbstractSmokeTest {

    private static VMBroker broker;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        broker = new VMBroker("CLUSTER");
        broker.start();
    }

    @Override
    protected Dispatcher newDispatcher(String nodeName) throws Exception {
        return new VMDispatcher(broker, nodeName, new URIEndPoint(URI.create("mock")));
    }
    
}
