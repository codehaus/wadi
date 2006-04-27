/**
 *
 * Copyright 2005 Core Developers Network Ltd.
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

import org.codehaus.wadi.ProxiedLocation;

/**
 * @version $Revision$
 */
public class HTTPProxiedLocation implements ProxiedLocation {
	private final InetSocketAddress address;
	
	public HTTPProxiedLocation(InetSocketAddress address) {
		this.address = address;
	}
	
	public InetSocketAddress getInetSocketAddress() {
		return address;
	}
	
}
