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
package org.codehaus.wadi.sandbox.context.impl.jetty;

import java.io.IOException;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.sandbox.context.Securable;
import org.mortbay.http.HttpException;
import org.mortbay.http.HttpRequest;
import org.mortbay.http.HttpResponse;
import org.mortbay.http.handler.AbstractHttpHandler;

/**
 * TODO - JavaDoc this type
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public class Handler extends AbstractHttpHandler {
	protected final Log _log=LogFactory.getLog(getClass());
	protected final Pattern _trustedIps;

	public Handler(Pattern trustedIps) {
		_trustedIps=trustedIps;
	}

	public void handle(String pathInContext, String pathParams, HttpRequest request, HttpResponse response) throws HttpException, IOException {
		// request must have been :
		//  proxied by WADI
		String field=request.getField("Via");
		if (field!=null && field.endsWith("\"WADI\"")) { // TODO - should we ignore case ?
			String ip=request.getRemoteAddr();
			//  from a trusted IP...
			if (_trustedIps.matcher(ip).matches()) {
				_log.info("securing proxied request: "+request.getRequestURL());
				((Securable)request.getHttpConnection()).setSecure(true);
			} else {
				// otherwise we have a configuration issue or are being spoofed...
				_log.warn("purported WADI request arrived from suspect IP address: "+_trustedIps.pattern()+" !~ "+ip);
			}
		}
	}
}
