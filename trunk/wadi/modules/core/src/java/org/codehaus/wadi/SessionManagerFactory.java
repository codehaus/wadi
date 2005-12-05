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
package org.codehaus.wadi;

import java.net.InetSocketAddress;
import java.util.Map;

public interface SessionManagerFactory {
	
	public abstract Object create(
			SessionPool sessionPool,
			AttributesFactory attributesFactory,
			ValuePool valuePool,
			SessionWrapperFactory sessionWrapperFactory,
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
