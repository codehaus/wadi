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
package org.codehaus.wadi.sandbox.impl.tomcat;

import java.io.IOException;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.catalina.HttpRequest;
import org.apache.catalina.Request;
import org.apache.catalina.Response;
import org.apache.catalina.ValveContext;
import org.apache.catalina.valves.ValveBase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * TODO - JavaDoc this type
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

// how do we install this ?

public class Valve extends ValveBase {
	protected final Log _log=LogFactory.getLog(getClass());
	protected final Pattern _trustedIps;

	public Valve(Pattern trustedIps) {
		_trustedIps=trustedIps;
	}

	/* (non-Javadoc)
	 * @see org.apache.catalina.Valve#invoke(org.apache.catalina.Request, org.apache.catalina.Response, org.apache.catalina.ValveContext)
	 */
	public void invoke(Request request, Response response, ValveContext context) throws IOException, ServletException {
		if (request instanceof HttpRequest) {
			HttpServletRequest hreq=(HttpServletRequest) request;
			// request must have been :
			//  proxied by WADI
			String val=hreq.getHeader("Via");
			if (val!=null && val.endsWith("\"WADI\"")) { // TODO - should we ignore case ?
				String ip=hreq.getRemoteAddr();
				//  from a trusted IP...
				if (_trustedIps.matcher(ip).matches()) {
					_log.info("securing proxied request: "+hreq.getRequestURL());
					request.setSecure(true);
				} else {
					// otherwise we have a configuration issue or are being spoofed...
					_log.warn("purported WADI request arrived from suspect IP address: "+_trustedIps.pattern()+" !~ "+ip);
				}
			}
		}
		context.invokeNext(request, response);
	}
}
