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
package org.codehaus.wadi.sandbox.context.impl.jetty;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.sandbox.context.impl.AbstractHttpProxy;
import org.mortbay.http.HttpException;
import org.mortbay.http.HttpResponse;
import org.mortbay.http.handler.AbstractHttpHandler;

/**
 * TODO - JavaDoc this type
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public class Handler extends AbstractHttpHandler {
	protected Log _log = LogFactory.getLog(getClass());

	public void handle(String pathInContext, String pathParams, org.mortbay.http.HttpRequest request, HttpResponse response) throws HttpException, IOException {
		if (isProxied(request) && hasSecureOrigin(request)) {
			// TODO - we should check the remote end's IP against a regexp to ensure
			// that we are not being spoofed...
			_log.info("securing proxied request: "+request.getRequestURL());
			((Securable)request.getHttpConnection()).setSecure(true);
		}
	}
	
	protected boolean isProxied(org.mortbay.http.HttpRequest request) {
		return request.containsField("X-Forwarded-For");
	}

	protected boolean hasSecureOrigin(org.mortbay.http.HttpRequest request) {
		return request.containsField(AbstractHttpProxy._WADI_IsSecure);
	}
}
