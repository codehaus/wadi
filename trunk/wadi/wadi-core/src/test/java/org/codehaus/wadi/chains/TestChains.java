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

import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.vm.VMBroker;
import org.codehaus.wadi.group.vm.VMDispatcher;

// IM fails - give up, we have lost the invocation
// IM->PM
// PM fails - wait for Partition reincarnation and address new PM
// PM->SM
// SM fails - PM waits for Session reincarnation, then either continues or signals failure to IM
// SM->IM
// IM fails - we have lost the invocation - signal failure to PM
// IM->SM
// SM fails - maintain lock on Session, approach PM (where missing Session will be reincarnated) and negotiate ownership (pre or post-emptively) - SM->PM skipped.
// SM->PM
// PM fails - we will be asked to aid in reconstitution of reincarnated Partition's content - so we needn't do anymore.


// IDEAS
// use leases and async callbacks instead of locks/timeouts and waiting threads.
// at the end a Lease, within it's scope, we need to receive a callback, run some logic and decide to renew the lease or allow it to lapse.

// as each node takes responsibility for downstream actions, it must remember the chain id, so that if anyone upstream tries to restart the same chain, we do not get two identical chains competing.
/**
 *
 * I'm going to isolate and test various links in various message chains here...whilst refactoring the whole area...
 *
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class TestChains extends TestCase {

	public TestChains(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testMessageChains() throws Exception {
		assertTrue(true);
		
		VMBroker broker=new VMBroker("TestBroker");
		long inactiveTime=2500;
		Dispatcher d1=new VMDispatcher(broker, "red", inactiveTime);
		Dispatcher d2=new VMDispatcher(broker, "green", inactiveTime);
		
		d1.start();
		d2.start();
		d2.stop();
		d1.stop();
	}
}
