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

package org.codehaus.wadi.jetty.jmx;

import javax.management.MBeanException;
import org.codehaus.wadi.jetty.Manager;
import org.mortbay.util.jmx.LifeCycleMBean;

public class
  ManagerMBean
  extends LifeCycleMBean
{
  public ManagerMBean() throws MBeanException {}
  public ManagerMBean(Manager object) throws MBeanException {super(object);}

  protected void
    defineManagedResource()
  {
    super.defineManagedResource();
    // jetty/Manager
    defineAttribute("houseKeepingInterval");
    defineAttribute("httpPort");
    defineAttribute("sessionCookieDomain");
    defineAttribute("sessionCookieName");
    defineAttribute("sessionUrlParamName");
    // shared/Manager
    defineAttribute("distributable");
    defineAttribute("maxInactiveInterval");
    defineAttribute("specificationVersion");
  }
}
