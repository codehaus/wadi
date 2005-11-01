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
package org.codehaus.wadi.sandbox.gridstate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.impl.FixedWidthSessionIdFactory;

import junit.framework.TestCase;

public class TestGCache extends TestCase {

    protected final Log _log=LogFactory.getLog(getClass().getName());

    public TestGCache(String name) {
        super(name);
    }

    protected int _numNodes=3;
    protected GCache[] _nodes=new GCache[_numNodes];
    protected int _numBuckets=_numNodes*1;
    protected FixedWidthSessionIdFactory _factory;
    protected BucketMapper _mapper;
    
    protected void setUp() throws Exception {
        super.setUp();
        _factory=new FixedWidthSessionIdFactory(10, "0123456789".toCharArray(), _numBuckets);
        _mapper=new BucketMapper() { public int map(Object key) { return _factory.getBucket((String)key);} };
    }

    protected void tearDown() throws Exception {
        _numBuckets=0;
        super.tearDown();
    }
    
    FixedWidthSessionIdFactory factory;
    
    public class Tester implements Runnable {
    	
    	GCache[] _nodes;
    	Object _key;
    	int _numIters;
    	
    	Tester(GCache[] nodes, Object key, int numIters) {
    		_nodes=nodes;
    		_key=key;
    		_numIters=numIters;
    	}
    	
    	public void run() {
    		_log.info("starting");
    		for (int i=0; i<_numIters; i++) {
    			//_log.info("iter: "+i);
    			Object[] values=new Object[_numNodes];
    			for (int j=0; j<_numNodes; j++) {
    				values[j]=_nodes[j].get(_key);
    				if (j>0)
    					assertTrue(values[j-1].equals(values[j]));
    			}
    		}
    	}
    }
    
    interface ProtocolFactory {
    	Protocol createProtocol(String name) throws Exception;
    }
    
    class JGroupsIndirectProtocolFactory implements ProtocolFactory {
    	public Protocol createProtocol(String name) throws Exception {
    		return new JGroupsIndirectProtocol(name, _numBuckets, _mapper);
    	}
    }
    
    class ActiveClusterIndirectProtocolFactory implements ProtocolFactory {
    	public Protocol createProtocol(String name) throws Exception {
    		return new ActiveClusterIndirectProtocol(name, _numBuckets, _mapper);
    	}
    }
    
    public void testGCache() throws Exception {
    	testGCache(new JGroupsIndirectProtocolFactory(), 1);
    	testGCache(new ActiveClusterIndirectProtocolFactory(), 1);
    }
    
    public void testGCache(ProtocolFactory factory, int numIterations) throws Exception {
    	for (int i=0; i<_numNodes; i++)
    		_nodes[i]=new GCache(factory.createProtocol("node-"+i), _mapper);

    	int bucketsPerNode=_numBuckets/_numNodes;
    	for (int i=0; i<_numBuckets; i++) {
    		GCache local=_nodes[i/bucketsPerNode];
    		for (int j=0; j<_numNodes; j++) {
    			GCache node=_nodes[j];
    			if (node!=local) {
    				Bucket bucket=new Bucket(local.getProtocol().createRemoteBucket());
    				bucket.init(node.getBucketConfig());
    				node.getBuckets()[i]=bucket;
    			}
    		}
    	}

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
        for (int i=0; i<_numBuckets; i++) {
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
            // retrieve an association from the StateOwner
            assertTrue(red.get(key).equals(data));
            assertTrue(red.containsKey(key));
            assertTrue(!green.containsKey(key));
            // retrieve an association from a node that is not the StateOwner
            //_log.info("get remote...");
            assertTrue(green.get(key).equals(data));
            assertTrue(!red.containsKey(key));
            assertTrue(green.containsKey(key));
            // retrieve an association from a node that is not the StateOwner
            assertTrue(red.get(key).equals(data));
            assertTrue(red.containsKey(key));
            assertTrue(!green.containsKey(key));
            // retrieve an association from a node that is not the StateOwner
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
            int numThreads=10;
            Thread[] thread=new Thread[numThreads];
            for (int j=0; j<numThreads; j++)
        		(thread[j]=new Thread(new Tester(_nodes, key, numIterations), "GCacheTestThread-"+j)).start();
            for (int j=0; j<numThreads; j++)
            	thread[j].join();
            _log.info("complete: "+(i+1)+"/"+_numBuckets+" - "+(System.currentTimeMillis()-start)+" millis");
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

}
