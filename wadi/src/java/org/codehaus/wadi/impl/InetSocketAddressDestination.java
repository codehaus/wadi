/**
*
* Copyright 2003-2004 The Apache Software Foundation
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

package org.codehaus.wadi.impl;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import javax.jms.Destination;

/**
 * A javax.jms.Destination that encapsulates a java.net.InetSocketAddress
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public class InetSocketAddressDestination
implements Destination, Serializable
{
    protected InetAddress _address;
    protected int         _port;
    
	public void setAddress(InetAddress address) {
		_address=address;
	}
	public InetAddress getAddress() {
		return _address;
	}
	public void setPort(int port) {
		_port=port;
	}
	public int getPort() {
		return _port;
	}
	
		public String
		toString()
		{
				return new InetSocketAddress(_address, _port).toString();
		}
}
