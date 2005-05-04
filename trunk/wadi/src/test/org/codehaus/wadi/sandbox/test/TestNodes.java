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
package org.codehaus.wadi.sandbox.test;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpConnection;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.methods.GetMethod;
import org.mortbay.jetty.Server;

import junit.framework.TestCase;

public class TestNodes extends TestCase {

    public TestNodes(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.Jdk14Logger");
        System.setProperty("java.util.logging.config.file", "lib/logging.properties");
        System.setProperty("wadi.home", "/home/jules/workspace/wadi");
        System.setProperty("jetty.home", "/usr/local/java/jetty-5.1.3");
        System.setProperty("org.mortbay.xml.XmlParser.NotValidating", "true");
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    // run up two Jetty's
    // create a session on one
    // ask for session at other
    // confirm that request was proxied
    // confirm that cookie has been rerouted
    public void testRequestRelocation() throws Exception {
        assertTrue(true);
        System.setProperty("http.port", "8080");
        System.setProperty("ajp.port", "8009");
        System.setProperty("STOP.PORT", "8040");
        System.setProperty("wadi.colour", "red");
        Server red=new Server("/home/jules/workspace/wadi/conf/jetty-wadi2.xml");
        red.start();
        Thread.sleep(2000);
        System.out.println("");
        
        System.setProperty("http.port", "8081");
        System.setProperty("ajp.port", "8010");
        System.setProperty("STOP.PORT", "8041");
        System.setProperty("wadi.colour", "green");
        Server green=new Server("/home/jules/workspace/wadi/conf/jetty-wadi2.xml");
        green.start();
        Thread.sleep(2000);
        System.out.println("");

        HttpConnection connection=new HttpConnection("localhost", 8080);
        HttpState state=new HttpState();
        assertTrue(state.getCookies().length==0);
        GetMethod get=new GetMethod("/wadi/jsp/create.jsp");
        get.execute(state, connection);
        
        // how do the cookies look ?
        Cookie[] cookies=state.getCookies();
        assertTrue(cookies.length==1);
        Cookie session=cookies[0];
        String oldValue=session.getValue();
        assertTrue(oldValue.endsWith(".red"));
        String root=oldValue.substring(0, oldValue.lastIndexOf("."));
        String newValue=root+"."+"green";
        session.setValue(newValue);
        Thread.sleep(2000);
        System.out.println("");
        
        get=new GetMethod("/wadi/jsp/create.jsp");
        get.execute(state, new HttpConnection("localhost", 8081));
        // how do the cookies look ?
        cookies=state.getCookies();
        assertTrue(cookies.length==1);
        assertTrue(cookies[0].getValue().endsWith(".red"));
        Thread.sleep(2000);
        System.out.println("");
        
        red.stop();
        Thread.sleep(2000);
        System.out.println("");
        //green.stop();
        Thread.sleep(2000);
        System.out.println("");
    }
}
