package org.codehaus.wadi;

public interface NewDirtier {

    boolean setMaxInactiveInterval(Attributes attributes, int maxInactiveInterval);
    
    boolean setAttribute(Attributes attributes, String name, Object value);
    
    boolean removeAttribute(Attributes attributes, String name);
    
    boolean getAttribute(Attributes attributes, String name);
    
}
