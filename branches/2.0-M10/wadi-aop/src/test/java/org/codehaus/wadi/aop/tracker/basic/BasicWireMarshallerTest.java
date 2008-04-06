package org.codehaus.wadi.aop.tracker.basic;

import org.codehaus.wadi.core.reflect.ClassIndexerRegistry;
import org.codehaus.wadi.core.util.SimpleStreamer;
import org.codehaus.wadi.core.util.Streamer;

import com.agical.rmock.extension.junit.RMockTestCase;

public class BasicWireMarshallerTest extends RMockTestCase {

    public void testMarshallAndUnmarshall() throws Exception {
        Streamer streamer = new SimpleStreamer();
        ClassIndexerRegistry indexerRegistry = (ClassIndexerRegistry) mock(ClassIndexerRegistry.class);
        InstanceAndTrackerReplacer replacer = (InstanceAndTrackerReplacer) mock(InstanceAndTrackerReplacer.class);
        
        startVerification();
        
        BasicWireMarshaller marshaller = new BasicWireMarshaller(streamer, indexerRegistry, replacer);
        
        ValueUpdaterInfo valueUpdaterInfo = new ValueUpdaterInfo(replacer, 1, new Object[0]);
        valueUpdaterInfo.setInstanceId("instanceId");
        ValueUpdaterInfo[] valueUpdaterInfos = new ValueUpdaterInfo[] {valueUpdaterInfo};
        
        byte[] serialized = marshaller.marshall(valueUpdaterInfos);
        ValueUpdaterInfo[] reInstantiatedValueUpdaterInfos = marshaller.unmarshall(serialized);
        assertEquals(1, reInstantiatedValueUpdaterInfos.length);
    }
    
}
