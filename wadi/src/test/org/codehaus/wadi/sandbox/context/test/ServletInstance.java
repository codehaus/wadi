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
package org.codehaus.wadi.sandbox.context.test;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * A Class that will instantiate to a Servlet that will wrap-n-delegate to
 * another Servlet instance.
 * 
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell </a>
 * @version $Revision$
 */

public class ServletInstance implements Servlet {

	protected ServletConfig _config;
	protected Servlet _instance;

	public void init(ServletConfig config) throws ServletException {
		_config=config;
	}

	public ServletConfig getServletConfig() {
		return _instance.getServletConfig();
	}

	public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
		_instance.service(req, res);
	}

	public String getServletInfo() {
		return _instance.getServletInfo();
	}

	public void destroy() {
		_instance.destroy();
		_instance=null;
	}

	public void setInstance(Servlet instance) throws ServletException {
		_instance=instance;
		_instance.init(_config);
	}

	public Servlet getInstance() {
		return _instance;
	}
}
