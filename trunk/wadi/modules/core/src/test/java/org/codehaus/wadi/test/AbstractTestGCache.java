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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.gridstate.PartitionManager;
import org.codehaus.wadi.gridstate.PartitionMapper;
import org.codehaus.wadi.gridstate.StateManager;
import org.codehaus.wadi.gridstate.impl.GCache;
import org.codehaus.wadi.gridstate.impl.IndirectStateManager;
import org.codehaus.wadi.gridstate.impl.StaticPartitionManager;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.impl.FixedWidthSessionIdFactory;

import junit.framework.TestCase;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision: 1550 $
 */
public abstract class AbstractTestGCache extends TestCase {

    protected final Log _log=LogFactory.getLog(getClass().getName());

    public AbstractTestGCache(String name) {
        super(name);
    }

    protected final int _numNodes=5;
    protected final int _numPartitions=_numNodes*2;
    //protected final int _numPartitions=1;
    protected final int _numThreads=10;
    protected final int _numIters=20;
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

    protected void setUp(DispatcherFactory factory) throws Exception {
    	long timeout=5000L;
        for (int i=0; i<_numNodes; i++) {
        	String nodeName="node-"+i;
        	Dispatcher dispatcher=factory.create(nodeName, _clusterName, timeout);
        	PartitionManager partitionManager=new StaticPartitionManager(dispatcher, _numPartitions, _mapper);
        	StateManager stateManager=new IndirectStateManager(dispatcher, timeout);
        	_partitionManagers[i]=partitionManager;
    		_nodes[i]=new GCache(dispatcher, partitionManager, stateManager);
        }

        StaticPartitionManager.partition(_nodes, _partitionManagers, _numPartitions);

        for (int i=0; i<_nodes.length; i++)
        	_nodes[i].init();

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
                    if (_log.isTraceEnabled()) _log.trace("node-" + j + ": acquiring: " + _key + " ..." + " <" + Thread.currentThread().getName() + ">");
    				values[j]=_nodes[j].get(_key);
                    if (_log.isTraceEnabled()) _log.trace("node-" + j + ": ...acquired: " + _key + "=" + values[j] + " <" + Thread.currentThread().getName() + ">");
    				if (j>0)
    					assertTrue(values[j-1].equals(values[j]));
    			}
    			long elapsed=System.currentTimeMillis()-start;
    			int numFetches=_numIters*_numNodes;
                if (_log.isWarnEnabled()) _log.warn("rate: " + numFetches + " in " + elapsed + " millis = " + ( elapsed / numFetches ) + " millis/fetch");
    		}
    	}
    }

    interface DispatcherFactory {
    	Dispatcher create(String nodeName, String clusterName, long timeout) throws Exception;
    }

	//protected final String _clusterUri="peer://org.codehaus.wadi";
	//protected final String _clusterUri="tcp://smilodon:61616";
	protected final String _clusterUri="vm://localhost";
	protected final String _clusterName="WADI";

//	class JGroupsDispatcherFactory implements DispatcherFactory {
//
//    	public Dispatcher create(String nodeName, String clusterName, long timeout) throws Exception {
//    		return new JGroupsDispatcher(nodeName, clusterName, timeout);
//    	}
//    }
//
//    class ActiveClusterDispatcherFactory implements DispatcherFactory {
//
//    	public Dispatcher create(String nodeName, String clusterName, long timeout) throws Exception {
//    		return new ActiveClusterDispatcher(nodeName, clusterName, _clusterUri, timeout);
//    	}
//    }

    protected abstract DispatcherFactory createDispatcherFactory();
    
//    public void testFunctionality() throws Exception {
//    	//testGCache(new JGroupsIndirectStateManagerFactory(), 1);
//    	testFunctionality(new ActiveClusterIndirectStateManagerFactory(60*1000), 1);
//    }
//
//    public void testConcurrency() throws Exception {
//    	//testGCache(new JGroupsIndirectStateManagerFactory(), 1);ping smilodon
//    	testConcurrency(new ActiveClusterIndirectStateManagerFactory(60*1000), 100, 100);
//    }

    public void testSoak() throws Exception {
    	testSoak(createDispatcherFactory());
    }

    public void testFunctionality(DispatcherFactory factory) throws Exception {

    	setUp(factory);

    	GCache red=_nodes[0];
    	GCache green=_nodes[1];
    	//GCache blue=_nodes[2];

	_log.info("0 nodes running");
        for (int i=0; i<_numNodes; i++)
        	_nodes[i].start();

        Thread.sleep(12000);
        //red.getCluster().waitForClusterToComplete(_numNodes, 6000);
        if (_log.isInfoEnabled()) _log.info(_numNodes + " nodes running");

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
        if (_log.isInfoEnabled()) _log.info("elapsed: " + ( System.currentTimeMillis() - start ) + " millis");


        Thread.sleep(6000);
        if (_log.isInfoEnabled()) _log.info(_numNodes + " nodes running");
        for (int i=1; i<_numNodes; i++)
        	_nodes[i].stop();

        //red.getCluster().waitForClusterToComplete(1, 6000);
	_log.info("1 node running");
        red.stop();
	_log.info("0 nodes running");
    }

    public void testConcurrency(DispatcherFactory factory) throws Exception {

    	setUp(factory);

	_log.info("0 nodes running");
        for (int i=0; i<_numNodes; i++)
        	_nodes[i].start();

        Thread.sleep(12000);
        //_nodes[_numNodes-1].getCluster().waitForClusterToComplete(_numNodes, 6000);
        if (_log.isWarnEnabled()) _log.warn(_numNodes + " nodes running");

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
        if (_log.isInfoEnabled()) _log.info(_numNodes + " nodes running");
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
            if (_log.isWarnEnabled()) _log.warn("rate: " + numOperations + " in " + elapsed + " millis = " + ( elapsed / numOperations ) + " millis/operation");
    	}
    }

    public void testSoak(DispatcherFactory factory) throws Exception {

    	setUp(factory);

	_log.info("0 nodes running");
        for (int i=0; i<_numNodes; i++)
        	_nodes[i].start();

        Thread.sleep(12000);
        //_nodes[_numNodes-1].getCluster().waitForClusterToComplete(_numNodes, 6000);
        if (_log.isWarnEnabled()) _log.warn(_numNodes + " nodes running");

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
        if (_log.isInfoEnabled()) _log.info(_numNodes + " nodes running");
        for (int i=0; i<_numNodes; i++)
        	_nodes[i].stop();

        Thread.sleep(6000);
	_log.info("0 nodes running");
    }

}
