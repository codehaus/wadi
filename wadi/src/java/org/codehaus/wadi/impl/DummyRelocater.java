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
package org.codehaus.wadi.impl;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.wadi.Immoter;
import org.codehaus.wadi.Relocater;
import org.codehaus.wadi.RelocaterConfig;

import EDU.oswego.cs.dl.util.concurrent.Sync;

public class DummyRelocater implements Relocater {

    public void init(RelocaterConfig config) {
        // empty
    }

    public void destroy() {
        // empty
    }

    public boolean relocate(HttpServletRequest hreq, HttpServletResponse hres, FilterChain chain, String name, Immoter immoter, Sync motionLock) throws IOException, ServletException {
        return true;
    }

}
