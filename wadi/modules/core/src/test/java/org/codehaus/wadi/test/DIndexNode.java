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

import javax.jms.Destination;

import org.apache.activecluster.Cluster;
import org.apache.activecluster.Node;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.ContextPool;
import org.codehaus.wadi.Contextualiser;
import org.codehaus.wadi.Immoter;
import org.codehaus.wadi.InvocationContext;
import org.codehaus.wadi.InvocationException;
import org.codehaus.wadi.Motable;
import org.codehaus.wadi.PoolableInvocationWrapperPool;
import org.codehaus.wadi.SessionPool;
import org.codehaus.wadi.Streamer;
import org.codehaus.wadi.StreamerConfig;
import org.codehaus.wadi.dindex.PartitionManagerConfig;
import org.codehaus.wadi.dindex.impl.DIndex;
import org.codehaus.wadi.gridstate.DispatcherConfig;
import org.codehaus.wadi.gridstate.PartitionMapper;
import org.codehaus.wadi.gridstate.activecluster.ActiveClusterDispatcher;
import org.codehaus.wadi.impl.DistributableSessionFactory;
import org.codehaus.wadi.impl.DummyContextualiser;
import org.codehaus.wadi.impl.MemoryContextualiser;
import org.codehaus.wadi.impl.NeverEvicter;
import org.codehaus.wadi.impl.SessionToContextPoolAdapter;
import org.codehaus.wadi.impl.SimplePartitionMapper;
import org.codehaus.wadi.impl.SimpleSessionPool;
import org.codehaus.wadi.impl.SimpleStreamer;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;
import EDU.oswego.cs.dl.util.concurrent.Latch;
import EDU.oswego.cs.dl.util.concurrent.Sync;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class DIndexNode implements DispatcherConfig, PartitionManagerConfig {

	protected final Log _log=LogFactory.getLog(getClass());

	//protected final String _clusterUri="peer://org.codehaus.wadi";
	//protected final String _clusterUri="tcp://localhost:61616";
	//protected final String _clusterUri="tcp://smilodon:61616";
	protected final String _clusterUri="vm://localhost";
	protected final String _clusterName="ORG.CODEHAUS.WADI.TEST";
	protected final ActiveClusterDispatcher _dispatcher;
	protected final Map _distributedState=new ConcurrentHashMap();
	protected final Contextualiser _contextualiser;
	protected final String _nodeName;
	protected final PartitionMapper _mapper;
	protected final int _numPartitions;
	protected final Map _entries;
	protected final DistributableSessionFactory _distributableSessionFactory=new DistributableSessionFactory();
	protected final SessionPool _distributableSessionPool=new SimpleSessionPool(_distributableSessionFactory);
	protected final PoolableInvocationWrapperPool _requestPool=new MyDummyHttpServletRequestWrapperPool();
	protected final ContextPool _distributableContextPool=new SessionToContextPoolAdapter(_distributableSessionPool);
	protected final Streamer _streamer;
	protected DIndex _dindex;

	public DIndexNode(String nodeName, int numPartitions, PartitionMapper mapper, long inactiveTime) {
		_nodeName=nodeName;
		_dispatcher=new ActiveClusterDispatcher(_nodeName, _clusterName, _clusterUri, inactiveTime);
		_numPartitions=numPartitions;
		//System.setProperty("activemq.persistenceAdapterFactory", VMPersistenceAdapterFactory.class.getName()); // peer protocol sees this
		// TODO - figure out how to tun off persistance
		_mapper=mapper;
		_streamer=new SimpleStreamer();
		_streamer.init(new StreamerConfig(){public ClassLoader getClassLoader() {return getClass().getClassLoader();}});
		_distributableSessionPool.init(new DummyDistributableSessionConfig());
		_entries=new HashMap();
		Contextualiser dummy=new DummyContextualiser();
		_contextualiser=new MemoryContextualiser(dummy, new NeverEvicter(30000,false), _entries, _streamer, _distributableContextPool, _requestPool);
	}

	// DIndexNode API

	public void start() throws Exception {
		_dispatcher.init(this);
		_dindex=new DIndex(_nodeName, _numPartitions, _dispatcher.getInactiveTime(), _dispatcher, _distributedState, _mapper);
		_dindex.init(this);
		_log.info("starting Cluster...");
		_dispatcher.setDistributedState(_distributedState);
		_dispatcher.start();
		_log.info("...Cluster started");
		_dindex.start();
	}

	public void stop() throws Exception {
		_dindex.stop();
	}

	public DIndex getDIndex() {
		return _dindex;
	}

	public Cluster getCluster() {
		return _dispatcher.getCluster();
	}

	public void insert(Object key, Object value, long timeout) {
		_dindex.insert((String)key, timeout);
		_entries.put(key, value);
	}

	public Object get(String key) {
		return _entries.get(key);
	}

	// PartitionManagerConfig API

	public void findRelevantSessionNames(int numPartitions, Collection[] resultSet) {
		_log.warn("findRelevantSessionNames() - NYI");
	}

	public Node getCoordinatorNode() {
		throw new UnsupportedOperationException("NYI");
	}

	public long getInactiveTime() {
		throw new UnsupportedOperationException("NYI");
	}

	public boolean contextualise(InvocationContext invocationContext, String id, Immoter immoter, Sync motionLock, boolean exclusiveOnly) throws InvocationException {
		return _contextualiser.contextualise(invocationContext, id, immoter, motionLock, exclusiveOnly);
	}

	public Immoter getImmoter(String name, Motable immotable) {
		return _contextualiser.getDemoter(name, immotable);
	}

	public String getNodeName(Destination destination) {
		return _dispatcher.getNodeName(destination);
	}

	// DispatcherConfig API

	public String getContextPath() {
		return "/";
	}

	//-----------------------------------------------------------

	protected static Latch _latch0=new Latch();
	protected static Latch _latch1=new Latch();

	protected static Object _exitSync = new Object();

	public static void main(String[] args) throws Exception {
		String nodeName=args[0];
		int numPartitions=Integer.parseInt(args[1]);

		try {
			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					System.err.println("SHUTDOWN");
					_latch0.release();
					try {
						_latch1.acquire();
					} catch (InterruptedException e) {
						Thread.interrupted();
					}
				}
			});

			DIndexNode node=new DIndexNode(nodeName, numPartitions, new SimplePartitionMapper(numPartitions), 5000L);
			node.start();

			_latch0.acquire();

			node.stop();
		} finally {
			_latch1.release();
		}
	}

	public Sync getInvocationLock(String name) {
		// TODO Auto-generated method stub
		return null;
	}

}
