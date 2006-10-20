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
import org.codehaus.wadi.Message;
import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.Cluster;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.Envelope;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.ServiceEndpoint;
import org.codehaus.wadi.group.impl.AbstractMsgDispatcher;
import org.codehaus.wadi.group.impl.RendezVousMsgDispatcher;
import org.codehaus.wadi.group.vm.VMBroker;
import org.codehaus.wadi.group.vm.VMDispatcher;
import org.codehaus.wadi.location.session.MoveIMToPM;
import org.codehaus.wadi.location.session.MovePMToIM;

//IM fails - give up, we have lost the invocation
//IM->PM
//PM fails - wait for Partition reincarnation and address new PM
//PM->SM
//SM fails - PM waits for Session reincarnation, then either continues or signals failure to IM
//SM->IM
//IM fails - we have lost the invocation - signal failure to PM
//IM->SM
//SM fails - maintain lock on Session, approach PM (where missing Session will be reincarnated) and negotiate ownership (pre or post-emptively) - SM->PM skipped.
//SM->PM
//PM fails - we will be asked to aid in reconstitution of reincarnated Partition's content - so we needn't do anymore.


//IDEAS
//use leases and async callbacks instead of locks/timeouts and waiting threads.
//at the end a Lease, within it's scope, we need to receive a callback, run some logic and decide to renew the lease or allow it to lapse. (See ExtendableLease)

//as each node takes responsibility for downstream actions, it must remember the chain id, so that if anyone upstream tries to restart the same chain, we do not get two identical chains competing.
/**
 *
 * I'm going to isolate and test various links in various message chains here...whilst refactoring the whole area...
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class TestChains extends TestCase {

	protected Log _log=LogFactory.getLog(getClass());

	public TestChains(String name) {
		super(name);
	}

	long _inactiveTime=2500;
	Dispatcher _dispatcher1;
	Cluster _cluster1;
	Dispatcher _dispatcher2;
	Cluster _cluster2;

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

	protected Envelope exchangeSendLoop(Dispatcher dispatcher, Address address, Message message, long timeout, int retries) throws MessageExchangeException {
		for (int attempts=0; attempts<retries; attempts++) {
			try {
				return dispatcher.exchangeSend(address, message, timeout);
			} catch (MessageExchangeException e) {
				_log.warn("no reply to message within timeframe - retrying ("+attempts+"/"+retries+")", e);
			}
		}
		throw new MessageExchangeException("no reply to repeated message");
	}

    class IMToPMEndpoint extends AbstractMsgDispatcher {
        
        IMToPMEndpoint(Dispatcher dispatcher, Class type) {
            super(dispatcher, type);
        }
        
        public void dispatch(Envelope om) throws Exception {
			_log.info("replying...");
			_dispatcher.reply(om, new MovePMToIM());
			_log.info("...replied");
        }
        
    }
    
	public void testIMToPM() throws Exception {
		ServiceEndpoint sync=new IMToPMEndpoint(_dispatcher2, MoveIMToPM.class);
		_dispatcher2.register(sync);
		ServiceEndpoint rv=new RendezVousMsgDispatcher(_dispatcher1, MovePMToIM.class);
		_dispatcher1.register(rv);
		
		String sessionName="xxx";
		String peerName=_dispatcher1.getCluster().getLocalPeer().getName();
		boolean relocateSession=true;
		boolean relocateInvocation=false;
		Message message=new MoveIMToPM(sessionName, peerName, relocateSession, relocateInvocation);
		long timeout=10000;
		try {
			_log.info("sending request...");
			Envelope envelope=exchangeSendLoop(_dispatcher1, _dispatcher2.getCluster().getLocalPeer().getAddress(), message, timeout, 3);
			_log.info("...response received");
		} catch (MessageExchangeException e) {
			// ok - for the moment...
			_log.error("no response - timed out");
		}

		_dispatcher2.unregister(sync, 10, 500); // what are these params for ?
		_dispatcher1.unregister(rv, 10, 5000); // what are these params for ?

		// TODO
		// register a Listener on other Dispatcher
		// Do not respond to e.g. first/second message
		// Do respond to third message
		// Use some shared code to resend request message
		// figure out how to identify a resent message on receipt side (remember messages being processed?)
		// etc...

		// TODO
		// we need an exchangeSend() that does not throw Exception on timeout to loop efficiently...
	}

	public void testPMToSM() throws Exception {
		assertTrue(true);
	}

	public void testSMToIM() throws Exception {
		assertTrue(true);
	}
    
}
