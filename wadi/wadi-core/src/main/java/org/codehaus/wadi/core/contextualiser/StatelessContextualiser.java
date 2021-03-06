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
package org.codehaus.wadi.core.contextualiser;

import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.core.motable.Immoter;
import org.codehaus.wadi.web.HttpInvocationContext;
import org.codehaus.wadi.web.impl.BasicHttpInvocationContext;
import org.codehaus.wadi.web.impl.WebInvocation;

//N.B.
//Ultimately this should support pluggable and combinable 'Tests' - i.e. URIPatternTest
//MethodPatternTest & AndTest...

/**
 * A Contextualiser that will intercept requests that can be shown to be stateless
 * and run them in a generic stateless Context immediately, without the overhead of
 * locating the (possibly remote) relevant Context.
 *
 * Logically, this Contextualiser should sit at the top of the stack, preventing
 * unecessary cycles being spent locating state that will not actually be consumed
 * by the incoming request. Actually, taking into account the expense of performing
 * this check, vs. the expense of checking locally for the session, or locating a
 * remote session, the sensible place to deploy this Contextualiser may be at the
 * boundary between local and remote Contextualisers.
 *
 * If you are caching static content agressively you may not need this Contextualiser.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class StatelessContextualiser extends AbstractDelegatingContextualiser {

	protected final Pattern _methods; // =Pattern.compile("GET|POST", Pattern.CASE_INSENSITIVE); // TODO - |HEAD|PUT|DELETE ?
	protected final boolean _methodFlag; //true
	protected final Pattern _uris; //=Pattern.compile(".*\\.(JPG|JPEG|GIF|PNG|ICO|HTML|HTM)", Pattern.CASE_INSENSITIVE); // TODO - CSS, ...?
	protected final boolean _uriFlag; // false
	protected final Log _lockLog=LogFactory.getLog("org.codehaus.wadi.LOCKS");

	/**
	 * @param next - The next Contextualiser in the stack
	 * @param methods - Pattern used to match HTTP method names (null will match nothing)
	 * @param methodFlag - Does this Pattern match stateful (true) or stateless (false) HTTP methods
	 * @param uris - Pattern used to match URIs (null will match nothing)
	 * @param uriFlag - Does this Pattern match stateful (true) or stateless (false) URIs
	 */
	public StatelessContextualiser(Contextualiser next, Pattern methods, boolean methodFlag, Pattern uris, boolean uriFlag) {
		super(next);
		_methods=methods;
		_methodFlag=methodFlag;
		_uris=uris;
		_uriFlag=uriFlag;
	}

	public boolean contextualise(Invocation invocation, Object id, Immoter immoter, boolean exclusiveOnly) throws InvocationException {
		WebInvocation webInvocation = (WebInvocation) invocation;
		HttpServletRequest hreq = webInvocation.getHreq();
		if (hreq==null || isStateful(hreq)) {
			// we cannot optimise...
			return next.contextualise(invocation, id, immoter, exclusiveOnly);
		} else {
			// wrap the request so that session is inaccessible and process here...
            HttpInvocationContext invocationContext = new BasicHttpInvocationContext(invocation);
			invocation.invoke(invocationContext);
			return true;
		}
	}

	/**
	 * We know request is stateful - if, either Pattern matches
	 * stateFULL requests AND match succeeded, or Pattern matches
	 * stateLESS requests AND matched failed
	 *
	 * @param hreq
	 * @return - whether, or not, the request must be assumed stateful
	 */
	public boolean isStateful(HttpServletRequest hreq) {
		// TODO - should the order of matching be configurable ?
		boolean matched;

		// can we prove it is stateless ? - try first test...
		if (_methods!=null) {
			matched=(_methods.matcher(hreq.getMethod()).matches());
			if (matched!=_methodFlag)
				return false;
		}

		// could still be stateful - try second test...
		if (_uris!=null) {
			matched=(_uris.matcher(hreq.getRequestURI()).matches());
			if (matched!=_uriFlag)
				return false;
		}

		// we cannot eliminate the possibility that the request is stateful...
		return true;
	}
}
