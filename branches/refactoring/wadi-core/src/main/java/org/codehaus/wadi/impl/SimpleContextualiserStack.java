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
package org.codehaus.wadi.impl;

//Thoughts -

//invalidation is tricky - stuff invalidated on disc needs may need to be unmarshalled so that the correct listeners may be notified...

//we need a JDBC passivation store

//can we even store an invalidated session without breaking listener model - consider...

import java.io.File;
import java.net.InetAddress;
import java.util.Map;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.Collapser;
import org.codehaus.wadi.Contextualiser;
import org.codehaus.wadi.ContextualiserConfig;
import org.codehaus.wadi.Emoter;
import org.codehaus.wadi.Evicter;
import org.codehaus.wadi.Immoter;
import org.codehaus.wadi.Invocation;
import org.codehaus.wadi.InvocationException;
import org.codehaus.wadi.Motable;
import org.codehaus.wadi.PartitionMapper;
import org.codehaus.wadi.PoolableInvocationWrapperPool;
import org.codehaus.wadi.Relocater;
import org.codehaus.wadi.SessionPool;
import org.codehaus.wadi.Streamer;
import org.codehaus.wadi.web.impl.DummyStatefulHttpServletRequestWrapperPool;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;
import EDU.oswego.cs.dl.util.concurrent.Sync;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class SimpleContextualiserStack implements Contextualiser {

	protected final Log _log = LogFactory.getLog(getClass());

	protected final Streamer _streamer;
	protected final Collapser _collapser;

	protected final DummyContextualiser _dummy;

	protected final DataSource _databaseDataSource;
	protected final String _databaseTable;
	protected Evicter _databaseEvicter;
	protected final SharedStoreContextualiser _database;

	protected final Relocater _clusterRelocater;
	protected final ClusterContextualiser _cluster;

	protected final Pattern _statelessMethods;
	protected final boolean _statelessMethodFlag;
	protected final Pattern _statelessURIs;
	protected final boolean _statelessURIFlag;
	protected final StatelessContextualiser _stateless;

	protected final File _discDirectory;
	protected final Evicter _discEvicter;
	protected final Map _discMap;
	protected final ExclusiveStoreContextualiser _disc;

	protected final SerialContextualiser _serial;

	protected final SessionPool _memoryPool;
	protected final PoolableInvocationWrapperPool _requestPool;
	protected final Evicter _memoryEvicter;
	protected final Map _memoryMap;
	protected final MemoryContextualiser _memory;

	public SimpleContextualiserStack(Map sessionMap, SessionPool pool, DataSource dataSource, Relocater relocater) throws Exception {
		super();
		_streamer=new SimpleStreamer();
		_collapser=new DebugCollapser();
		//_collapser=new HashingCollapser(1000, 6000);

		_dummy=new DummyContextualiser();
		_databaseDataSource=dataSource;
		_databaseTable="WADI";
		//DatabaseMotable.init(_databaseDataSource, _databaseTable);
		_database=new SharedStoreContextualiser(_dummy, _collapser, true, new DatabaseStore("test", _databaseDataSource, _databaseTable, false, false, false));
		InetAddress localhost=InetAddress.getLocalHost();
		System.out.println("LOCALHOST: "+localhost);
		_clusterRelocater=relocater;
		_cluster=new ClusterContextualiser(_database, _collapser, _clusterRelocater);

		_statelessMethods=Pattern.compile("GET|POST", Pattern.CASE_INSENSITIVE);
		_statelessMethodFlag=true;
		_statelessURIs=Pattern.compile(".*\\.(JPG|JPEG|GIF|PNG|ICO|HTML|HTM)(|;jsessionid=.*)", Pattern.CASE_INSENSITIVE);
		_statelessURIFlag=false;
		_stateless=new StatelessContextualiser(_cluster, _statelessMethods, _statelessMethodFlag, _statelessURIs, _statelessURIFlag);

		File dir=new File(new File(System.getProperty("java.io.tmpdir")), "sessions");
		dir.delete();
		dir.mkdir();
		_discDirectory=dir;
		// TODO - consider eviction on disc, indexing by ttl would be efficient enough...
		_discEvicter=new NeverEvicter(20, true); // sessions never pass below this point, unless the node is shutdown
		_discMap=new ConcurrentHashMap();
		_disc=new ExclusiveStoreContextualiser(_stateless, _collapser, true, _discEvicter, _discMap, _streamer, _discDirectory);


		_memoryPool=pool;
		_memoryEvicter=new AbsoluteEvicter(10, true, 10); // if a session is inactive for 10 secs, it moves to disc
		_memoryMap=sessionMap;
		_serial=new SerialContextualiser(_disc, _collapser, _memoryMap);
		_requestPool=new DummyStatefulHttpServletRequestWrapperPool(); // TODO - use a ThreadLocal based Pool
		_memory=new MemoryContextualiser(_serial, _memoryEvicter, _memoryMap, _memoryPool, _requestPool);

		// ready to rock !
	}

	public boolean contextualise(Invocation invocation, String key, Immoter immoter, Sync invocationLock, boolean exclusiveOnly) throws InvocationException {
		return _memory.contextualise(invocation, key, immoter, invocationLock, exclusiveOnly);
	}

	public Evicter getEvicter() {
		return _memory.getEvicter();
	}

	public boolean isExclusive() {
		return _memory.isExclusive();
	}

	public Immoter getDemoter(String name, Motable motable) {
		return _memory.getDemoter(name, motable);
	}

	public Immoter getSharedDemoter() {
		return _memory.getSharedDemoter();
	}

	public void init(ContextualiserConfig config) {
		_memory.init(config);
	}

	public void start() throws Exception {
		_memory.start();
	}

	public void stop() throws Exception {
		_memory.stop();
	}

	public void promoteToExclusive(Immoter immoter){_memory.promoteToExclusive(immoter);}
	public void load(Emoter emoter, Immoter immoter) {_memory.load(emoter, immoter);}

	public Contextualiser getTop() {return _memory;}

    public void findRelevantSessionNames(PartitionMapper mapper, Map keyToSessionNames) {
		_log.info("findRelevantSessionNames");
		_memory.findRelevantSessionNames(mapper, keyToSessionNames);
	}

}
