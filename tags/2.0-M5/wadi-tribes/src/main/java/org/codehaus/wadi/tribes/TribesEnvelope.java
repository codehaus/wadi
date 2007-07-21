package org.codehaus.wadi.tribes;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.Envelope;

/**
 * <p>Title: </p>
 *
 * <p>Description: </p>
 *
 * <p>Copyright: Copyright (c) 2006</p>
 *
 * <p>Company: </p>
 *
 * @author not attributable
 * @version 1.0
 */
public class TribesEnvelope implements Envelope, Serializable {
    protected Address address;
    protected Serializable payload;
    protected Address replyto;
    protected String sourceCorrId;
    protected String targetCorrId;
    private final Map properties = new HashMap();
    
    public TribesEnvelope() {
    }

    /**
     * getAddress
     *
     * @return Address
     * @todo Implement this org.codehaus.wadi.group.Message method
     */
    public Address getAddress() {
        return address;
    }

    /**
     * getPayload
     *
     * @return Serializable
     * @todo Implement this org.codehaus.wadi.group.Message method
     */
    public Serializable getPayload() {
        return payload;
    }

    /**
     * getReplyTo
     *
     * @return Address
     * @todo Implement this org.codehaus.wadi.group.Message method
     */
    public Address getReplyTo() {
        return replyto;
    }

    /**
     * getSourceCorrelationId
     *
     * @return String
     * @todo Implement this org.codehaus.wadi.group.Message method
     */
    public String getSourceCorrelationId() {
        return sourceCorrId;
    }

    /**
     * getTargetCorrelationId
     *
     * @return String
     * @todo Implement this org.codehaus.wadi.group.Message method
     */
    public String getTargetCorrelationId() {
        return targetCorrId;
    }

    /**
     * setAddress
     *
     * @param address Address
     * @todo Implement this org.codehaus.wadi.group.Message method
     */
    public void setAddress(Address address) {
        this.address = address;
    }

    /**
     * setPayload
     *
     * @param payload Serializable
     * @todo Implement this org.codehaus.wadi.group.Message method
     */
    public void setPayload(Serializable payload) {
        this.payload = payload;
    }

    /**
     * setReplyTo
     *
     * @param replyTo Address
     * @todo Implement this org.codehaus.wadi.group.Message method
     */
    public void setReplyTo(Address replyTo) {
        this.replyto = replyTo;
    }

    /**
     * setSourceCorrelationId
     *
     * @param correlationId String
     * @todo Implement this org.codehaus.wadi.group.Message method
     */
    public void setSourceCorrelationId(String correlationId) {
        this.sourceCorrId = correlationId;
    }

    /**
     * setTargetCorrelationId
     *
     * @param correlationId String
     * @todo Implement this org.codehaus.wadi.group.Message method
     */
    public void setTargetCorrelationId(String correlationId) {
        this.targetCorrId = correlationId;
    }

    public Map getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    public Object getProperty(String key) {
        return properties.get(key);
    }

    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }
    
    @Override
    public String toString() {
        return "Message: to [" + address + "]; replyTo [" + replyto + "]; payload [" + payload + "]";
    }
    
}