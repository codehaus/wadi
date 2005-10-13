package org.codehaus.wadi;

public interface NewReplicater {

	void create(Object tmp);
	void replicate(Object tmp);
	void destroy(Object tmp);
	
}