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

import java.io.Serializable;
import java.util.Map;

import org.activecluster.Cluster;
import org.activemq.ActiveMQConnectionFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.DispatcherConfig;
import org.codehaus.wadi.ExtendedCluster;
import org.codehaus.wadi.dindex.impl.BucketFacade;
import org.codehaus.wadi.impl.CustomClusterFactory;
import org.codehaus.wadi.impl.Dispatcher;
import org.codehaus.wadi.impl.FixedWidthSessionIdFactory;

import junit.framework.TestCase;

public class TestGCache extends TestCase {

    protected final Log _log=LogFactory.getLog(getClass().getName());

    public TestGCache(String name) {
        super(name);
    }

    protected int _numBuckets;
    protected FixedWidthSessionIdFactory _factory;
    protected BucketMapper _mapper;
    
    protected void setUp() throws Exception {
        super.setUp();
        _numBuckets=12;
        _factory=new FixedWidthSessionIdFactory(10, "0123456789".toCharArray(), _numBuckets);
        _mapper=new BucketMapper() { public int map(Serializable key) { return _factory.getBucket((String)key);} };
    }

    protected void tearDown() throws Exception {
        _numBuckets=0;
        super.tearDown();
    }
    
    class Foo implements Runnable {
        
        protected final BucketFacade _facade;
        
        public Foo(BucketFacade facade) {
            _facade=facade;
        }
        
        public void run() {
            
        }
        
    }
    
    class MyNode {
        protected final String _clusterUri="tcp://smilodon:61616";
        protected final String _clusterName="ORG.CODEHAUS.WADI.TEST";
        protected final ActiveMQConnectionFactory _connectionFactory=new ActiveMQConnectionFactory(_clusterUri);
        protected final CustomClusterFactory _clusterFactory=new CustomClusterFactory(_connectionFactory);
        protected final Cluster _cluster;
        protected final Dispatcher _dispatcher;
        protected final GCache _gcache;
        
        public MyNode(String nodeName, int numBuckets) throws Exception {
        	_cluster=_clusterFactory.createCluster(_clusterName);
        	_dispatcher=new Dispatcher(nodeName);
        	_dispatcher.init(new MyDispatcherConfig(_cluster));
        	_gcache=new GCache(nodeName, numBuckets, _dispatcher, _mapper);
        }
        
        public void start() throws Exception {
        	_cluster.start();
        	_gcache.start();
        }
        
        public void stop() throws Exception {
        	_gcache.stop();
        	_cluster.stop();
        }
        
        public Cluster getCluster() {
        	return _cluster;
        }
        
        public Bucket[] getBuckets() {
        	return _gcache.getBuckets();
        }
        
        public GCache getGCache() {
        	return _gcache;
        }
        
        public Map getMap() {
        	return _gcache.getMap();
        }
        
        public boolean putFirst(Object key, Object value) {
        	return _gcache.putFirst(key, value);
        }

        public Object put(Object key, Object value) {
        	return _gcache.put(key, value);
        }

        public Object put(Object key, Object value, boolean overwrite, boolean returnOldValue) {
        	return _gcache.put(key, value, overwrite, returnOldValue);
        }

        public Object remove(Object key) {
        	return _gcache.remove(key);
        }

        public Object remove(Object key, boolean returnOldValue) {
        	return _gcache.remove(key, returnOldValue);
        }

        public Object get(Object key) {
        	return _gcache.get(key);
        }
        
    }
    
    class MyDispatcherConfig implements DispatcherConfig {

    	protected final Cluster _cluster;
    	
    	MyDispatcherConfig(Cluster cluster) {
    		_cluster=cluster;
    	}
    	
    	public ExtendedCluster getCluster() {
    		return (ExtendedCluster)_cluster;
    	}
    }
    
    FixedWidthSessionIdFactory factory;
    
    public void testGCache() throws Exception {
        MyNode red=new MyNode("red", _numBuckets);
        MyNode green=new MyNode("green", _numBuckets);
        {
        	Bucket[] buckets=red.getBuckets();
        	for (int i=6; i<_numBuckets; i++) {
        		Bucket bucket=new Bucket(new RemoteBucket(green.getCluster().getLocalNode().getDestination()));
        		bucket.init(red.getGCache());
        		buckets[i]=bucket;
        	}
        }
        {
        	Bucket[] buckets=green.getBuckets();
        	for (int i=0; i<6; i++) {
        		Bucket bucket=new Bucket(new RemoteBucket(red.getCluster().getLocalNode().getDestination()));
        		bucket.init(green.getGCache());
        		buckets[i]=bucket;
        	}
        }
        
        //GCache green=new GCache("green", _numBuckets);
        //GCache blue=new GCache("blue", _numBuckets);

        _log.info("0 nodes running");
        red.start();
        red.getCluster().waitForClusterToComplete(1, 6000);
        _log.info("1 node running");
        green.start();
        red.getCluster().waitForClusterToComplete(2, 6000);
        green.getCluster().waitForClusterToComplete(2, 6000);
        _log.info("2 nodes running");
//        blue.start();
//        red.getCluster().waitForClusterToComplete(3, 6000);
//        green.getCluster().waitForClusterToComplete(3, 6000);
//        blue.getCluster().waitForClusterToComplete(3, 6000);
//        _log.info("3 nodes running");

        for (int i=0; i<_numBuckets; i++) {
            String key=_factory.create(i);
            _log.info("key: "+key);
            // retrieve an association that does not exist...
            assertFalse(red.getMap().containsKey(key));
            assertTrue(red.get(key)==null);
            assertFalse(green.getMap().containsKey(key));
            assertTrue(green.get(key)==null);
            // remove an association that does not exist...
            assertTrue(red.remove(key)==null);
            assertTrue(green.remove(key)==null);
            String data=key+"-data";
            // insert an association for the first time...
            assertTrue(red.putFirst(key, data));
            assertTrue(red.getMap().containsKey(key));
            assertTrue(!green.getMap().containsKey(key));
            // remove a local association - returning its last value
            assertTrue(red.remove(key).equals(data));
            assertTrue(!red.getMap().containsKey(key));
            assertTrue(!green.getMap().containsKey(key));
            // remove a non-existant association
            assertTrue(red.remove(key)==null);
            // replace the association - expecting it to not already exist
            assertTrue(red.putFirst(key, data));
            assertTrue(red.getMap().containsKey(key));
            assertTrue(!green.getMap().containsKey(key));
            // remove it again - not returning the last value
            assertTrue(red.remove(key, false)==null);
            assertTrue(!red.getMap().containsKey(key));
            assertTrue(!green.getMap().containsKey(key));
            // replace the association - expecting it to not already exist
            assertTrue(red.putFirst(key, data));
            assertTrue(red.getMap().containsKey(key));
            assertTrue(!green.getMap().containsKey(key));
            // insert an association that already exists, stating that this should fail (for session id insertion)
            assertTrue(!red.putFirst(key, data));
            // try overwriting the [local] value with itself
            assertTrue(red.put(key, data).equals(data));
            // try overwriting the [local] value with itself - but not returning old value
            assertTrue(red.put(key, data, true, false)==null);
            // retrieve an association from the StateOwner
            assertTrue(red.get(key).equals(data));
            assertTrue(red.getMap().containsKey(key));
            assertTrue(!green.getMap().containsKey(key));
            // retrieve an association from a node that is not the StateOwner
            _log.info("get remote...");
            assertTrue(green.get(key).equals(data));
            assertTrue(!red.getMap().containsKey(key));
            assertTrue(green.getMap().containsKey(key));
            // retrieve an association from a node that is not the StateOwner
            assertTrue(red.get(key).equals(data));
            assertTrue(red.getMap().containsKey(key));
            assertTrue(!green.getMap().containsKey(key));
            // retrieve an association from a node that is not the StateOwner
            _log.info("read remote...");
            assertTrue(green.get(key).equals(data));
            assertTrue(!red.getMap().containsKey(key));
            assertTrue(green.getMap().containsKey(key));
            _log.info("remove remote...");
            Object value=red.remove(key);
            _log.info(key+" = "+value);
            assertTrue(value.equals(data));
            assertTrue(red.put(key, data)==null);
            String newData=(data+".new");
            assertTrue(green.put(key, newData).equals(data));
            assertTrue(red.put(key, data).equals(newData));
        }
        
//        _log.info("3 nodes running");
//        blue.stop();
        green.getCluster().waitForClusterToComplete(2, 6000);
        red.getCluster().waitForClusterToComplete(2, 6000);
        _log.info("2 nodes running");
        green.stop();
        red.getCluster().waitForClusterToComplete(1, 6000);
        _log.info("1 nodes running");
        red.stop();
        _log.info("0 nodes running");
    }

}
