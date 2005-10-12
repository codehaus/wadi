package org.codehaus.wadi;

public interface Replicater {

	void create(Object tmp);
	void replicate(Object tmp);
	void destroy(Object tmp);
	
}