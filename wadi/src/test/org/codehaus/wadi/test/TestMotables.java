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
package org.codehaus.wadi.test;

import java.io.File;

import org.codehaus.wadi.ExclusiveDiscMotableConfig;
import org.codehaus.wadi.Motable;
import org.codehaus.wadi.impl.ExclusiveDiscMotable;
import org.codehaus.wadi.impl.SimpleMotable;

import junit.framework.TestCase;

public class TestMotables extends TestCase {

    public TestMotables(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    public void testMotables() throws Exception {
        assertTrue(true);
        
        Motable sm0=new SimpleMotable();
        long creationTime=System.currentTimeMillis();
        long lastAccessedTime=creationTime+1;
        int maxInactiveInterval=30*60;
        String name="foo";
        byte[] bytes=new byte[]{0,1,2,3,4,5,6,7,8,9};
        
        sm0.init(creationTime, lastAccessedTime, maxInactiveInterval, name);
        sm0.setBytes(bytes);
        
        ExclusiveDiscMotableConfig config=new ExclusiveDiscMotableConfig() {
            protected File _dir=new File("/tmp");
            public File getDirectory() {return _dir;}
            public String getSuffix() {return ".ser";}
        };
        
        File file=new File(new File("/tmp"), name+".ser");
        assertTrue(!file.exists());
        
        // ExclusiveDiscMotable...
        ExclusiveDiscMotable edm0=new ExclusiveDiscMotable();
        edm0.init(config);
        assertTrue(!file.exists());
        edm0.copy(sm0); // should create file
        assertTrue(file.exists());
        edm0=null;
        
        ExclusiveDiscMotable edm1=new ExclusiveDiscMotable();
        edm1.init(config, name); // should load file
        assertTrue(file.exists());
       
        Motable sm1=new SimpleMotable();
        sm1.copy(edm1);
        assertTrue(file.exists());
        
        edm1.destroy(); // should remove file
        assertTrue(!file.exists());
        
        assertTrue(sm0.equals(sm1));
    }

}
