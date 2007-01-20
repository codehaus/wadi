/**
 *
 * Copyright 2003-2006 Core Developers Network Ltd.
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
package org.codehaus.wadi.webapp;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Random;


import org.codehaus.wadi.Manager;
import org.codehaus.wadi.impl.SpringManagerFactory;

import junit.framework.TestCase;

/**
 * Confirm that the current wadi-web.xml will construct a valid
 * WADI installation.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class TestWadiWebXml extends TestCase {

	public void testWadiWebXml() throws Exception {
		System.setProperty("http.port", "8080");
		System.setProperty("node.name", "test-"+new Random().nextInt());
		System.setProperty("wadi.db.url", "jdbc:derby:WADI;create=true");
		System.setProperty("wadi.db.driver", "org.apache.derby.jdbc.EmbeddedDriver");
		Manager manager=null;
		try {
			InputStream descriptor=new FileInputStream("src/webapp/WEB-INF/wadi-web.xml");
			manager=SpringManagerFactory.create(descriptor, "SessionManager");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		assertTrue(manager!=null);
		manager.init(null);
		// manager.start();
		// manager.stop();
	}
}
