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

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * A Class that can be instantiated to a Filter that will wrap-n-delegate to
 * another Filter instance.
 * 
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell </a>
 * @version $Revision$
 */

public class FilterInstance implements Filter {

	protected FilterConfig _config;
	protected Filter _instance;

	public void init(FilterConfig config) throws ServletException {
		_config=config;
	}

	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws ServletException, IOException {
		_instance.doFilter(req, res, chain);
	}

	public void destroy() {
		_instance.destroy();
		_instance=null;
	}

	public void setInstance(Filter instance) throws ServletException {
		_instance=instance;
		_instance.init(_config);
	}

	public Filter getInstance() {
		return _instance;
	}
}

