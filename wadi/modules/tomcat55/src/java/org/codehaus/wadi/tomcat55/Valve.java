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
package org.codehaus.wadi.tomcat55;

import java.io.IOException;
import java.util.regex.Pattern;

import javax.servlet.ServletException;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A Tomcat Valvewhich checks incoming proxied requests to see if they were originally from
 * a secure connection. If this is the case, it modifies the request object so that it encodes
 * this fact.
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
		if (_log.isInfoEnabled()) _log.info("WADI Valve in place: "+_trustedIps.pattern());
	}

	public void invoke(Request request, Response response) throws IOException, ServletException {
		// request must have been :
		//  proxied by WADI
		String val=request.getHeader("Via");
		if (val!=null && val.endsWith("\"WADI\"")) { // TODO - should we ignore case ?
			String ip=request.getRemoteAddr();
			//  from a trusted IP...
			if (_trustedIps.matcher(ip).matches()) {
				if (_log.isTraceEnabled()) _log.trace("securing proxied request: "+request.getRequestURL());
				request.setSecure(true);
			} else {
				// otherwise we have a configuration issue or are being spoofed...
				if (_log.isWarnEnabled()) _log.warn("purported WADI request arrived from suspect IP address: "+_trustedIps.pattern()+" !~ "+ip);
			}
		}
		getNext().invoke(request, response);
	}

}
