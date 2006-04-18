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
package org.codehaus.wadi.jgroups;

import junit.framework.TestCase;

import org.apache.activecluster.Cluster;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

//can't use Utils...

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision: 1567 $
 */
public class TestJGroups extends TestCase {
	
	protected Log _log = LogFactory.getLog(getClass());
	
	// early version of JGroupsCluster would sometimes hang on startup... - test
	public void testWADI() throws Exception {
		String clusterName="org.codehaus.wadi.cluster-"+Math.random();
		Cluster c0=new JGroupsCluster(clusterName);
		Cluster c1=new JGroupsCluster(clusterName);
		c0.start();
		c1.start();
		c1.stop();
		c0.stop();
	}
	
}
