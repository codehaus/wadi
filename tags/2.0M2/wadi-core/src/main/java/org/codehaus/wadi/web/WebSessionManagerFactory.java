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
package org.codehaus.wadi.web;

import java.net.InetSocketAddress;
import java.util.Map;
import org.codehaus.wadi.Contextualiser;
import org.codehaus.wadi.InvocationProxy;
import org.codehaus.wadi.SessionIdFactory;
import org.codehaus.wadi.Streamer;
import org.codehaus.wadi.ValuePool;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision: 1497 $
 */
public interface WebSessionManagerFactory {

	public abstract Object create(
			WebSessionPool sessionPool,
			AttributesFactory attributesFactory,
			ValuePool valuePool,
			WebSessionWrapperFactory sessionWrapperFactory,
			SessionIdFactory sessionIdFactory,
			Contextualiser contextualiser,
			Map sessionMap,
			Router router,
			Streamer streamer,
			boolean accessOnLoad,
			String clusterUri,
			String clusterName,
			String nodeName,
			InvocationProxy httpProxy,
			InetSocketAddress httpAddress,
			int numPartitions
			) throws Exception;

}
