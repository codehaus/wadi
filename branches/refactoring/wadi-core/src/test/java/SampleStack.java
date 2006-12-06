

import java.net.URI;

import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.impl.ClusteredManager;
import org.codehaus.wadi.impl.StackContext;
import org.codehaus.wadi.servicespace.ServiceSpace;
import org.codehaus.wadi.servicespace.ServiceSpaceName;

public class SampleStack {
    private ClusteredManager manager;
    private ServiceSpace serviceSpace;
    
    public void create(Dispatcher dispatcher) throws Exception {
        StackContext stackContext = new StackContext(new ServiceSpaceName(new URI("name")), dispatcher);
        stackContext.build();

        serviceSpace = stackContext.getServiceSpace();
        manager = stackContext.getManager();
    }

    public ClusteredManager getManager() {
        return manager;
    }

    public void start() throws Exception {
        serviceSpace.start();
    }

    public void stop() throws Exception {
        serviceSpace.stop();
    }

}
