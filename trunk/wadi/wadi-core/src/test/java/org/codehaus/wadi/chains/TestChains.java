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

import org.codehaus.wadi.Message;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.Envelope;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.ServiceEndpoint;
import org.codehaus.wadi.group.impl.AbstractMsgDispatcher;
import org.codehaus.wadi.group.impl.RendezVousMsgDispatcher;
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
public class TestChains extends ChainTestCase {

	public TestChains(String name) {
		super(name);
	}

	/* If a message is not answered within a given timeframe, it may be repeated
	 * until answered (giving time for a service fail-over to complete) or until
	 * a failure condition is recognised.
	 * All messages that correspond to the same Link in a MessageChain
	 * must carry the same source correlationId, so that a reply to the e.g. the
	 * the first, may be taken as a reply to the second etc...
	 * The recipient of such a message needs to remember which Links are currently
	 * being processed, so that they can avoid starting a second attempt at e.g.
	 * an upstream Chain because the sender downstream is getting impatient.
	 *
	 */

	//-----------------------------------------------------------------
	// TODO - this stuff should be in wadi-group

    static class MyEndpoint extends AbstractMsgDispatcher {

    	protected int _count;
    	protected String _correlationId;

        MyEndpoint(Dispatcher dispatcher, Class type) {
            super(dispatcher, type);
        }

        public int getCount() {
        	return _count;
        }

        public void dispatch(Envelope om) throws Exception {
        	_count++;

        	if (_correlationId==null)
        		_correlationId=om.getSourceCorrelationId();
        	else {
        		assertTrue(_correlationId.equals(om.getSourceCorrelationId()));
        		_dispatcher.reply(om, new MyMessage());
        	}
        }

    }

    static class MyMessage implements Message {
    };

    // TODO: no stacktrace if everything works ok
    // TODO: count failed rvs

    /**
	 * Test that Messages corresponding to the same 'Link' in a MessageChain, carry
	 * the same source correlation Id.
	 */
	public void testLinkSourceCorrelationId() throws Exception {
		_dispatcher1.register(new RendezVousMsgDispatcher(_dispatcher1, MyMessage.class));
		MyEndpoint endpoint=new MyEndpoint(_dispatcher2, MyMessage.class);
		_dispatcher2.register(endpoint);
		Envelope response=_dispatcher1.exchangeSendLink(_cluster2.getLocalPeer().getAddress(), new MyMessage(), 100, 2);
		assertTrue(endpoint.getCount()==2);
		assertTrue(response.getPayload().getClass()==MyMessage.class);
	}

	//-----------------------------------------------------------------

	// I guess that we will have to maintain a list of running message handlers,
	// keyed by sourceCorrelationId on each recipient. If a message with the same id
	// as a currently running message handler arrives, it should be logged (trace?)
	// and discarded. Maintenance of this table will have ramifications for the
	// concurrency of message handling....

	/**
	 * Test that duplicate Link Messages arriving are ignored, to avoid creating duplicate
	 * downstream processes unnecessarily.
	 */
	public void testMergeSourceCorrelationId() throws Exception {
		_dispatcher1.register(new RendezVousMsgDispatcher(_dispatcher1, MyMessage.class));
		MyEndpoint endpoint=new MyEndpoint(_dispatcher2, MyMessage.class);
		_dispatcher2.register(endpoint);
		Envelope response=_dispatcher1.exchangeSendLink(_cluster2.getLocalPeer().getAddress(), new MyMessage(), 100, 2);
		assertTrue(endpoint.getCount()==2);
		assertTrue(response.getPayload().getClass()==MyMessage.class);
		// TODO..
	}

	//-----------------------------------------------------------------
	// this stuff should probably move out to a specific IM->PM testsuite

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

    // client sends message to remote Partition
    // Partition receives it and quits without answering - mimicking failure
    // client continues trying until Partition is reincarnated (upon itself) and chain completes

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
		// If PM fails - wait for Partition reincarnation and then address new PM...
		try {
			_log.info("sending request...");
			Envelope envelope=_dispatcher1.exchangeSendLink(_dispatcher2.getCluster().getLocalPeer().getAddress(), message, timeout, 3);
			assertTrue(envelope.getPayload().getClass()==MovePMToIM.class);
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

}
