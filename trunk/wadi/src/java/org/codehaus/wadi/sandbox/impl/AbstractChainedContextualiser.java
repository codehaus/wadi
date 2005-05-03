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
package org.codehaus.wadi.sandbox.impl;

import org.codehaus.wadi.sandbox.Contextualiser;
import org.codehaus.wadi.sandbox.ContextualiserConfig;
import org.codehaus.wadi.sandbox.Evictable;

public abstract class AbstractChainedContextualiser extends AbstractContextualiser {

  protected final Contextualiser _next;

  public AbstractChainedContextualiser(Contextualiser next) {
    _next=next;
  }

  public void init(ContextualiserConfig config) {
    super.init(config);
    _next.init(config);
  }

  public void start() throws Exception {
    super.start();
    _next.start();
  }

  public void stop() throws Exception {
    _next.stop();
    super.stop();
  }

  public void destroy() {
    _next.destroy();
    super.destroy();
  }

  public void setLastAccessedTime(Evictable evictable, long oldTime, long newTime) {/* do nothing */}
  public void setMaxInactiveInterval(Evictable evictable, int oldInterval, int newTime) {/* do nothing */}
}
