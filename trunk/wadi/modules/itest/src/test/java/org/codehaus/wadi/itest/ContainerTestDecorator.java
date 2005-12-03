/**
 *
 * Copyright 2005 The Apache Software Foundation
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
package org.codehaus.wadi.itest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import junit.extensions.TestSetup;
import junit.framework.Test;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.cargo.container.LocalContainer;
import org.codehaus.cargo.container.configuration.ConfigurationType;
import org.codehaus.cargo.container.configuration.LocalConfiguration;
import org.codehaus.cargo.container.deployable.WAR;
import org.codehaus.cargo.container.installer.Installer;
import org.codehaus.cargo.container.installer.ZipURLInstaller;
import org.codehaus.cargo.container.property.ServletPropertySet;
import org.codehaus.cargo.container.tomcat.TomcatPropertySet;
import org.codehaus.cargo.container.tomcat.TomcatWAR;
import org.codehaus.cargo.generic.ContainerFactory;
import org.codehaus.cargo.generic.DefaultContainerFactory;
import org.codehaus.cargo.generic.configuration.DefaultConfigurationFactory;

public class ContainerTestDecorator extends TestSetup {
    protected Log _log = LogFactory.getLog(getClass());
    // the config dir base is the path that the various containers will be
    // copied to
    private static final String CONTAINER_CONFIG_DIR_BASE = "target/test-containers/";
    private static final String WADI_VERSION = System
            .getProperty("wadi.version");
    // the systemProps are initialized in a static block at the end of this class
    private static Map systemProps = null;
    // the systemProps are initialized in a static block at the end of this class
    private static Map installProps = null;
    private LocalContainer containers[] = null;

    public ContainerTestDecorator(Test decorated) {
        super(decorated);
    }

    public String getPortForNode(String node) {
        return (String)((Map)systemProps.get(node)).get("http.port");
    }
    
    protected void setUp() throws Exception {
        // parse containers to start
        String containersProp = System.getProperty("containers");
        String containerProp = System.getProperty("container");
        String nodesProp = System.getProperty("nodes");
        StringTokenizer nodeTokenizer = new StringTokenizer(nodesProp, ",");
        List nodes = new ArrayList();
        while(nodeTokenizer.hasMoreTokens()) {
            nodes.add(nodeTokenizer.nextToken());
        }
        if(null != containerProp) {
            // install the container
            String containerUrl = (String)((Map)installProps.get(containerProp)).get("url");
            String containerInstallDir = (String)((Map)installProps.get(containerProp)).get("installDir");
            String containerConfigBase = (String)((Map)installProps.get(containerProp)).get("configBase");
            String managerClassName = (String)((Map)installProps.get(containerProp)).get("managerClassName");
            String cargoContainerName = (String)((Map)installProps.get(containerProp)).get("cargoContainerName");
            Installer installer = installContainer(containerUrl, containerInstallDir);
            copyJars(installer.getHome(), containerProp);
            containers = new LocalContainer[nodes.size()];
            // starting one container type
            for (int i = 0; i < nodes.size(); i++) {
                containers[i] = startContainer((String) nodes.get(i),
                        containerConfigBase, managerClassName,
                        cargoContainerName, installer);
            }
        } else {
            StringTokenizer containerTokenizer = new StringTokenizer(containersProp, ",");
            List containerNames = new ArrayList();
            while(containerTokenizer.hasMoreTokens()) {
                containerNames.add(containerTokenizer.nextToken());
            }
            if(containerNames.size() != nodes.size()) {
                throw new RuntimeException(
                        "Bad Configuration - containers list must match nodes list -"
                                + " nodes = " + nodesProp + " containers = " + containersProp);
            }
            containers = new LocalContainer[containerNames.size()];
            // starting several container types
            for(int i = 0;i < containerNames.size();i++) {
                String containerName = (String)containerNames.get(i);
                String containerUrl = (String)((Map)installProps.get(containerName)).get("url");
                String containerInstallDir = (String)((Map)installProps.get(containerName)).get("installDir");
                String containerConfigBase = (String)((Map)installProps.get(containerName)).get("configBase");
                String managerClassName = (String)((Map)installProps.get(containerName)).get("managerClassName");
                String cargoContainerName = (String)((Map)installProps.get(containerName)).get("cargoContainerName");
                Installer installer = installContainer(containerUrl, containerInstallDir);
                copyJars(installer.getHome(), containerName);
                // starting one node on each container
                containers[i] = startContainer((String) nodes.get(i),
                        containerConfigBase, managerClassName, cargoContainerName,
                        installer);
            }
        }
    }

    private Installer installContainer(String url, String installDir)
            throws IOException {
        Installer zipInstaller = new ZipURLInstaller(new URL(url), new File(
                installDir));
        zipInstaller.install();
        return zipInstaller;
    }

    protected void tearDown() throws Exception {
        for(int i = 0;i < containers.length;i++) {
            containers[i].stop();
        }
    }

    private LocalContainer startContainer(String nodeName, String dirName,
            String managerClassName, String cargoContainerName,
            Installer installer) throws Exception {
        if (null == WADI_VERSION) {
            throw new RuntimeException(
                    "Bad Configuration - wadi.version must be specified");
        }
        String m2Repo = System.getProperty("user.home") + "/.m2/repository";
        String wadiWARPath = m2Repo + "/wadi/wadi-test-webapp/" + WADI_VERSION
                + "/wadi-test-webapp-" + WADI_VERSION + ".war";
        WAR wadi = new TomcatWAR(wadiWARPath);
        wadi.setContext("wadi-test");
        File configDir = new File(dirName + "_" + nodeName);
        File log = new File(configDir.getPath() + "/cargo.log");
        DefaultConfigurationFactory factory = new DefaultConfigurationFactory();
        LocalConfiguration config = (LocalConfiguration) factory
                .createConfiguration(cargoContainerName,
                        ConfigurationType.STANDALONE, configDir);
        config
                .setProperty(TomcatPropertySet.WEBAPP_TOKEN_VALUE,
                        getWebappToken(wadi,
                                managerClassName));
        config.addDeployable(wadi);
        Map props = (Map) systemProps.get(nodeName);
        config.setProperty(ServletPropertySet.PORT, (String) props
                .get("http.port"));
        config.setProperty(TomcatPropertySet.SHUTDOWN_PORT, (String) props
                .get("STOP.PORT"));
        ContainerFactory containerFactory = new DefaultContainerFactory();
        LocalContainer container = (LocalContainer)containerFactory.createContainer(cargoContainerName, config);
        container.setSystemProperties(props);
        container.setHome(installer.getHome());
        container.setOutput(log);
        container.start();
        return container;
    }

    private String getWebappToken(WAR deployable, String managerClassName) {
        StringBuffer contextTokenValue = new StringBuffer();
        contextTokenValue.append("\t<Context path=\"");
        contextTokenValue.append("/" + deployable.getContext());
        contextTokenValue.append("\" docBase=\"");
        // Tomcat requires an absolute path for the "docBase" attribute.
        contextTokenValue.append(deployable.getFile().getAbsolutePath());
        contextTokenValue.append("\">\n");
        contextTokenValue.append("\t\t<Manager className=\"" + managerClassName
                + "\"/>\n");
        contextTokenValue.append("\t</Context>\n");
        return contextTokenValue.toString();
    }

    private void copyJars(File home, String containerName) throws IOException {
        String rootPath = System.getProperty("user.home") + "/.m2/repository";
        String commonLibJars[] = new String[] {
                rootPath
                        + "/activecluster/activecluster/WADI-1.1-SNAPSHOT/activecluster-WADI-1.1-SNAPSHOT.jar",
                rootPath
                        + "/activemq/activemq/WADI-3.2/activemq-WADI-3.2.jar",
                rootPath + "/axion/axion/1.0-M3-dev/axion-1.0-M3-dev.jar",
                rootPath
                        + "/commons-collections/commons-collections/3.1/commons-collections-3.1.jar",
                rootPath
                        + "/commons-primitives/commons-primitives/1.0/commons-primitives-1.0.jar",
                rootPath
                        + "/concurrent/concurrent/1.3.4/concurrent-1.3.4.jar",
                rootPath
                        + "/geronimo-spec/geronimo-spec-j2ee-management/1.0-rc4/geronimo-spec-j2ee-management-1.0-rc4.jar",
                rootPath
                        + "/geronimo-spec/geronimo-spec-jms/1.1-rc4/geronimo-spec-jms-1.1-rc4.jar",
                rootPath + "/regexp/regexp/1.3/regexp-1.3.jar",
                rootPath + "/springframework/spring/1.2.5/spring-1.2.5.jar",
                rootPath + "/wadi/wadi-core/" + WADI_VERSION + "/wadi-core-"
                        + WADI_VERSION + ".jar"};
        for (int i = 0; i < commonLibJars.length; i++) {
            File jarFile = new File(commonLibJars[i]);
            if (!jarFile.exists()) {
                throw new RuntimeException("file " + jarFile.getPath() + " does not exist");
            } else {
                // copy the jar to home/common/lib
                File dest = new File(home.getAbsolutePath() + "/common/lib/" + jarFile.getName());
                if(!dest.exists()) {
                    copy(jarFile, dest);
                }
            }
        }
        String serverLibJar = rootPath + "/wadi/wadi-" + containerName + "/" + WADI_VERSION
                + "/wadi-" + containerName + "-" + WADI_VERSION + ".jar";
        File serverLibJarFile = new File(serverLibJar);
        if (!serverLibJarFile.exists()) {
            throw new RuntimeException("file " + serverLibJarFile.getPath() + " does not exist");
        } else {
            // copy the jar to home/server/lib
            File dest = new File(home.getAbsolutePath() + "/server/lib/" + serverLibJarFile.getName());
            if(!dest.exists()) {
                copy(serverLibJarFile, dest);
            }
        }
    }

    private void copy(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);
        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    static {
        systemProps = new HashMap();
        Map map = new HashMap();
        systemProps.put("red", map);
        map.put("http.port", "8080");
        map.put("ajp.port", "8009");
        map.put("jndi.port", "1099");
        map.put("STOP.PORT", "8040");
        map.put("node.name", "red");
        map = new HashMap();
        systemProps.put("green", map);
        map.put("http.port", "8081");
        map.put("ajp.port", "8010");
        map.put("jndi.port", "1100");
        map.put("STOP.PORT", "8041");
        map.put("node.name", "green");
        map = new HashMap();
        systemProps.put("blue", map);
        map.put("http.port", "8082");
        map.put("ajp.port", "8011");
        map.put("jndi.port", "1101");
        map.put("STOP.PORT", "8042");
        map.put("node.name", "blue");
        map = new HashMap();
        systemProps.put("yellow", map);
        map.put("http.port", "8083");
        map.put("ajp.port", "8012");
        map.put("jndi.port", "1102");
        map.put("STOP.PORT", "8043");
        map.put("node.name", "yellow");
        map = new HashMap();
        systemProps.put("pink", map);
        map.put("http.port", "8084");
        map.put("ajp.port", "8013");
        map.put("jndi.port", "1103");
        map.put("STOP.PORT", "8044");
        map.put("node.name", "pink");
        map = new HashMap();
        systemProps.put("orange", map);
        map.put("http.port", "8085");
        map.put("ajp.port", "8014");
        map.put("jndi.port", "1104");
        map.put("STOP.PORT", "8045");
        map.put("node.name", "orange");
        map = new HashMap();
        systemProps.put("purple", map);
        map.put("http.port", "8086");
        map.put("ajp.port", "8015");
        map.put("jndi.port", "1105");
        map.put("STOP.PORT", "8046");
        map.put("node.name", "purple");
        map = new HashMap();
        systemProps.put("brown", map);
        map.put("http.port", "8087");
        map.put("ajp.port", "8016");
        map.put("jndi.port", "1106");
        map.put("STOP.PORT", "8047");
        map.put("node.name", "brown");
        map = new HashMap();
        systemProps.put("white", map);
        map.put("http.port", "8088");
        map.put("ajp.port", "8017");
        map.put("jndi.port", "1107");
        map.put("STOP.PORT", "8048");
        map.put("node.name", "white");
        // install props
        installProps = new HashMap();
        map = new HashMap();
        installProps.put("tomcat50", map);
        map.put("configBase", CONTAINER_CONFIG_DIR_BASE + "tomcat50");
        map.put("installDir", "target/installs");
        map.put("url", "http://www.apache.org/dist/jakarta/tomcat-5/"
                + "v5.0.30/bin/jakarta-tomcat-5.0.30.zip");
        map.put("managerClassName", "org.codehaus.wadi.tomcat50.TomcatManager");
        map.put("cargoContainerName", "tomcat5x");
        map = new HashMap();
        installProps.put("tomcat55", map);
        map.put("configBase", CONTAINER_CONFIG_DIR_BASE + "tomcat55");
        map.put("installDir", "target/installs");
        map.put("url", "http://www.apache.org/dist/tomcat/tomcat-5/"
                + "v5.5.12/bin/apache-tomcat-5.5.12.zip");
        map.put("managerClassName", "org.codehaus.wadi.tomcat55.TomcatManager");
        map.put("cargoContainerName", "tomcat5x");
    }

}
