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

package org.codehaus.wadi;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Cookie;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

// TODO - will a redirect work in all cases ?

/**
 * Provides necessary fn-ality for redirecting HttpRequests between WADI nodes.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class
  HttpRedirect
{
  protected static Log _log = LogFactory.getLog(HttpRedirect.class);

  public static void
    process(String hostname, int port, HttpServletRequest request, HttpServletResponse response)
  {
    response.setContentLength(0);
    String pathInfo=request.getPathInfo();
    pathInfo=pathInfo==null?"":pathInfo;
    String url=response.encodeRedirectURL(request.getContextPath()+ request.getServletPath()+ pathInfo);

    String ourRoutingInfo="";
    String theirRoutingInfo="";

    // TODO - PICK UP JSESSIONID STRING FROM MANAGER...

    if (!request.isRequestedSessionIdFromCookie()
	// || !request.isUsingCookies() // how do we work this out ?
	)
      url.replaceAll(ourRoutingInfo,theirRoutingInfo); // TODO - what if url itself contains ourRoutingInfo
    else
    {
      Cookie[] cookies=request.getCookies(); // why isn't there a getCookie(String name)?
      for (int i=0; i<cookies.length; i++)
      {
	Cookie c=cookies[i];
	if (c.getName().equals("JSESSIONID"))
	{
	  String value=c.getValue();
	  value.replaceAll(ourRoutingInfo,theirRoutingInfo);
	  c.setValue(value);
	  response.addCookie(c);	// will this work ?
	  break;
	}
      }
    }

    try
    {
      response.sendRedirect(url);
    }
    catch (IOException e)
    {
      _log.warn("unexpected problem redirecting request", e);
    }
  }
}
