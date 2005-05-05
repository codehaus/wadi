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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.wadi.Router;

public class DummyRouter implements Router {

    public String strip(String session) {
        return session;
    }

    public String augment(String session) {
        return session;
    }

    public String getInfo() {
        return "";
    }

    public boolean canReroute() {
        return false;
    }
    
    public boolean reroute(HttpServletRequest req, HttpServletResponse res) {
        return false;
    }

    public boolean rerouteCookie(HttpServletRequest req, HttpServletResponse res, String id) {
        return false;
    }

    public boolean rerouteCookie(HttpServletRequest req, HttpServletResponse res, String id, String route) {
        return false;
    }

    public boolean rerouteURL() {
        return false;
    }

    public boolean rerouteURL(String target) {
        return false;
    }

}
