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
package org.codehaus.wadi.gridstate.impl;

import javax.jms.Destination;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.gridstate.Dispatcher;
import org.codehaus.wadi.gridstate.PartitionConfig;
import org.codehaus.wadi.gridstate.PartitionManager;
import org.codehaus.wadi.gridstate.PartitionManagerConfig;
import org.codehaus.wadi.gridstate.PartitionMapper;

public class StaticPartitionManager implements PartitionManager {

	protected final Partition[] _partitions;
	protected final PartitionMapper _mapper;

	public StaticPartitionManager(int numPartitions, PartitionMapper mapper) {
		_partitions=new Partition[numPartitions];
		_mapper=mapper;
	}

	class StaticPartitionConfig implements PartitionConfig {

		protected final Destination _localDestination;
		
		StaticPartitionConfig(Destination localDestination) {
			_localDestination=localDestination;
		}
		
		public Destination getLocalDestination() {
			return _localDestination;
		}

		public Dispatcher getDispatcher() {
			return null;
		}
		
	}
	
	public void init(PartitionManagerConfig config) {
		for (int i=0; i<_partitions.length; i++) {
			_partitions[i].init(new StaticPartitionConfig(config.getLocalDestination()));
		}
	}

	public Partition[] getPartitions() {
		return _partitions;
	}
	
	public int getNumPartitions() {
		return _partitions.length;
	}

    protected final static Log _log=LogFactory.getLog(StaticPartitionManager.class);

    // MUST be called once to distribute the Partitions between Caches and initialise them correctly before they are used...
	public static void partition(GCache[] caches, PartitionManager[] managers, int numPartitions) {
		int numCaches=caches.length;
    	// initialise the partitions...
    	int partitionsPerCache=numPartitions/numCaches;
    	for (int i=0; i<numPartitions; i++) {
    		// figure out which node is Partition Master...
    		int index=i/partitionsPerCache;
    		GCache master=caches[index];
            if ( _log.isInfoEnabled() ) {

                _log.info("partition-" + i + " -> node-" + index);
            }
    		// go through all the nodes...
    		for (int j=0; j<numCaches; j++) {
    			GCache cache=caches[j];
    			if (cache!=master) {
    				// if node is not PartitionMaster - make partition remote, pointing to PartitionMaster
    				Partition partition=new Partition(master.getStateManager().createRemotePartition());
    				partition.init(cache.getPartitionConfig());
    				cache.getPartitions()[i]=partition;
    			} else {
    				Partition partition=new Partition(new LocalPartition());
    				cache.getPartitions()[i]=partition;
    			}
    		}
    	}
	}

	public Partition getPartition(Object key) {
		return _partitions[_mapper.map(key)];
	}

	public void start() throws Exception {
		// empty
	}

	public void stop() throws Exception {
		// empty
	}

}