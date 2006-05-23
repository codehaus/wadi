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
package org.codehaus.wadi.activecluster;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import org.apache.activecluster.Cluster;
import org.codehaus.wadi.Contextualiser;
import org.codehaus.wadi.DistributableContextualiserConfig;
import org.codehaus.wadi.dindex.impl.DIndex;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.impl.DummyContextualiserConfig;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedBoolean;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class DummyDistributableContextualiserConfig extends DummyContextualiserConfig implements DistributableContextualiserConfig {

	protected final Cluster _cluster;

	public DummyDistributableContextualiserConfig(Cluster cluster) {
		super();
		_cluster=cluster;
	}

	public ClassLoader getClassLoader() {
		return getClass().getClassLoader();
	}

	public Cluster getCluster() {
		return _cluster;
	}

	public String getNodeName() {
		return "dummy";
	}

	public InetSocketAddress getHttpAddress() {
		return null;
	}

	protected Map _state=new HashMap();

	public Object getDistributedState(Object key) {
		return _state.get(key);
	}

	public Object putDistributedState(Object key, Object newValue) {
		return _state.put(key, newValue);
	}

	public Object removeDistributedState(Object key) {
		return _state.remove(key);
	}

	public void distributeState() {
	}

	public boolean getAccessOnLoad() {
		// TODO Auto-generated method stub
		return false;
	}

	public SynchronizedBoolean getShuttingDown() {
		// TODO Auto-generated method stub
		return null;
	}

	public Map getDistributedState() {
		// TODO Auto-generated method stub
		return null;
	}

	public long getInactiveTime() {
		// TODO Auto-generated method stub
		return 0;
	}

	public int getNumPartitions() {
		// TODO Auto-generated method stub
		return 0;
	}

	public Dispatcher getDispatcher() {
		return null;
	}

	public DIndex getDIndex() {
		return null;
	}

	public Contextualiser getContextualiser() {
		return null;
	}

}
