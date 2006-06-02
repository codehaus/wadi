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
package org.codehaus.wadi.replication;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.sql.DataSource;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.axiondb.jdbc.AxionDataSource;
import org.codehaus.wadi.Collapser;
import org.codehaus.wadi.SessionPool;
import org.codehaus.wadi.Contextualiser;
import org.codehaus.wadi.Evicter;
import org.codehaus.wadi.InvocationProxy;
import org.codehaus.wadi.PoolableInvocationWrapperPool;
import org.codehaus.wadi.EndPoint;
import org.codehaus.wadi.ReplicaterFactory;
import org.codehaus.wadi.SessionIdFactory;
import org.codehaus.wadi.Streamer;
import org.codehaus.wadi.ValuePool;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.impl.AbstractExclusiveContextualiser;
import org.codehaus.wadi.impl.AlwaysEvicter;
import org.codehaus.wadi.impl.ClusterContextualiser;
import org.codehaus.wadi.impl.ClusteredManager;
import org.codehaus.wadi.impl.DatabaseStore;
import org.codehaus.wadi.impl.DummyContextualiser;
import org.codehaus.wadi.impl.DummyManagerConfig;
import org.codehaus.wadi.impl.DummyRelocater;
import org.codehaus.wadi.impl.ExclusiveStoreContextualiser;
import org.codehaus.wadi.impl.HashingCollapser;
import org.codehaus.wadi.impl.MemoryContextualiser;
import org.codehaus.wadi.impl.MemoryReplicaterFactory;
import org.codehaus.wadi.impl.NeverEvicter;
import org.codehaus.wadi.impl.SerialContextualiser;
import org.codehaus.wadi.impl.SharedStoreContextualiser;
import org.codehaus.wadi.impl.SimpleSessionPool;
import org.codehaus.wadi.impl.SimpleStreamer;
import org.codehaus.wadi.impl.SimpleValuePool;
import org.codehaus.wadi.impl.TomcatSessionIdFactory;
import org.codehaus.wadi.impl.Utils;
import org.codehaus.wadi.test.MockInvocation;
import org.codehaus.wadi.web.AttributesFactory;
import org.codehaus.wadi.web.WebSessionPool;
import org.codehaus.wadi.web.WebSessionWrapperFactory;
import org.codehaus.wadi.web.impl.AbstractReplicableSession;
import org.codehaus.wadi.web.impl.AtomicallyReplicableSessionFactory;
import org.codehaus.wadi.web.impl.DistributableAttributesFactory;
import org.codehaus.wadi.web.impl.DistributableValueFactory;
import org.codehaus.wadi.web.impl.DummyRouter;
import org.codehaus.wadi.web.impl.DummyStatefulHttpServletRequestWrapperPool;
import org.codehaus.wadi.web.impl.StandardHttpProxy;
import org.codehaus.wadi.web.impl.StandardSessionWrapperFactory;
import org.codehaus.wadi.web.impl.WebEndPoint;
import org.codehaus.wadi.web.impl.WebSessionToSessionPoolAdapter;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision: 1553 $
 */
public abstract class AbstractTestReplication extends TestCase {

	protected Log _log = LogFactory.getLog(getClass());

	public AbstractTestReplication(String arg0) {
		super(arg0);
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testReplication(Dispatcher dispatcher) throws Exception {

		int sweepInterval=1000*60*60*24; // 1 eviction/day
		boolean strictOrdering=true;
		Streamer streamer=new SimpleStreamer();
		Collapser collapser=new HashingCollapser(100, 1000);

		// Terminator
		Contextualiser terminator=new DummyContextualiser();
		Streamer sessionStreamer=new SimpleStreamer();

		// DB
		String url="jdbc:axiondb:WADI";
		DataSource ds=new AxionDataSource(url);
		String storeTable="SESSIONS";
		DatabaseStore store=new DatabaseStore(url, ds, storeTable, false, true, true);
		boolean clean=true;
		Contextualiser db=new SharedStoreContextualiser(terminator, collapser, clean, store);

		// Cluster
		Contextualiser cluster=new ClusterContextualiser(db, collapser, new DummyRelocater());

		// Disc
		Evicter devicter=new NeverEvicter(sweepInterval, strictOrdering);
		Map dmap=new HashMap();
		File dir=Utils.createTempDirectory("wadi", ".test", new File("/tmp"));
		Contextualiser spool=new ExclusiveStoreContextualiser(cluster, collapser, false, devicter, dmap, sessionStreamer, dir);

		Map mmap=new HashMap();

		Contextualiser serial=new SerialContextualiser(spool, collapser, mmap);

		WebSessionPool sessionPool=new SimpleSessionPool(new AtomicallyReplicableSessionFactory());

		// Memory
		Evicter mevicter=new AlwaysEvicter(sweepInterval, strictOrdering);
		SessionPool contextPool=new WebSessionToSessionPoolAdapter(sessionPool);
		PoolableInvocationWrapperPool requestPool=new DummyStatefulHttpServletRequestWrapperPool();
		AbstractExclusiveContextualiser memory=new MemoryContextualiser(serial, mevicter, mmap, streamer, contextPool, requestPool);

		// Manager
		int numPartitions=72;
		AttributesFactory attributesFactory=new DistributableAttributesFactory();
		ValuePool valuePool=new SimpleValuePool(new DistributableValueFactory());
		WebSessionWrapperFactory wrapperFactory=new StandardSessionWrapperFactory();
		SessionIdFactory idFactory=new TomcatSessionIdFactory();
		ReplicaterFactory replicaterfactory=new MemoryReplicaterFactory(numPartitions);
		EndPoint location = new WebEndPoint(new InetSocketAddress("localhost", 8080));
		InvocationProxy proxy=new StandardHttpProxy("jsessionid");
		ClusteredManager manager=new ClusteredManager(sessionPool, attributesFactory, valuePool, wrapperFactory, idFactory, memory, memory.getMap(), new DummyRouter(), true, streamer, true, replicaterfactory, location, proxy, dispatcher, 24, collapser);
//		manager.setSessionListeners(new HttpSessionListener[]{});
		//manager.setAttributelisteners(new HttpSessionAttributeListener[]{});
		manager.init(new DummyManagerConfig());

		manager.start();
		//mevicter.stop(); // we'll run it by hand...
		//devicter.stop();

		_log.info("CREATING SESSION");
		AbstractReplicableSession session=(AbstractReplicableSession)manager.create();
		String foo="bar";
		session.setAttribute("foo", foo);
		String name=session.getId();
		assertTrue(mmap.size()==1);
		assertTrue(dmap.size()==0);

		_log.info("TOUCHING SESSION");
		long lat=session.getLastAccessedTime();
		memory.contextualise(new MockInvocation(null, null, new FilterChain() { public void doFilter(ServletRequest req, ServletResponse res){_log.info("running request");} }), name, null, null, false);
		assert(lat!=session.getLastAccessedTime());
		session=(AbstractReplicableSession)mmap.get(name);
		assertTrue(mmap.size()==1);
		assertTrue(dmap.size()==0);

		_log.info("DEMOTING SESSION to short-term SPOOL");
		mevicter.evict();
		assertTrue(mmap.size()==0);
		assertTrue(dmap.size()==1);

//		_log.info("DEMOTING SESSION to long-term STORE");
//		manager.stop();
//		assertTrue(mmap.size()==0);
//		assertTrue(dmap.size()==0);
//
//		_log.info("PROMOTING SESSION to short-term SPOOL");
//		manager.start();
//		assertTrue(mmap.size()==0);
//		assertTrue(dmap.size()==1);
//
		_log.info("PROMOTING SESSION to Memory");
		memory.contextualise(new MockInvocation(null, null, new FilterChain() { public void doFilter(ServletRequest req, ServletResponse res){_log.info("running request");} }), name, null, null, false);
		session=(AbstractReplicableSession)mmap.get(name);
		assertTrue(session.getAttribute("foo")!=foo);
		assertTrue(session.getAttribute("foo").equals(foo));
		assertTrue(mmap.size()==1);
		assertTrue(dmap.size()==0);

		_log.info("DESTROYING SESSION");
		manager.destroy(session);
		assertTrue(mmap.size()==0);
		assertTrue(dmap.size()==0);

		manager.stop();

		dir.delete();
	}

}
