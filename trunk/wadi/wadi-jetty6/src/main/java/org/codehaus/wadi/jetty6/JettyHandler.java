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

import java.io.IOException;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.jetty.handler.AbstractHandler;

public class JettyHandler extends AbstractHandler {
  
  protected final Log _log = LogFactory.getLog(getClass());
  protected final Pattern _trustedIps;
  
  public JettyHandler(Pattern trustedIps) {
    _trustedIps=trustedIps;
    if (_log.isInfoEnabled()) _log.info("WADI Handler in place: "+_trustedIps.pattern());
  }
  
  public boolean handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch) throws IOException,ServletException {
    // we need to decide whether to run the invocation locally or remotely...
    
    _log.info("HANDLER CALLED!");
    return false;
  }
  
}
