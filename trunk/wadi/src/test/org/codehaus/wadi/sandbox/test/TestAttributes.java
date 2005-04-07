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

import java.io.IOException;
import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.StreamingStrategy;
import org.codehaus.wadi.impl.SimpleStreamingStrategy;
import org.codehaus.wadi.sandbox.Attributes;
import org.codehaus.wadi.sandbox.Dirtier;
import org.codehaus.wadi.sandbox.impl.PartAttributes;
import org.codehaus.wadi.sandbox.impl.ReadWriteDirtier;
import org.codehaus.wadi.sandbox.impl.SimpleAttributes;
import org.codehaus.wadi.sandbox.impl.WholeAttributes;
import org.codehaus.wadi.sandbox.impl.WriteDirtier;

import junit.framework.TestCase;

/**
 * TODO - JavaDoc this type
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public class TestAttributes extends TestCase {
	protected Log _log = LogFactory.getLog(getClass());
    
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
    public TestAttributes(String name) {
        super(name);
    }
    
    public static int _serialisations=0;
    public static int _deserialisations=0;
    
    public static class Counter implements Serializable {
        
        double _random=Math.random();
        
        public boolean equals(Object that) {
            return (that==this) || ((that instanceof Counter) && ((Counter)that)._random==this._random);
        }
        
        private void writeObject(java.io.ObjectOutputStream out) throws IOException {
            _serialisations++;
            out.defaultWriteObject();
        }
        
        private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
            in.defaultReadObject();
            _deserialisations++;
        }
    }
    
    public void testAttributesWrapper() throws Exception {
        Dirtier dirtier=new WriteDirtier();
        StreamingStrategy streamer=new SimpleStreamingStrategy();
        boolean evictObjectRepASAP=false;
        boolean evictByteRepASAP=false;

        evictObjectRepASAP=false;
        evictByteRepASAP=false;
        testSerialisation(new WholeAttributes(dirtier, streamer, evictObjectRepASAP, evictByteRepASAP), evictObjectRepASAP, evictByteRepASAP, false);
        evictObjectRepASAP=true;
        evictByteRepASAP=false;
        testSerialisation(new WholeAttributes(dirtier, streamer, evictObjectRepASAP, evictByteRepASAP), evictObjectRepASAP, evictByteRepASAP, false);
        evictObjectRepASAP=false;
        evictByteRepASAP=true;
        testSerialisation(new WholeAttributes(dirtier, streamer, evictObjectRepASAP, evictByteRepASAP), evictObjectRepASAP, evictByteRepASAP, false);
        evictObjectRepASAP=true;
        evictByteRepASAP=true;
        testSerialisation(new WholeAttributes(dirtier, streamer, evictObjectRepASAP, evictByteRepASAP), evictObjectRepASAP, evictByteRepASAP, false);
        
//        dirtier=new ReadWriteDirtier();
//        evictObjectRepASAP=false;
//        evictByteRepASAP=false;
//        testSerialisation(new WholeAttributesWrapper(new SimpleAttributes(), dirtier, streamer, evictObjectRepASAP, evictByteRepASAP), evictObjectRepASAP, evictByteRepASAP, true);
//        evictObjectRepASAP=true;
//       evictByteRepASAP=false;
//        testSerialisation(new WholeAttributesWrapper(new SimpleAttributes(), dirtier, streamer, evictObjectRepASAP, evictByteRepASAP), evictObjectRepASAP, evictByteRepASAP, true);
//        evictObjectRepASAP=false;
//        evictByteRepASAP=true;
//        testSerialisation(new WholeAttributesWrapper(new SimpleAttributes(), dirtier, streamer, evictObjectRepASAP, evictByteRepASAP), evictObjectRepASAP, evictByteRepASAP, true);
//        evictObjectRepASAP=true;
//        evictByteRepASAP=true;
//        testSerialisation(new WholeAttributesWrapper(new SimpleAttributes(), dirtier, streamer, evictObjectRepASAP, evictByteRepASAP), evictObjectRepASAP, evictByteRepASAP, true);
    }
    
    public void testWholeAttributes() throws Exception {
        testAttributes(new SimpleAttributes());
        testAttributes(new SimpleAttributes());
        testAttributes(new SimpleAttributes());
        testAttributes(new SimpleAttributes());
        testAttributes(new SimpleAttributes());
        testAttributes(new SimpleAttributes());
        testAttributes(new SimpleAttributes());
        testAttributes(new SimpleAttributes());

        testAttributes(new WholeAttributes(new WriteDirtier(), new SimpleStreamingStrategy(), false, false));
        testAttributes(new WholeAttributes(new WriteDirtier(), new SimpleStreamingStrategy(), true, false));
        testAttributes(new WholeAttributes(new WriteDirtier(), new SimpleStreamingStrategy(), false, true));
        testAttributes(new WholeAttributes(new WriteDirtier(), new SimpleStreamingStrategy(), true, true));
        testAttributes(new WholeAttributes(new ReadWriteDirtier(), new SimpleStreamingStrategy(), false, false));
        testAttributes(new WholeAttributes(new ReadWriteDirtier(), new SimpleStreamingStrategy(), true, false));
        testAttributes(new WholeAttributes(new ReadWriteDirtier(), new SimpleStreamingStrategy(), false, true));
        testAttributes(new WholeAttributes(new ReadWriteDirtier(), new SimpleStreamingStrategy(), true, true));

        testAttributes(new PartAttributes(new WriteDirtier(), new SimpleStreamingStrategy(), false, false));
        testAttributes(new PartAttributes(new WriteDirtier(), new SimpleStreamingStrategy(), true, false));
        testAttributes(new PartAttributes(new WriteDirtier(), new SimpleStreamingStrategy(), false, true));
        testAttributes(new PartAttributes(new WriteDirtier(), new SimpleStreamingStrategy(), true, true));
        testAttributes(new PartAttributes(new ReadWriteDirtier(), new SimpleStreamingStrategy(), false, false));
        testAttributes(new PartAttributes(new ReadWriteDirtier(), new SimpleStreamingStrategy(), true, false));
        testAttributes(new PartAttributes(new ReadWriteDirtier(), new SimpleStreamingStrategy(), false, true));
        testAttributes(new PartAttributes(new ReadWriteDirtier(), new SimpleStreamingStrategy(), true, true));

    }

    public void testAttributes(Attributes a) throws Exception {
        // can we serialise an empty instance OK ?
        byte[] bytes=a.getBytes();
        // and then put the result back in ?
        a.setBytes(bytes);
        
        String key="foo";
        Object val=key;
        a.put(key, val);
        assertTrue(a.get(key).equals(val));
        bytes=a.getBytes();
        a.remove(key);
        assertTrue(a.get(key)==null);
        a.setBytes(bytes);
        assertTrue(a.get(key).equals(val));
    }
    
    public void testSerialisation(Attributes wrapper, boolean evictObjectRepASAP, boolean evictByteRepASAP, boolean readIsDirty) throws Exception {
        String key="foo";
        Counter val=new Counter();
        int serialisations=0;
        _serialisations=serialisations;
        int deserialisations=0;
        _deserialisations=deserialisations;
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
        if (evictObjectRepASAP) {
            assertTrue(val.equals(wrapper.get(key)));
            deserialisations++;
        } else {
            assertTrue(val==wrapper.get(key));
        }
        // did this last operation affect effect the content ?
        assertTrue(_serialisations==serialisations);
        assertTrue(_deserialisations==deserialisations);
        // serialise the container again - should still not alter contents ...
        bytes=wrapper.getBytes();
        if (evictObjectRepASAP && evictByteRepASAP)
            serialisations++;
        assertTrue(_serialisations==serialisations);
        assertTrue(_deserialisations==deserialisations);
        // reinsert content, should invalidate serialised cache...
        wrapper.put(key, val);
        assertTrue(val==wrapper.get(key));
        if (evictObjectRepASAP && evictByteRepASAP)
            deserialisations++;
        assertTrue(_serialisations==serialisations);
        _log.info(""+_deserialisations+"=="+deserialisations);
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
        if (evictByteRepASAP) {
            serialisations++;
        }
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
