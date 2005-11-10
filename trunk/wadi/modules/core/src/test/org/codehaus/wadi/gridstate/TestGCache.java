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
package org.codehaus.wadi.gridstate;

import org.activecluster.Cluster;
import org.activemq.ActiveMQConnectionFactory;
import org.activemq.store.vm.VMPersistenceAdapterFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.ActiveClusterDispatcherConfig;
import org.codehaus.wadi.Dispatcher;
import org.codehaus.wadi.gridstate.PartitionManager;
import org.codehaus.wadi.gridstate.PartitionMapper;
import org.codehaus.wadi.gridstate.Protocol;
import org.codehaus.wadi.gridstate.activecluster.ActiveClusterDispatcher;
import org.codehaus.wadi.gridstate.activecluster.CustomClusterFactory;
import org.codehaus.wadi.gridstate.impl.GCache;
import org.codehaus.wadi.gridstate.impl.IndirectProtocol;
import org.codehaus.wadi.gridstate.impl.StaticPartitionManager;
import org.codehaus.wadi.gridstate.jgroups.JGroupsDispatcher;
import org.codehaus.wadi.impl.FixedWidthSessionIdFactory;
import org.jgroups.Channel;
import org.jgroups.JChannel;

import junit.framework.TestCase;

public class TestGCache extends TestCase {

    protected final Log _log=LogFactory.getLog(getClass().getName());

    public TestGCache(String name) {
        super(name);
    }

    protected final int _numNodes=3;
    protected final int _numPartitions=_numNodes;
    //protected final int _numPartitions=1;
    protected final int _numThreads=1;
    protected final int _numIters=10;
    protected final FixedWidthSessionIdFactory _factory=new FixedWidthSessionIdFactory(10, "0123456789".toCharArray(), _numPartitions);
    protected final PartitionMapper _mapper=new PartitionMapper() { public int map(Object key) { return _factory.getPartition((String)key);} };
    
    protected GCache[] _nodes=new GCache[_numNodes];
    protected PartitionManager[] _partitionManagers=new PartitionManager[_numNodes];

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    protected void setUp(ProtocolFactory factory) throws Exception {
        for (int i=0; i<_numNodes; i++) {
        	PartitionManager manager=new StaticPartitionManager(_numPartitions);
        	_partitionManagers[i]=manager;
    		_nodes[i]=new GCache(factory.createProtocol("node-"+i, manager), _mapper);
        }

        StaticPartitionManager.partition(_nodes, _partitionManagers, _numPartitions);
    }
    
    public class Tester implements Runnable {
    	
    	Object _key;
    	
    	Tester(Object key) {
    		_key=key;
    	}
    	
    	public void run() {
    		for (int i=0; i<_numIters; i++) {
    			long start=System.currentTimeMillis();
    			//_log.info("iter: "+i);
    			Object[] values=new Object[_numNodes];
    			for (int j=0; j<_numNodes; j++) {
    				_log.trace("node-"+j+": acquiring: "+_key+" ..."+" <"+Thread.currentThread().getName()+">");
    				values[j]=_nodes[j].get(_key);
    				_log.trace("node-"+j+": ...acquired: "+_key+"="+values[j]+" <"+Thread.currentThread().getName()+">");
    				if (j>0)
    					assertTrue(values[j-1].equals(values[j]));
    			}
    			long elapsed=System.currentTimeMillis()-start;
    			int numFetches=_numIters*_numNodes;
    			_log.warn("rate: "+numFetches+" in "+elapsed+" millis = "+(elapsed/numFetches)+" millis/fetch");
    		}
    	}
    }
    
    interface ProtocolFactory {
    	Protocol createProtocol(String name, PartitionManager manager) throws Exception;
    }
    
    abstract class AbstractProtocolFactory implements ProtocolFactory {
    	
    	protected final long _timeout;
    	
    	public AbstractProtocolFactory(long timeout) {
    		_timeout=timeout;
    	}
    }
    
	class MyJGroupsDispatcherConfig implements JGroupsDispatcherConfig {

		protected final Channel _channel;
		
		MyJGroupsDispatcherConfig() throws Exception {
			_channel=new JChannel();
		}
		
		public Channel getChannel() {
			return _channel;
		}

	}

	class JGroupsIndirectProtocolFactory extends AbstractProtocolFactory {
    	
    	public JGroupsIndirectProtocolFactory(long timeout) {
    		super(timeout);
    	}
    	
    	public Protocol createProtocol(String name, PartitionManager manager) throws Exception {
    		Dispatcher dispatcher=new JGroupsDispatcher();
    		dispatcher.init(new MyJGroupsDispatcherConfig());
    		return new IndirectProtocol(name, manager, _mapper, dispatcher, _timeout);
    	}
    }
    
    // do something whith this...
	//protected final String _clusterUri="peer://org.codehaus.wadi";
	//protected final String _clusterUri="tcp://smilodon:61616";
	protected final String _clusterUri="vm://localhost";
	protected final String _clusterName="WADI";
	protected final ActiveMQConnectionFactory _connectionFactory=new ActiveMQConnectionFactory(_clusterUri);
	protected final CustomClusterFactory _clusterFactory=new CustomClusterFactory(_connectionFactory);
	protected Cluster _cluster;

	class MyActiveClusterDispatcherConfig implements ActiveClusterDispatcherConfig {

		protected final Cluster _cluster;

		MyActiveClusterDispatcherConfig(Cluster cluster) {
			_cluster=cluster;
		}

		public ExtendedCluster getCluster() {
			return (ExtendedCluster)_cluster;
		}
	}

    
    class ActiveClusterIndirectProtocolFactory extends AbstractProtocolFactory {
    	
    	public ActiveClusterIndirectProtocolFactory(long timeout) {
    		super(timeout);
    	}
    	
    	public Protocol createProtocol(String name, PartitionManager manager) throws Exception {
    		System.setProperty("activemq.persistenceAdapterFactory", VMPersistenceAdapterFactory.class.getName());
    		//_clusterFactory.setInactiveTime(100000L); // ???
    		_cluster=_clusterFactory.createCluster(_clusterName);
    		Dispatcher dispatcher=new ActiveClusterDispatcher(name);
    		dispatcher.init(new MyActiveClusterDispatcherConfig(_cluster));
    		return new IndirectProtocol(name, manager, _mapper, dispatcher, _timeout);
    	}
    }
    
//    public void testFunctionality() throws Exception {
//    	//testGCache(new JGroupsIndirectProtocolFactory(), 1);
//    	testFunctionality(new ActiveClusterIndirectProtocolFactory(60*1000), 1);
//    }
//
//    public void testConcurrency() throws Exception {
//    	//testGCache(new JGroupsIndirectProtocolFactory(), 1);ping smilodon
//    	testConcurrency(new ActiveClusterIndirectProtocolFactory(60*1000), 100, 100);
//    }

    public void testSoak() throws Exception {
    	testSoak(new JGroupsIndirectProtocolFactory(60*1000));
    	testSoak(new ActiveClusterIndirectProtocolFactory(60*1000));
    }

    public void testFunctionality(ProtocolFactory factory) throws Exception {

    	setUp(factory);
    	
    	GCache red=_nodes[0];
    	GCache green=_nodes[1];
    	//GCache blue=_nodes[2];
    	
        _log.info("0 nodes running");
        for (int i=0; i<_numNodes; i++)
        	_nodes[i].start();
        
        Thread.sleep(12000);
        //red.getCluster().waitForClusterToComplete(_numNodes, 6000);
        _log.info(_numNodes+" nodes running");

        long start=System.currentTimeMillis();
        for (int i=0; i<_numPartitions; i++) {
            String key=_factory.create(i);
            //_log.info("key: "+key);
            // retrieve an association that does not exist...
            assertFalse(red.containsKey(key));
            assertTrue(red.get(key)==null);
            assertFalse(green.containsKey(key));
            assertTrue(green.get(key)==null);
            // remove an association that does not exist...
            assertTrue(red.remove(key)==null);
            assertTrue(!red.containsKey(key));
            assertTrue(!green.containsKey(key));
            assertTrue(green.remove(key)==null);
            assertTrue(!red.containsKey(key));
            assertTrue(!green.containsKey(key));
            String data=key+"-data";
            // insert an association for the first time...
            assertTrue(red.putFirst(key, data));
            assertTrue(red.containsKey(key));
            assertTrue(!green.containsKey(key));
            // remove a local association - returning its last value
            assertTrue(red.remove(key).equals(data));
            assertTrue(!red.containsKey(key));
            assertTrue(!green.containsKey(key));
            // remove a non-existant association
            assertTrue(red.remove(key)==null);
            // replace the association - expecting it to not already exist
            assertTrue(red.putFirst(key, data));
            assertTrue(red.containsKey(key));
            assertTrue(!green.containsKey(key));
            // remove it again - not returning the last value
            assertTrue(red.remove(key, false)==null);
            assertTrue(!red.containsKey(key));
            assertTrue(!green.containsKey(key));
            // replace the association - expecting it to not already exist
            assertTrue(red.putFirst(key, data));
            assertTrue(red.containsKey(key));
            assertTrue(!green.containsKey(key));
            // insert an association that already exists, stating that this should fail (for session id insertion)
            assertTrue(!red.putFirst(key, data));
            // try overwriting the [local] value with itself
            assertTrue(red.put(key, data).equals(data));
            // try overwriting the [local] value with itself - but not returning old value
            assertTrue(red.put(key, data, true, false)==null);
            // retrieve an association from the StateMaster
            assertTrue(red.get(key).equals(data));
            assertTrue(red.containsKey(key));
            assertTrue(!green.containsKey(key));
            // retrieve an association from a node that is not the StateMaster
            //_log.info("get remote...");
            assertTrue(green.get(key).equals(data));
            assertTrue(!red.containsKey(key));
            assertTrue(green.containsKey(key));
            // retrieve an association from a node that is not the StateMaster
            assertTrue(red.get(key).equals(data));
            assertTrue(red.containsKey(key));
            assertTrue(!green.containsKey(key));
            // retrieve an association from a node that is not the StateMaster
            //_log.info("read remote...");
            assertTrue(green.get(key).equals(data));
            assertTrue(!red.containsKey(key));
            assertTrue(green.containsKey(key));
            //_log.info("remove remote...");
            Object value=red.remove(key);
            //_log.info(key+" = "+value);
            assertTrue(value.equals(data));
            assertTrue(red.put(key, data)==null);
            String newData=(data+".new");
            assertTrue(green.put(key, newData).equals(data));
            //_log.info("put remote...");
            Object value2=red.put(key, data);
            //_log.info(key+" = "+value2);
            assertTrue(value2.equals(newData));
        }
        _log.info("elapsed: "+(System.currentTimeMillis()-start)+" millis");

        
        Thread.sleep(6000);
        _log.info(_numNodes+" nodes running");
        for (int i=1; i<_numNodes; i++)
        	_nodes[i].stop();

        //red.getCluster().waitForClusterToComplete(1, 6000);
        _log.info("1 node running");
        red.stop();
        _log.info("0 nodes running");
    }

    public void testConcurrency(ProtocolFactory factory) throws Exception {
    	
    	setUp(factory);

        _log.info("0 nodes running");
        for (int i=0; i<_numNodes; i++)
        	_nodes[i].start();
        
        Thread.sleep(12000);
        //_nodes[_numNodes-1].getCluster().waitForClusterToComplete(_numNodes, 6000);
        _log.warn(_numNodes+" nodes running");

        _log.info("starting");
        for (int i=0; i<_numPartitions; i++) { // do this for each partition...
            String key=_factory.create(i);

            
            // put something into the cache
            _nodes[0].put(key, key+"-data");

            Thread[] thread=new Thread[_numThreads];
            for (int j=0; j<_numThreads; j++)
        		(thread[j]=new Thread(new Tester(key), "GCacheTestThread-"+j)).start();
            for (int j=0; j<_numThreads; j++)
            	thread[j].join();
        }
        _log.warn("finished");
        
        Thread.sleep(6000);
        _log.info(_numNodes+" nodes running");
        for (int i=0; i<_numNodes; i++)
        	_nodes[i].stop();

        //red.getCluster().waitForClusterToComplete(1, 6000);
        Thread.sleep(6000);
        _log.info("0 nodes running");
    }
    
    public class Soaker implements Runnable {
    	
    	String[] _keys;
    	
    	Soaker(String[] keys) {
    		_keys=keys;
    	}
    	
    	public void run() {
    		long start=System.currentTimeMillis();
    		for (int i=0; i<_numIters; i++) {
    			for (int j=0; j<_nodes.length; j++) {
    				GCache cache=_nodes[j];
    				for (int k=0; k<_keys.length; k++) {
    					String key=_keys[k];
    					
    					cache.put(key, key+"-data"); // add a lock
    					cache.get(key); // we already have this lock
    					//cache.remove(key); // remove item and leave lock until release...
    					cache.release(); // release all locks in this interaction...
    				}
    			}
    		}
    		long elapsed=System.currentTimeMillis()-start;
    		int numOperations=_numIters*_numNodes*_keys.length*2;
    		_log.warn("rate: "+numOperations+" in "+elapsed+" millis = "+(elapsed/numOperations)+" millis/operation");
    	}
    }
    
    public void testSoak(ProtocolFactory factory) throws Exception {
    	
    	setUp(factory);
    	
        _log.info("0 nodes running");
        for (int i=0; i<_numNodes; i++)
        	_nodes[i].start();
        
        Thread.sleep(12000);
        //_nodes[_numNodes-1].getCluster().waitForClusterToComplete(_numNodes, 6000);
        _log.warn(_numNodes+" nodes running");

        _log.info("starting");
        
        // make up keys that will hash into every partition...
        GCache node=_nodes[0];
        String[] keys=new String[_numPartitions];
        for (int i=0; i<_numPartitions; i++) {
        	String key=_factory.create(i);
        	keys[i]=key;
        	node.put(key, key+"-data");
    	}	

        Thread[] thread=new Thread[_numThreads];
        for (int j=0; j<_numThreads; j++)
    		(thread[j]=new Thread(new Soaker(keys), "SoakThread-"+j)).start();
        for (int j=0; j<_numThreads; j++)
        	thread[j].join();
        _log.warn("finished");
        
        Thread.sleep(6000);
        _log.info(_numNodes+" nodes running");
        for (int i=0; i<_numNodes; i++)
        	_nodes[i].stop();

        Thread.sleep(6000);
        _log.info("0 nodes running");
    }
    
}
