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
package org.codehaus.wadi.sandbox.distributable.test;

import java.io.IOException;
import java.io.Serializable;

import org.codehaus.wadi.StreamingStrategy;
import org.codehaus.wadi.impl.SimpleStreamingStrategy;
import org.codehaus.wadi.sandbox.Attributes;
import org.codehaus.wadi.sandbox.distributable.Dirtier;
import org.codehaus.wadi.sandbox.distributable.impl.WholeAttributesWrapper;
import org.codehaus.wadi.sandbox.distributable.impl.WriteDirtier;
import org.codehaus.wadi.sandbox.impl.SimpleAttributes;

import junit.framework.TestCase;

/**
 * TODO - JavaDoc this type
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public class TestDistribution extends TestCase {

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Constructor for TestDistribution.
     * @param name
     */
    public TestDistribution(String name) {
        super(name);
    }

    	public static int _serialisations=0;
    	public static int _deserialisations=0;

        public static class Counter implements Serializable {

        private void writeObject(java.io.ObjectOutputStream out) throws IOException {
            _serialisations++;
            out.defaultWriteObject();
        }

        private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
            in.defaultReadObject();
            _deserialisations++;
        }
    }

    public void testSerialisation() throws Exception {
        Dirtier dirtier=new WriteDirtier();
        StreamingStrategy streamer=new SimpleStreamingStrategy();
        boolean saveMemory=false;
        Attributes wrapper=new WholeAttributesWrapper(new SimpleAttributes(), dirtier, streamer, saveMemory);
        String key="foo";
        Counter val=new Counter();
	int serialisations=0;
	int deserialisations=0;

        // check initial state
        assertTrue(_serialisations==serialisations);
        assertTrue(_deserialisations==deserialisations);
        // insert into container and reinspect - expect no change
        wrapper.put(key, val);
        assertTrue(_serialisations==serialisations);
        assertTrue(_deserialisations==deserialisations);
        // can we retrieve the same reference ?
        assertTrue(val==wrapper.get(key));
        // try serialising container - should serialise content..
        byte[] bytes=wrapper.getBytes();
	serialisations++;
        assertTrue(_serialisations==serialisations);
        assertTrue(_deserialisations==deserialisations);
        // serialise container again - should used cached serialised content
        bytes=wrapper.getBytes();
        assertTrue(_serialisations==serialisations);
        assertTrue(_deserialisations==deserialisations);
        // can we still retrieve the original reference ?
        assertTrue(val==wrapper.get(key));
        // did this last operation affect effect the content ?
        assertTrue(_serialisations==serialisations);
        assertTrue(_deserialisations==deserialisations);
        // serialise the container again - should still not alter contents ...
        bytes=wrapper.getBytes();
        assertTrue(_serialisations==serialisations);
        assertTrue(_deserialisations==deserialisations);
        // reinsert content, should invalidate serialised cache...
        wrapper.put(key, val);
        assertTrue(val==wrapper.get(key));
        assertTrue(_serialisations==serialisations);
        assertTrue(_deserialisations==deserialisations);
        bytes=wrapper.getBytes();
	serialisations++;
        assertTrue(_serialisations==serialisations);
        assertTrue(_deserialisations==deserialisations);

        // Looks good - now let's try deserialising...

        // populate the container - should not change old content state...
        wrapper.setBytes(bytes);
        assertTrue(_serialisations==serialisations);
        assertTrue(_deserialisations==deserialisations);
        // retrieve content - should cause deserialisation
        val=(Counter)wrapper.get(key);
	deserialisations++;
        assertTrue(_serialisations==serialisations);
        assertTrue(_deserialisations==deserialisations);
        // retrieve content again - should be found in cache - no deserialisation...
        val=(Counter)wrapper.get(key);
        assertTrue(_serialisations==serialisations);
        assertTrue(_deserialisations==deserialisations);
        // reinitialise content - should invalidate object cache...
        bytes=wrapper.getBytes();
        assertTrue(_serialisations==serialisations);
        assertTrue(_deserialisations==deserialisations);
        wrapper.setBytes(bytes);
        assertTrue(_serialisations==serialisations);
        assertTrue(_deserialisations==deserialisations);
        // reretrieve content - should cause fresh deserialisation
        val=(Counter)wrapper.get(key);
	deserialisations++;
        assertTrue(_serialisations==serialisations);
        assertTrue(_deserialisations==deserialisations);

        // TODO:
        // cool - lots more to do...
        // add a second counter
        // try with a PartAttributes
        // try with saveMem=true
        // tyr other things...
    }
}
