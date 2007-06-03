

import java.net.URI;

import org.codehaus.wadi.core.assembler.StackContext;
import org.codehaus.wadi.core.manager.DummyRouter;
import org.codehaus.wadi.core.manager.Manager;
import org.codehaus.wadi.core.manager.Router;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.servicespace.ServiceSpace;
import org.codehaus.wadi.servicespace.ServiceSpaceName;

public class SampleStack {
    private Manager manager;
    private ServiceSpace serviceSpace;
    
    public void create(Dispatcher dispatcher) throws Exception {
        StackContext stackContext = new StackContext(new ServiceSpaceName(new URI("name")), dispatcher) {
            protected Router newRouter() {
                return new DummyRouter();
            }
        };
        stackContext.build();

        serviceSpace = stackContext.getServiceSpace();
        manager = stackContext.getManager();
    }

    public Manager getManager() {
        return manager;
    }

    public void start() throws Exception {
        serviceSpace.start();
    }

    public void stop() throws Exception {
        serviceSpace.stop();
    }

}
