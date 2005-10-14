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

import javax.sql.DataSource;

import org.axiondb.jdbc.AxionDataSource;
import org.codehaus.wadi.Motable;
import org.codehaus.wadi.Store;
import org.codehaus.wadi.StoreMotable;
import org.codehaus.wadi.impl.DatabaseMotable;
import org.codehaus.wadi.impl.DatabaseStore;
import org.codehaus.wadi.impl.DiscStore;
import org.codehaus.wadi.impl.SimpleMotable;
import org.codehaus.wadi.impl.SimpleStreamer;

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
    
    public void testDatabaseMotables() throws Exception {
        DataSource ds=new AxionDataSource("jdbc:axiondb:testdb");
        String table="SESSIONS";
        DatabaseMotable.init(ds, table);
        testDatabaseMotables(new DatabaseStore("test", ds, table, false));
//        testDatabaseMotables(new DiscStore(new SimpleStreamer(), new File("/tmp"), true));
    }

    public void testDatabaseMotables(Store store) throws Exception {
    }
    
    public void testDiscMotables() throws Exception {
        testDiscMotables(new DiscStore(new SimpleStreamer(), new File("/tmp"), false));
        testDiscMotables(new DiscStore(new SimpleStreamer(), new File("/tmp"), true));
    }
 
    public void testDiscMotables(Store store) throws Exception {
        assertTrue(true);
        
        Motable sm0=new SimpleMotable();
        long creationTime=System.currentTimeMillis();
        long lastAccessedTime=creationTime+1;
        int maxInactiveInterval=30*60;
        String name="foo";
        byte[] bytes=new byte[]{'a','b','c','d','e','f'};
        
        sm0.init(creationTime, lastAccessedTime, maxInactiveInterval, name);
        sm0.setBodyAsByteArray(bytes);
        
        File file=new File(new File("/tmp"), name+".ser");
        file.delete();
        assertTrue(!file.exists());
        
        StoreMotable edm0=store.create();
        edm0.init(store);
        assertTrue(!file.exists());
        edm0.copy(sm0); // should create file
        assertTrue(file.exists());
        edm0=null;
        
        StoreMotable edm1=store.create();
        edm1.init(store, name); // should load file
        assertTrue(file.exists());
       
        Motable sm1=new SimpleMotable();
        sm1.copy(edm1);
        assertTrue(file.exists());
        
        edm1.destroy(); // should remove file
        assertTrue(!file.exists());
        
        assertTrue(sm0.equals(sm1));
    }
    
}
