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

package org.codehaus.wadi.test.container;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.codehaus.wadi.Manager;


public class
  FilterChain
  implements javax.servlet.FilterChain
{
  protected final Manager _manager;
  protected final Invocation _invocation;

  public
    FilterChain(Manager manager, Invocation invocation)
  {
    _manager=manager;
    _invocation=invocation;
  }

  public void
    doFilter(ServletRequest request, ServletResponse response)
    throws IOException, ServletException
  {
    _invocation.invoke(_manager, request, response);
  }
}
