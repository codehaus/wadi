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

import junit.framework.TestCase;

import org.codehaus.wadi.Immoter;
import org.codehaus.wadi.Invocation;
import org.codehaus.wadi.InvocationException;
import org.codehaus.wadi.Motable;
import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.ClusterEvent;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.DispatcherConfig;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.impl.SimplePartitionMapper;
import org.codehaus.wadi.location.PartitionManager;
import org.codehaus.wadi.location.PartitionManagerConfig;
import org.codehaus.wadi.location.impl.PartitionManagerJoiningEvent;
import org.codehaus.wadi.location.impl.SimplePartitionManager;
import org.codehaus.wadi.location.impl.SimplePartitionManager.Callback;

import EDU.oswego.cs.dl.util.concurrent.Sync;

//Put this off until later - no current need to disentangle DIndex and PartitionManager, althought it will have to be done eventually...

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision: 1563 $
 */
public abstract class AbstractTestPartitionManager extends TestCase {

	public static void main(String[] args) {
	}

	public AbstractTestPartitionManager(String arg0) {
		super(arg0);
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	protected abstract Dispatcher createDispatcher(String clusterName, String nodeName, long timeout) throws Exception;
	
	protected PartitionManager create(String nodeName) throws Exception {
		String clusterName="WADI";
		long inactiveTime=5000L;
		Dispatcher dispatcher=createDispatcher(clusterName, nodeName, inactiveTime);
		DispatcherConfig dc=new DispatcherConfig() {

			public String getContextPath() {
				return "/";
			}

		};
		dispatcher.init(dc);
		dispatcher.start();

		Callback callback = new Callback() {
			public void onPartitionEvacuationRequest(ClusterEvent event) {
			}

			public void onPartitionManagerJoiningEvent(PartitionManagerJoiningEvent joiningEvent) {
			}
		};

		int numPartitions=2;
		return new SimplePartitionManager(dispatcher, numPartitions, callback, new SimplePartitionMapper(numPartitions));
	}

	public void testPartitionManager() throws Exception {

		PartitionManager pm1=create("red");
		PartitionManager pm2=create("green");

		PartitionManagerConfig pmc=new PartitionManagerConfig() {

			protected long _birthTime=System.currentTimeMillis();
			
			public void findRelevantSessionNames(int numPartitions, Collection[] resultSet) {
			}

            public Peer getCoordinator() {
                return null;
            }

			public long getInactiveTime() {
				return 0;
			}

			public boolean contextualise(Invocation invocation, String id, Immoter immoter, Sync motionLock, boolean exclusiveOnly) throws InvocationException {
				return false;
			}

			public String getPeerName(Address address) {
				return null;
			}

			public Immoter getImmoter(String name, Motable immotable) {
				return null;
			}

			public Sync getInvocationLock(String name) {
				return null;
			}

			public long getBirthTime() {
				return _birthTime;
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
