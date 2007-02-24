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

import javax.servlet.http.HttpSession;

import org.codehaus.wadi.Manager;
import org.codehaus.wadi.impl.SpringManagerFactory;
import org.codehaus.wadi.web.WebSession;
import org.codehaus.wadi.web.WebSessionWrapperFactory;
import org.codehaus.wadi.web.impl.AtomicallyReplicableSessionFactory;

import junit.framework.TestCase;

/**
 * Confirm that the current wad-web.xml will construct a valid
 * WADI installation.
 * 
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class TestWadiWebXml extends TestCase {

	public void testWadiWebXml() throws Exception {
		System.setProperty("http.port", "8080");
		System.setProperty("node.name", "test-"+new Random().nextInt());
		Manager manager=null;
		try {
			InputStream descriptor=new FileInputStream("src/webapp/WEB-INF/wadi-web.xml");
			manager=SpringManagerFactory.create(descriptor,
					"SessionManager",
					new AtomicallyReplicableSessionFactory(),
					new WebSessionWrapperFactory() {
						public HttpSession create(WebSession arg0) {
							return null;
						}});
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		assertTrue(manager!=null);
		manager.init(null);
		manager.start();
		manager.stop();
	}
}