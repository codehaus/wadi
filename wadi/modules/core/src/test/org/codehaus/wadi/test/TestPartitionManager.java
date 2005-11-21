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
package org.codehaus.wadi.test;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.activecluster.ClusterEvent;
import org.codehaus.wadi.dindex.PartitionManager;
import org.codehaus.wadi.dindex.PartitionManagerConfig;
import org.codehaus.wadi.dindex.impl.SimplePartitionManager;
import org.codehaus.wadi.dindex.impl.SimplePartitionManager.Callback;
import org.codehaus.wadi.gridstate.Dispatcher;
import org.codehaus.wadi.gridstate.DispatcherConfig;
import org.codehaus.wadi.gridstate.activecluster.ActiveClusterDispatcher;

import junit.framework.TestCase;

// Put this off until later - no current need to disentangle DIndex and PartitionManager, althought it will have to be done eventually...

public class TestPartitionManager extends TestCase {
	
	public static void main(String[] args) {
	}
	
	public TestPartitionManager(String arg0) {
		super(arg0);
	}
	
	protected void setUp() throws Exception {
		super.setUp();
	}
	
	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	protected PartitionManager create(String nodeName) throws Exception {
		String clusterName="WADI";
		String clusterUri="vm://localhost";
		long inactiveTime=5000L;
		Dispatcher dispatcher=new ActiveClusterDispatcher(nodeName, clusterName, clusterUri, inactiveTime);
		DispatcherConfig dc=new DispatcherConfig() {
			
			public String getContextPath() {
				return "/";
			}
			
		};
		dispatcher.init(dc);
		dispatcher.start();

		Map distributedState=new HashMap();
		Callback callback=new Callback() {
			
			public void onNodeRemoved(ClusterEvent event) {
				// TODO Auto-generated method stub
				
			}
		};
		
		return new SimplePartitionManager(dispatcher, 2, distributedState, callback);
	}	
	
	public void testPartitionManager() throws Exception {
		
		PartitionManager pm1=create("red");
		PartitionManager pm2=create("green");
		
		PartitionManagerConfig pmc=new PartitionManagerConfig() {
			
			public void findRelevantSessionNames(int numPartitions, Collection[] resultSet) {
				// TODO Auto-generated method stub
				
			}
			
		};
		
		pm1.init(pmc);
		pm2.init(pmc);
		pm1.start();
		pm2.start();
		
		Thread.sleep(10*1000);
		
		assertTrue(true);
	}
	
}
