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
package org.codehaus.wadi.impl;

import java.util.ArrayList;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;

// only create a new MBeanServer if one does not already exist...

public class MBeanServerFactoryBean extends org.springframework.jmx.support.MBeanServerFactoryBean {

	protected MBeanServer _server;

	public void afterPropertiesSet() {
		String agentId=null; // TODO - parameterise
		ArrayList servers=MBeanServerFactory.findMBeanServer(agentId);
		if (servers!=null && servers.size()>0)
			_server=(MBeanServer)servers.get(0);
		else
			super.afterPropertiesSet();
	}

	public Object getObject() {
		if (_server!=null)
			return _server;
		else
			return super.getObject();
	}

	public Class getObjectType() {
		if (_server!=null)
			return _server.getClass();
		else
			return super.getObjectType();
	}

	public void destroy() {
		if (_server==null)
			super.destroy();
	}
}
