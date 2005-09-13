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
package org.codehaus.wadi.jetty6;

import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.Securable;
import org.mortbay.jetty.HttpConnection;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.nio.SelectChannelConnector;

/**
 * A Jetty Listener, which defines a type of Connection on which we may set a flag to indicate whether
 * it should be considered secure.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public class Connector extends SelectChannelConnector {
	
	protected final Log _log=LogFactory.getLog(getClass());
	protected final Pattern _trustedIps;
	
	public Connector(Pattern trustedIps) {
		_trustedIps=trustedIps;
		if (_log.isInfoEnabled()) _log.info("WADI Handler in place: "+_trustedIps.pattern());
	}
	
	public boolean isConfidential(Request request) {
		// request must have been :
		//  proxied by WADI
		String field=request.getHeader("Via");
		if (field!=null && field.endsWith("\"WADI\"")) { // TODO - should we ignore case ?
			String ip=request.getRemoteAddr();
			//  from a trusted IP...
			if (_trustedIps.matcher(ip).matches()) {
				if (_log.isTraceEnabled()) _log.trace("securing proxied request: "+request.getRequestURL());
				return true;
			} else {
				// otherwise we have a configuration issue or are being spoofed...
				if (_log.isWarnEnabled()) _log.warn("purported WADI request arrived from suspect IP address: "+_trustedIps.pattern()+" !~ "+ip);
				return false;
			}
		}
		return true;
	}
	
}
