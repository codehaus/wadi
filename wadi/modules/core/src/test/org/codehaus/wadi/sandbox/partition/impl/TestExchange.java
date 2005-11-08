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
package org.codehaus.wadi.sandbox.partition.impl;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import junit.framework.TestCase;

public class TestExchange extends TestCase {

	protected final Log _log = LogFactory.getLog(getClass());

	public static void main(String[] args) {
	}

	public TestExchange(String arg0) {
		super(arg0);
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}


	static class Node {

		public Node(int numPartitionsInCluster) {
			_numPartitionsInCluster=numPartitionsInCluster;
			_numPartitionsOwned=numPartitionsInCluster;
			_numNodesInCluster=1;
		}

		public Node() {
		}

		int _numPartitionsInCluster;
		int _numPartitionsOwned;
		int _numNodesInCluster;

		public String toString() {
			return (""+_numPartitionsOwned+"/"+_numPartitionsInCluster+"/"+_numNodesInCluster);
		}
	}

	public void join(List cluster, Node node) {
		for (Iterator i=cluster.iterator(); i.hasNext();) {
			Node n=(Node)i.next();
			node._numPartitionsInCluster=n._numPartitionsInCluster;
			node._numNodesInCluster= ++n._numNodesInCluster;

			int src=n._numPartitionsOwned;
			int tgt=node._numPartitionsOwned;

			while (tgt<numPartitionsPerNode && src>numPartitionsPerNode) {
				tgt++;
				src--;
			}

			n._numPartitionsOwned=src;
			node._numPartitionsOwned=tgt;
		}

		cluster.add(node);
	}

	protected int numPartitions=1024;
	protected int numNodes=100;
	protected int numPartitionsPerNode=numPartitions/numNodes;

	public void testExchange() throws Exception {


		List cluster=new ArrayList();

		cluster.add(new Node(numPartitions));

		for (int i=1; i<numNodes; i++) {
			join(cluster, new Node());
		}

		int numPartitionsInCluster=0;
		for (Iterator i=cluster.iterator(); i.hasNext(); ) {
			Node node=(Node)i.next();
			assertTrue(node._numPartitionsInCluster==numPartitions);
			assertTrue(node._numPartitionsOwned==numPartitionsPerNode || node._numPartitionsOwned==(numPartitionsPerNode+1));
			numPartitionsInCluster+=node._numPartitionsOwned;
			_log.info("node["+i+"]: "+node);
		}
		assertTrue(numPartitionsInCluster==numPartitions);

	}
}
