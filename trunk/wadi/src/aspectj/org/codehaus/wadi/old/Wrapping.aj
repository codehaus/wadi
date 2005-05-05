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

package org.codehaus.wadi.old;

//----------------------------------------

// every attribute is wrapped up so that we can manage it's
// passivation and (lazy) activation. This is necessary to:

// 1. ensure that replicant nodes do not bother demarshalling attribute values
// 2. HttpSessionActivationListeners can be notified correctly
// 3. non-serialisable types which require support can be worked around (EJBs etc...)

// NB. we could use the same HttpSessionEvents across the whole
// session. They should be built lazily and cached somewhere upon it.

//----------------------------------------

// TODO - Distributable sessions only...

/**
 * Wraps session attributes in a Wrapper responsible for management of
 * their [de]serialisation.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public aspect
  Wrapping
{
  pointcut setAttribute(AbstractHttpSessionImpl ahsi, String key, Object val, boolean returnVal) :
    execution(Object HttpSessionSetters.setAttribute(String, Object, boolean)) && args(key, val, returnVal) && target(ahsi);

  Object
    around(AbstractHttpSessionImpl ahsi, String key, Object val, boolean returnVal)
    : setAttribute(ahsi, key, val, returnVal)
    {
      Wrapper newVal=wrap(ahsi, key, val);
      Wrapper oldVal=(Wrapper)proceed(ahsi, key, newVal, true);

      return returnVal?unwrap(ahsi, key, oldVal):null;
    }

  pointcut getAttribute(AbstractHttpSessionImpl ahsi, String key) :
    execution(Object HttpSessionGetters.getAttribute(String)) && args(key) && target(ahsi);

  Object
    around(AbstractHttpSessionImpl ahsi, String key)
    : getAttribute(ahsi, key)
    {
      return unwrap(ahsi, key, proceed(ahsi, key));
    }

  pointcut removeAttribute(AbstractHttpSessionImpl ahsi, String key, boolean returnVal) :
    execution(Object HttpSessionSetters.removeAttribute(String, boolean)) && args(key, returnVal) && target(ahsi);

  Object
    around(AbstractHttpSessionImpl ahsi, String key, boolean returnVal)
    : removeAttribute(ahsi, key, returnVal)
    {
      return returnVal?unwrap(ahsi, key, proceed(ahsi, key, true)):null;
    }

  protected Wrapper
    wrap(AbstractHttpSessionImpl ahsi, String name, Object value)
    {
      // needs to select relevant wrapper subclass...
      return new Wrapper(ahsi, value);
    }

  protected Object
    unwrap(AbstractHttpSessionImpl ahsi, String name, Object value)
    {
      return value==null?null:((Wrapper)value).getValue(ahsi);
    }
}
