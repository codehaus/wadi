package org.codehaus.wadi.console.groovy;

import org.codehaus.wadi.core.assembler.StackContext
import org.codehaus.wadi.servicespace.ServiceSpaceName

class ServiceSpaceBuilder {

    def ServiceSpaceBuilder(serviceSpaceName, sessionPrefix, dispatcher, dispatcherRegistry) {
        dispatcherRegistry.register(dispatcher)
        
        StackContext stackContext = new StackContext(new ServiceSpaceName(new URI(serviceSpaceName)), dispatcher)
        stackContext.build()
        def serviceSpace = stackContext.getServiceSpace()
        serviceSpace.start()
        
        def manager = stackContext.getManager()
        
        def operateOnSession = {
            def sessions = []
            def index = 0
            while (true) {
		        def session = manager.createWithName(sessionPrefix + index++)
		        sessions.add(session)
		        Thread.sleep(500)
		        if (sessions.size() > 10) {
		            session = sessions.remove(0)
		            manager.destroy(session)
		        }
            }
        }
        
        new Thread(operateOnSession as Runnable).start()
    }
    
}