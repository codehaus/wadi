/**
 *
 * Copyright 2003-2006 Core Developers Network Ltd.
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
package org.codehaus.wadi.chains;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.group.Cluster;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.vm.VMBroker;
import org.codehaus.wadi.group.vm.VMDispatcher;

/**
 * A TestCase that sets up and tears down a pair of Dispatchers around each test.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class ChainTestCase extends TestCase {

	protected Log _log=LogFactory.getLog(getClass());

	public ChainTestCase(String name) {
		super(name);
	}

	long _inactiveTime=2500;
	Dispatcher _dispatcher1;
	Cluster _cluster1;
	Dispatcher _dispatcher2;
	Cluster _cluster2;

    public void testEmpty() {
        
    }
    
	protected void setUp() throws Exception {
		super.setUp();
		// TODO - use unique names
		VMBroker broker=new VMBroker("TestBroker");
		_dispatcher1=new VMDispatcher(broker, "dispatcher1", _inactiveTime);
		_cluster1=_dispatcher1.getCluster();
		_dispatcher1.start();
		_cluster1.waitOnMembershipCount(1, _inactiveTime);
		_log.info("1 peer...");
		_dispatcher2=new VMDispatcher(broker, "dispatcher2", _inactiveTime);
		_cluster2=_dispatcher2.getCluster();
		_dispatcher2.start();
		_cluster1.waitOnMembershipCount(2, _inactiveTime);
		_cluster2.waitOnMembershipCount(2, _inactiveTime);
		_log.info("2 peers...");
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		_cluster1.waitOnMembershipCount(2, _inactiveTime);
		_cluster2.waitOnMembershipCount(2, _inactiveTime);
		_log.info("2 peers...");
		_dispatcher2.stop();
		_dispatcher2=null;
		_cluster1.waitOnMembershipCount(1, _inactiveTime); // TODO: this is probably not stricly working
		_log.info("1 peer...");
		_dispatcher1.stop();
		_dispatcher1=null;
		_log.info("0 peers...");
	}

}
