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
import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.Cluster;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.Envelope;
import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.group.PeerInfo;
import org.codehaus.wadi.group.ServiceEndpoint;
import org.codehaus.wadi.group.impl.AbstractMsgDispatcher;
import org.codehaus.wadi.group.impl.RendezVousMsgDispatcher;
import org.codehaus.wadi.location.Partition;
import org.codehaus.wadi.location.PartitionConfig;
import org.codehaus.wadi.location.impl.LocalPartition;
import org.codehaus.wadi.location.session.MoveIMToPM;
import org.codehaus.wadi.location.session.MovePMToIM;

/**
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class TestIMToPMLink extends ChainTestCase {

	public TestIMToPMLink(String name) {
		super(name);
	}

	static class MockPartitionConfig implements PartitionConfig {

		protected Dispatcher _dispatcher;

		public MockPartitionConfig(Dispatcher dispatcher) {
			_dispatcher=dispatcher;
		}

		public Cluster getCluster() {
			return _dispatcher.getCluster();
		}

		public Dispatcher getDispatcher() {
			return _dispatcher;
		}

		public long getInactiveTime() {
			return _dispatcher.getCluster().getInactiveTime();
		}

		public String getLocalPeerName() {
			return _dispatcher.getCluster().getLocalPeer().getName();
		}

		public String getPeerName(Address address) {
			return _dispatcher.getCluster().getPeerFromAddress(address).getName();
		}

	}

	/**
	 * Test locking around a Partition as its location becomes known/unknown...
	 */
//	public void testUnknownPartition() {
//		int index=0;
//		UnknownPartition unknown=new UnknownPartition(index);
//		PartitionFacade partition=new PartitionFacade(index, unknown, new MockPartitionConfig(_dispatcher1));
//		// since we initialised it with an unknown location, it should be locked.
//		assertTrue(partition.isLocked());
//		LocalPartition known=new LocalPartition(_dispatcher1, index, 5000);
//		partition.setContent(known);
//		assertTrue(!partition.isLocked());
//	}

	protected WatermarkedCounter _count=new WatermarkedCounter();

	class CountedPeer implements Peer {

		protected Peer _peer;

		public CountedPeer(Peer peer) {
			_peer=peer;
		}

		public Address getAddress() {
			_count.increment();
			//Thread.yield();
			_count.decrement();
			return _peer.getAddress();
		}

		public String getName() {
			return _peer.getName();
		}

		public PeerInfo getPeerInfo() {
			return _peer.getPeerInfo();
		}

	}

	class CountedPartition extends LocalPartition {

		public CountedPartition(Dispatcher dispatcher, int index, long timeout) {
			super(dispatcher, index, timeout);
		}

		public Address getAddress() {
			_count.increment();
			//Thread.yield();
			_count.decrement();
			return super.getAddress();
		}

	}

	class PartitionReader implements Runnable {

		int _iters;
		Partition _partition;

		public PartitionReader(int iters, Partition partition) {
			_iters=iters;
			_partition=partition;
		}

		public void run() {
			for (int i=0; i<_iters; i++) {
				_partition.getAddress();
				//Thread.yield();
			}
		}

	}

	static class WatermarkedCounter {

		protected int _count; // a count the is inc and decremented
		protected int _total; // the total number of incs
		protected int _watermark; // the count's high watermark

		public synchronized int increment() {
			if (++_count>_watermark) ++_watermark;
			_total++;
			return _count;
		}

		public synchronized int decrement() {
			return --_count;
		}

		public synchronized int getCount() {
			return _count;
		}

		public synchronized int getWatermark() {
			return _watermark;
		}

		public synchronized int getTotal() {
			return _total;
		}
	}

	/**
	 * Test the changing of a Partition's location whilst it is in use - does locking all work properly ?
	 */
//	public void testPartitionSwap() throws Exception {
//		int index=0;
//		UnknownPartition unknown=new UnknownPartition(index);
//		PartitionFacade partition=new PartitionFacade(index, unknown, new MockPartitionConfig(_dispatcher1));
//		// since we initialised it with an unknown location, it should be locked.
//		assertTrue(partition.isLocked());
//		int numIters=100;
//		int numThreads=100;
//		// start off the readers...
//		Thread[] threads=new Thread[numThreads];
//		for (int i=0; i<numThreads; i++)
//			(threads[i]=new Thread(new PartitionReader(numIters, partition))).start();
//		// do some writing...
//		LocalPartition local=new CountedPartition(_dispatcher1, index, 5000);
//		Peer remote=new CountedPeer(_dispatcher2.getCluster().getLocalPeer()); // hmmm... - this is actually a LocalPeer - NB
//		assertTrue(_count.getCount()==0);
//		for (int i=0; i<numIters; i++) {
//			Thread.yield();
//			partition.setContent(local);
//			Thread.yield();
//			partition.acquire();
//			partition.release(remote);
//		}
//		// tidy up...
//		for (int i=0; i<numThreads; i++)
//			threads[i].join();
//		assertTrue(!partition.isLocked());
//		// check counter again
//		_log.info("count: "+_count.getCount());
//		assertTrue(_count.getTotal()==numIters*numThreads);
//		assertTrue(_count.getCount()==0);
//		_log.info("max concurrency: "+_count.getWatermark());
//	}

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
		LocalPeer localPeer = _dispatcher1.getCluster().getLocalPeer();
		boolean relocateSession=true;
		boolean relocateInvocation=false;
		Message message=new MoveIMToPM(localPeer, sessionName, relocateSession, relocateInvocation);
		long timeout=10000;
		// If PM fails - wait for Partition reincarnation and then address new PM...
		try {
			_log.info("sending request...");
			Envelope envelope=_dispatcher1.exchangeSendLink(_dispatcher2.getCluster().getLocalPeer(), message, timeout, 3);
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
