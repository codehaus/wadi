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

import java.io.IOException;
import java.io.Serializable;

import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.AttributesConfig;
import org.codehaus.wadi.Streamer;
import org.codehaus.wadi.StreamerConfig;
import org.codehaus.wadi.Value;
import org.codehaus.wadi.ValueFactory;
import org.codehaus.wadi.ValueHelper;
import org.codehaus.wadi.ValuePool;
import org.codehaus.wadi.impl.DistributableValue;
import org.codehaus.wadi.impl.DistributableValueFactory;
import org.codehaus.wadi.impl.SimpleStreamer;
import org.codehaus.wadi.impl.SimpleValuePool;
import org.codehaus.wadi.impl.Utils;

import junit.framework.TestCase;

/**
 * Test Attribute and Attributes classes and subclasses...
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
    
    static class NotSerializable {
        
        final String _content;
        public NotSerializable(String content) {_content=content;}
        
        public boolean equals(Object that) {
            return this==that || (that instanceof NotSerializable && safeEquals(this._content, ((NotSerializable)that)._content));
        }
    }
    
    static class IsSerializable implements Serializable {
        String _content;
        public IsSerializable(){/* empty */} // for Serialising...
        public IsSerializable(String content){_content=content;} // for Helper

        private Object readResolve() {
            return new NotSerializable(_content);
        }
}	
    
    static class NotSerializableHelper implements ValueHelper {
        public Serializable replace(Object object) {return new IsSerializable(((NotSerializable)object)._content);}
    }
    
    public void testAttribute() throws Exception {
        ValueFactory factory=new DistributableValueFactory();
        ValuePool pool=new SimpleValuePool(factory); 
        
        Value a=pool.take(null);
        // test get/set
        assertTrue(a.getValue()==null);
        String foo="foo";
        a.setValue(foo);
        assertTrue(a.getValue()==foo);
        a.setValue(null);
        assertTrue(a.getValue()==null);
        pool.put(a);
        
        // test serialisation with various values
        {
            DistributableValue attr1=(DistributableValue)pool.take(null);
            DistributableValue attr2=(DistributableValue)pool.take(null);
            testAttributeSerialisation(attr1, attr2, null);
            pool.put(attr1);
            pool.put(attr2);
        }
        {
            DistributableValue attr1=(DistributableValue)pool.take(null);
            DistributableValue attr2=(DistributableValue)pool.take(null);
            testAttributeSerialisation(new DistributableValue(null), new DistributableValue(null), foo);
            pool.put(attr1);
            pool.put(attr2);
        }
        
// FIXME
        
//        // try using a Helper
//        DistributableValue.registerHelper(NotSerializable.class, new NotSerializableHelper());
//        {
//            DistributableValue attr1=(DistributableValue)pool.take(null);
//            DistributableValue attr2=(DistributableValue)pool.take(null);
//            testAttributeSerialisation(new DistributableValue(null), new DistributableValue(null), new NotSerializable(foo));
//            pool.put(attr1);
//            pool.put(attr2);
//        }
//        
//        // try without the Helper
//        assertTrue(DistributableValue.deregisterHelper(NotSerializable.class));
//        assertTrue(!DistributableValue.deregisterHelper(NotSerializable.class)); // can't be removed twice
//        {
//            DistributableValue attr1=(DistributableValue)pool.take(null);
//            DistributableValue attr2=(DistributableValue)pool.take(null);
//            try {
//                testAttributeSerialisation(attr1, attr2, new NotSerializable(foo));
//                assertTrue(false); // not expected
//            } catch (NotSerializableException ignore) {
//                // expected
//            }
//            pool.put(attr1);
//            pool.put(attr2);
//        }
    }
    

    static class ActivationListener implements HttpSessionActivationListener, Serializable {
        
        protected double _content=Math.random();
        public boolean equals(Object that) {
            return this==that || (that instanceof ActivationListener && this._content==((ActivationListener)that)._content);
        }
        
        int _activations;
        int _passivations;
        
        public void sessionDidActivate(HttpSessionEvent se){_activations++;}
        public void sessionWillPassivate(HttpSessionEvent se){_passivations++;}

    }
    
//    public void testActivatableAttribute() throws Exception {
//        ActivationListener al=new ActivationListener();
//        ValueFactory factory=new DistributableValueFactory();
//        ValuePool pool=new SimpleValuePool(factory); 
//        DistributableValue attr1=(DistributableValue)pool.take(null);
//        DistributableValue attr2=(DistributableValue)pool.take(null);
//        testAttributeSerialisation(attr1, attr2, al);
//        _log.info("passivations: "+al._passivations);
//        assertTrue(al._passivations==1);        
//        _log.info("activations: "+al._activations);
//        assertTrue(al._activations==1);
//        pool.put(attr1);
//        pool.put(attr2);
//    }
    
    public void testAttributeSerialisation(DistributableValue a, DistributableValue b, Object s) throws Exception {
        Streamer streamer=new SimpleStreamer();
        streamer.init(new StreamerConfig(){public ClassLoader getClassLoader() {return getClass().getClassLoader();}});
        a.setValue(s);
        byte[] bytes=Utils.getContent(a, streamer);
        assertTrue(a.getValue()==s); // activation
        Utils.setContent(b, bytes, streamer);
        assertTrue(safeEquals(b.getValue(), s)); // activation
        assertTrue(safeEquals(b.getValue(), s)); // do this twice to show that activation only occurs once (for this Attribute)
    }
    
    public static boolean safeEquals(Object a, Object b) {
        if (a==null)
            return b==null;
        else
            return a.equals(b);
    }
    
    public void testAtomicAttributes() throws Exception {
        AttributesConfig config=null;
//        AttributesFactory factory=new 
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
    
//    public void testAttributes() throws Exception {
//            Dirtier dirtier=new WriteDirtier();
//            StreamingStrategy streamer=new SimpleStreamingStrategy();
//            boolean evictObjectRepASAP=false;
//            boolean evictByteRepASAP=false;
//    
//            evictObjectRepASAP=false;
//            evictByteRepASAP=false;
//            testSerialisation(new WholeAttributes(dirtier, streamer, evictObjectRepASAP, evictByteRepASAP), evictObjectRepASAP, evictByteRepASAP, false);
//            evictObjectRepASAP=true;
//            evictByteRepASAP=false;
//            testSerialisation(new WholeAttributes(dirtier, streamer, evictObjectRepASAP, evictByteRepASAP), evictObjectRepASAP, evictByteRepASAP, false);
//            evictObjectRepASAP=false;
//            evictByteRepASAP=true;
//            testSerialisation(new WholeAttributes(dirtier, streamer, evictObjectRepASAP, evictByteRepASAP), evictObjectRepASAP, evictByteRepASAP, false);
//            evictObjectRepASAP=true;
//            evictByteRepASAP=true;
//            testSerialisation(new WholeAttributes(dirtier, streamer, evictObjectRepASAP, evictByteRepASAP), evictObjectRepASAP, evictByteRepASAP, false);
//            
//    //        dirtier=new ReadWriteDirtier();
//    //        evictObjectRepASAP=false;
//    //        evictByteRepASAP=false;
//    //        testSerialisation(new WholeAttributesWrapper(new SimpleAttributes(), dirtier, streamer, evictObjectRepASAP, evictByteRepASAP), evictObjectRepASAP, evictByteRepASAP, true);
//    //        evictObjectRepASAP=true;
//    //       evictByteRepASAP=false;
//    //        testSerialisation(new WholeAttributesWrapper(new SimpleAttributes(), dirtier, streamer, evictObjectRepASAP, evictByteRepASAP), evictObjectRepASAP, evictByteRepASAP, true);
//    //        evictObjectRepASAP=false;
//    //        evictByteRepASAP=true;
//    //        testSerialisation(new WholeAttributesWrapper(new SimpleAttributes(), dirtier, streamer, evictObjectRepASAP, evictByteRepASAP), evictObjectRepASAP, evictByteRepASAP, true);
//    //        evictObjectRepASAP=true;
//    //        evictByteRepASAP=true;
//    //        testSerialisation(new WholeAttributesWrapper(new SimpleAttributes(), dirtier, streamer, evictObjectRepASAP, evictByteRepASAP), evictObjectRepASAP, evictByteRepASAP, true);
//        }
    
//    public void testWholeAttributes() throws Exception {
//        testAttributes(new SimpleAttributes());
//        testAttributes(new SimpleAttributes());
//        testAttributes(new SimpleAttributes());
//        testAttributes(new SimpleAttributes());
//        testAttributes(new SimpleAttributes());
//        testAttributes(new SimpleAttributes());
//        testAttributes(new SimpleAttributes());
//        testAttributes(new SimpleAttributes());
//
//        testAttributes(new WholeAttributes(new WriteDirtier(), new SimpleStreamingStrategy(), false, false));
//        testAttributes(new WholeAttributes(new WriteDirtier(), new SimpleStreamingStrategy(), true, false));
//        testAttributes(new WholeAttributes(new WriteDirtier(), new SimpleStreamingStrategy(), false, true));
//        testAttributes(new WholeAttributes(new WriteDirtier(), new SimpleStreamingStrategy(), true, true));
//        testAttributes(new WholeAttributes(new ReadWriteDirtier(), new SimpleStreamingStrategy(), false, false));
//        testAttributes(new WholeAttributes(new ReadWriteDirtier(), new SimpleStreamingStrategy(), true, false));
//        testAttributes(new WholeAttributes(new ReadWriteDirtier(), new SimpleStreamingStrategy(), false, true));
//        testAttributes(new WholeAttributes(new ReadWriteDirtier(), new SimpleStreamingStrategy(), true, true));
//
//        testAttributes(new PartAttributes(new WriteDirtier(), new SimpleStreamingStrategy(), false, false));
//        testAttributes(new PartAttributes(new WriteDirtier(), new SimpleStreamingStrategy(), true, false));
//        testAttributes(new PartAttributes(new WriteDirtier(), new SimpleStreamingStrategy(), false, true));
//        testAttributes(new PartAttributes(new WriteDirtier(), new SimpleStreamingStrategy(), true, true));
//        testAttributes(new PartAttributes(new ReadWriteDirtier(), new SimpleStreamingStrategy(), false, false));
//        testAttributes(new PartAttributes(new ReadWriteDirtier(), new SimpleStreamingStrategy(), true, false));
//        testAttributes(new PartAttributes(new ReadWriteDirtier(), new SimpleStreamingStrategy(), false, true));
//        testAttributes(new PartAttributes(new ReadWriteDirtier(), new SimpleStreamingStrategy(), true, true));
//
//    }

//    public void testAttributes(Attributes a) throws Exception {
//        // can we serialise an empty instance OK ?
//        byte[] bytes=a.getBytes();
//        // and then put the result back in ?
//        a.setBytes(bytes);
//        
//        String key="foo";
//        Object val=key;
//        a.put(key, val);
//        assertTrue(a.get(key).equals(val));
//        bytes=a.getBytes();
//        a.remove(key);
//        assertTrue(a.get(key)==null);
//        a.setBytes(bytes);
//        assertTrue(a.get(key).equals(val));
//    }
//    
//    public void testSerialisation(Attributes wrapper, boolean evictObjectRepASAP, boolean evictByteRepASAP, boolean readIsDirty) throws Exception {
//        String key="foo";
//        Counter val=new Counter();
//        int serialisations=0;
//        _serialisations=serialisations;
//        int deserialisations=0;
//        _deserialisations=deserialisations;
//        // check initial state
//        assertTrue(_serialisations==serialisations);
//        assertTrue(_deserialisations==deserialisations);
//        // insert into container and reinspect - expect no change
//        wrapper.put(key, val);
//        assertTrue(_serialisations==serialisations);
//        assertTrue(_deserialisations==deserialisations);
//        // can we retrieve the same reference ?
//        assertTrue(val==wrapper.get(key));
//        // try serialising container - should serialise content..
//        byte[] bytes=wrapper.getBytes();
//        serialisations++;
//        assertTrue(_serialisations==serialisations);
//        assertTrue(_deserialisations==deserialisations);
//        // serialise container again - should used cached serialised content
//        bytes=wrapper.getBytes();
//        assertTrue(_serialisations==serialisations);
//        assertTrue(_deserialisations==deserialisations);
//        // can we still retrieve the original reference ?
//        if (evictObjectRepASAP) {
//            assertTrue(val.equals(wrapper.get(key)));
//            deserialisations++;
//        } else {
//            assertTrue(val==wrapper.get(key));
//        }
//        // did this last operation affect effect the content ?
//        assertTrue(_serialisations==serialisations);
//        assertTrue(_deserialisations==deserialisations);
//        // serialise the container again - should still not alter contents ...
//        bytes=wrapper.getBytes();
//        if (evictObjectRepASAP && evictByteRepASAP)
//            serialisations++;
//        assertTrue(_serialisations==serialisations);
//        assertTrue(_deserialisations==deserialisations);
//        // reinsert content, should invalidate serialised cache...
//        wrapper.put(key, val);
//        assertTrue(val==wrapper.get(key));
//        if (evictObjectRepASAP && evictByteRepASAP)
//            deserialisations++;
//        assertTrue(_serialisations==serialisations);
//        _log.info(""+_deserialisations+"=="+deserialisations);
//        assertTrue(_deserialisations==deserialisations);
//        bytes=wrapper.getBytes();
//        serialisations++;
//        assertTrue(_serialisations==serialisations);
//        assertTrue(_deserialisations==deserialisations);
//        
//        // Looks good - now let's try deserialising...
//        
//        // populate the container - should not change old content state...
//        wrapper.setBytes(bytes);
//        assertTrue(_serialisations==serialisations);
//        assertTrue(_deserialisations==deserialisations);
//        // retrieve content - should cause deserialisation
//        val=(Counter)wrapper.get(key);
//        deserialisations++;
//        assertTrue(_serialisations==serialisations);
//        assertTrue(_deserialisations==deserialisations);
//        // retrieve content again - should be found in cache - no deserialisation...
//        val=(Counter)wrapper.get(key);
//        assertTrue(_serialisations==serialisations);
//        assertTrue(_deserialisations==deserialisations);
//        // reinitialise content - should invalidate object cache...
//        bytes=wrapper.getBytes();
//        if (evictByteRepASAP) {
//            serialisations++;
//        }
//        assertTrue(_serialisations==serialisations);
//        assertTrue(_deserialisations==deserialisations);
//        wrapper.setBytes(bytes);
//        assertTrue(_serialisations==serialisations);
//        assertTrue(_deserialisations==deserialisations);
//        // reretrieve content - should cause fresh deserialisation
//        val=(Counter)wrapper.get(key);
//        deserialisations++;
//        assertTrue(_serialisations==serialisations);
//        assertTrue(_deserialisations==deserialisations);
//    }
}
