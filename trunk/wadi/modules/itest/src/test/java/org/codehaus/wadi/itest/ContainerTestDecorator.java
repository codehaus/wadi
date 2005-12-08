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
import java.util.HashMap;
import java.util.Map;
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
  // node names
  private static final String WHITE = "white";
  private static final String BROWN = "brown";
  private static final String PURPLE = "purple";
  private static final String ORANGE = "orange";
  private static final String PINK = "pink";
  private static final String YELLOW = "yellow";
  private static final String BLUE = "blue";
  private static final String GREEN = "green";
  private static final String RED = "red";
  // property names to configure nodes
  private static final String NODE_NAME_PROP_NAME = "node.name";
  private static final String STOP_PORT_PROP_NAME = "STOP.PORT";
  private static final String JNDI_PORT_PROP_NAME = "jndi.port";
  private static final String AJP_PORT_PROP_NAME = "ajp.port";
  private static final String HTTP_PORT_PROP_NAME = "http.port";
  // cargo property names
  private static final String TOMCAT55_KEY_NAME = "tomcat55";
  private static final String TOMCAT50_KEY_NAME = "tomcat50";
  private static final String CARGO_CONTAINER_NAME_PROP_NAME = "cargoContainerName";
  private static final String MANAGER_CLASS_NAME_PROP_NAME = "managerClassName";
  private static final String CONTAINER_URL_PROP_NAME = "url";
  private static final String INSTALL_DIR_PROP_NAME = "installDir";
  private static final String CONFIG_BASE_PROP_NAME = "configBase";
  // the config dir base is the path that the various containers will be
  // copied to
  private static final String CONTAINER_CONFIG_DIR_BASE = "target/test-containers/";
  private static final String WADI_VERSION = System.getProperty("wadi.version");
  // the systemProps are initialized in a static block at the end of this
  // class
  private static Map systemProps = null;
  // the systemProps are initialized in a static block at the end of this
  // class
  //private static Map installProps = null;
  private LocalContainer containers[] = null;

  public ContainerTestDecorator(Test decorated) {
    super(decorated);
  }

  public String getPortForNode(String node) {
    return (String) ((Map) systemProps.get(node)).get(HTTP_PORT_PROP_NAME);
  }

  protected void setUp() throws Exception {
    if(_log.isDebugEnabled()) _log.debug(getClass().getName() + ".setUp()");
    // parse containers to start
    String containersProp = System.getProperty("containers");
    String containerProp = System.getProperty("container");
    String nodesProp = System.getProperty("nodes");
    String nodes[] = nodesProp.split(",");
    if (null != containerProp) {
      Installer installer = installContainer(containerProp);
      String containerConfigBase = System.getProperty("container.config.dir") + "/" + containerProp;
      //String containerConfigBase = (String) ((Map) installProps
      //    .get(containerProp)).get(CONFIG_BASE_PROP_NAME);
      String managerClassName = System.getProperty(containerProp + ".manager.class.name");
      //String managerClassName = (String) ((Map) installProps.get(containerProp))
      //    .get(MANAGER_CLASS_NAME_PROP_NAME);
      String cargoContainerName = System.getProperty(containerProp + ".cargo.container.name");
      //String cargoContainerName = (String) ((Map) installProps
      //    .get(containerProp)).get(CARGO_CONTAINER_NAME_PROP_NAME);
      containers = new LocalContainer[nodes.length];
      // starting one container type
      for (int i = 0; i < nodes.length; i++) {
        containers[i] = startContainer((String) nodes[i], containerConfigBase,
            managerClassName, cargoContainerName, installer);
      }
    } else {
      String containerNames[] = containersProp.split(",");
      if (containerNames.length != nodes.length) {
        throw new RuntimeException(
            "Bad Configuration - containers list must match nodes list -"
                + " nodes = " + nodesProp + " containers = " + containersProp);
      }
      containers = new LocalContainer[containerNames.length];
      // starting several container types
      for (int i = 0; i < containerNames.length; i++) {
        Installer installer = installContainer(containerNames[i]);
        String containerConfigBase = System.getProperty("container.config.dir") + "/" + containerNames[i];
        //String containerConfigBase = (String) ((Map) installProps
        //    .get(containerProp)).get(CONFIG_BASE_PROP_NAME);
        String managerClassName = System.getProperty(containerNames[i] + ".manager.class.name");
        //String managerClassName = (String) ((Map) installProps.get(containerProp))
        //    .get(MANAGER_CLASS_NAME_PROP_NAME);
        String cargoContainerName = System.getProperty(containerNames[i] + ".cargo.container.name");
        //String cargoContainerName = (String) ((Map) installProps
        //    .get(containerProp)).get(CARGO_CONTAINER_NAME_PROP_NAME);
        // starting one node on each container
        containers[i] = startContainer((String) nodes[i], containerConfigBase,
            managerClassName, cargoContainerName, installer);
      }
    }
  }

  // TODO: this assumes tomcat (with the copyJars method, need to rework for
  // jetty
  // and jboss
  private Installer installContainer(String containerName) throws IOException {
    if(_log.isDebugEnabled()) _log.debug(getClass().getName() + ".installContainer(" + containerName + ")");
    String containerUrl = System.getProperty(containerName + ".url");
    //String containerUrl = (String) ((Map) installProps.get(containerName))
    //    .get(CONTAINER_URL_PROP_NAME);
    String containerInstallDir = System.getProperty("container.install.dir");
    //String containerInstallDir = (String) ((Map) installProps
    //    .get(containerName)).get(INSTALL_DIR_PROP_NAME);
    Installer installer = installContainer(containerUrl, containerInstallDir);
    copyJars(installer.getHome(), containerName);
    if(_log.isDebugEnabled()) _log.debug("copy jars successful");
    if (containerName.equals(TOMCAT55_KEY_NAME)) {
      String compatibilityURL = System.getProperty("tomcat55.compatibility.url");
      //String compatibilityURL = (String) ((Map) installProps.get(containerName))
      //   .get("tomcat55JDK14CompaitilityURL");
      installTomcat55JDK14CompatibilityStuff(compatibilityURL,
          containerInstallDir, installer.getHome().getPath());
      if(_log.isDebugEnabled()) _log.debug("install compat success");
    }
    return installer;
  }

  private Installer installContainer(String url, String installDir)
      throws IOException {
    if(_log.isDebugEnabled()) _log.debug(getClass().getName() + ".installContainer(" + url + ", "
        + installDir + ")");
    Installer zipInstaller = new ZipURLInstaller(new URL(url), new File(
        installDir));
    zipInstaller.install();
    return zipInstaller;
  }

  private void installTomcat55JDK14CompatibilityStuff(String url,
      String installDir, String tc55InstallDir) throws IOException {
    Installer zipInstaller = new ZipURLInstaller(new URL(url), new File(
        installDir));
    zipInstaller.install();
    if(_log.isDebugEnabled()) _log.debug("installed compat");
    // copy the jars
    File jmx = new File(zipInstaller.getHome().getPath() + "/bin/jmx.jar");
    File dest = new File(tc55InstallDir + "/bin/jmx.jar");
    if(_log.isDebugEnabled()) _log.debug("about to copy " + jmx.getPath() + " to " + dest.getPath());
    copy(jmx, dest);
    File xerces = new File(zipInstaller.getHome().getPath()
        + "/common/endorsed/xercesImpl.jar");
    dest = new File(tc55InstallDir + "/common/endorsed/xercesImp.jar");
    if(_log.isDebugEnabled()) _log.debug("about to copy " + xerces.getPath() + " to " + dest.getPath());
    copy(xerces, dest);
    File xml = new File(zipInstaller.getHome().getPath()
        + "/common/endorsed/xml-apis.jar");
    dest = new File(tc55InstallDir + "/common/endorsed/xml-apis.jar");
    if(_log.isDebugEnabled()) _log.debug("about to copy " + xml.getPath() + " to " + dest.getPath());
    copy(xml, dest);
  }

  protected void tearDown() throws Exception {
    for (int i = 0; i < containers.length; i++) {
      containers[i].stop();
    }
  }

  private LocalContainer startContainer(String nodeName, String dirName,
      String managerClassName, String cargoContainerName, Installer installer)
      throws Exception {
    if(_log.isDebugEnabled()) _log.debug("startContainer(" + nodeName + ", " + dirName + ", "
        + managerClassName + ", " + cargoContainerName + ", "
        + installer.getHome());
    if (null == WADI_VERSION) {
      if(_log.isDebugEnabled()) _log.debug("no wadi version");
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
        .createConfiguration(cargoContainerName, ConfigurationType.STANDALONE,
            configDir);
    config.setProperty(TomcatPropertySet.WEBAPP_TOKEN_VALUE, getWebappToken(
        wadi, managerClassName));
    config.addDeployable(wadi);
    Map props = (Map) systemProps.get(nodeName);
    config.setProperty(ServletPropertySet.PORT, (String) props
        .get(HTTP_PORT_PROP_NAME));
    config.setProperty(TomcatPropertySet.SHUTDOWN_PORT, (String) props
        .get(STOP_PORT_PROP_NAME));
    ContainerFactory containerFactory = new DefaultContainerFactory();
    LocalContainer container = (LocalContainer) containerFactory
        .createContainer(cargoContainerName, config);
    container.setSystemProperties(props);
    container.setHome(installer.getHome());
    container.setOutput(log);
    container.setTimeout(60000);
    if(_log.isDebugEnabled()) _log.debug("about to start " + container.getName());
    try {
      container.start();
      if(_log.isDebugEnabled()) _log.debug("started " + container.getName());
    } catch (Exception e) {
      if(_log.isDebugEnabled()) _log.debug("failed to start " + container.getName());
      if(_log.isDebugEnabled()) _log.debug("throwable is ", e);
    }
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
    if (null == WADI_VERSION) {
      if(_log.isDebugEnabled()) _log.debug("about to throw beacuse of no wadi.version");
      throw new RuntimeException(
          "Bad Configuration - wadi.version must be specified");
    }
    String rootPath = System.getProperty("user.home") + "/.m2/repository";
    String commonLibJars[] = new String[] {
        rootPath
            + "/activecluster/activecluster/WADI-1.1-SNAPSHOT/activecluster-WADI-1.1-SNAPSHOT.jar",
        rootPath + "/activemq/activemq/WADI-3.2/activemq-WADI-3.2.jar",
        rootPath + "/axion/axion/1.0-M3-dev/axion-1.0-M3-dev.jar",
        rootPath
            + "/commons-collections/commons-collections/3.1/commons-collections-3.1.jar",
        rootPath
            + "/commons-primitives/commons-primitives/1.0/commons-primitives-1.0.jar",
        rootPath + "/concurrent/concurrent/1.3.4/concurrent-1.3.4.jar",
        rootPath
            + "/geronimo-spec/geronimo-spec-j2ee-management/1.0-rc4/geronimo-spec-j2ee-management-1.0-rc4.jar",
        rootPath
            + "/geronimo-spec/geronimo-spec-jms/1.1-rc4/geronimo-spec-jms-1.1-rc4.jar",
        rootPath + "/regexp/regexp/1.3/regexp-1.3.jar",
        rootPath + "/org/springframework/spring/1.2.5/spring-1.2.5.jar",
        rootPath + "/wadi/wadi-core/" + WADI_VERSION + "/wadi-core-"
            + WADI_VERSION + ".jar" };
    for (int i = 0; i < commonLibJars.length; i++) {
      File jarFile = new File(commonLibJars[i]);
      if (!jarFile.exists()) {
        if(_log.isDebugEnabled()) _log.debug("about to throw beause " + jarFile.getPath()
            + " does not exist");
        throw new RuntimeException("file " + jarFile.getPath()
            + " does not exist");
      } else {
        // copy the jar to home/common/lib
        File dest = new File(home.getAbsolutePath() + "/common/lib/"
            + jarFile.getName());
        if (!dest.exists()) {
          copy(jarFile, dest);
        }
      }
    }
    String serverLibJar = rootPath + "/wadi/wadi-" + containerName + "/"
        + WADI_VERSION + "/wadi-" + containerName + "-" + WADI_VERSION + ".jar";
    File serverLibJarFile = new File(serverLibJar);
    if (!serverLibJarFile.exists()) {
      if(_log.isDebugEnabled()) _log.debug("about to throw because " + serverLibJarFile.getPath()
          + " does not exist");
      throw new RuntimeException("file " + serverLibJarFile.getPath()
          + " does not exist");
    } else {
      // copy the jar to home/server/lib
      File dest = new File(home.getAbsolutePath() + "/server/lib/"
          + serverLibJarFile.getName());
      if (!dest.exists()) {
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
    systemProps.put(RED, map);
    map.put(HTTP_PORT_PROP_NAME, "18080");
    map.put(AJP_PORT_PROP_NAME, "18009");
    map.put(JNDI_PORT_PROP_NAME, "11099");
    map.put(STOP_PORT_PROP_NAME, "18040");
    map.put(NODE_NAME_PROP_NAME, RED);
    map = new HashMap();
    systemProps.put(GREEN, map);
    map.put(HTTP_PORT_PROP_NAME, "18081");
    map.put(AJP_PORT_PROP_NAME, "18010");
    map.put(JNDI_PORT_PROP_NAME, "11100");
    map.put(STOP_PORT_PROP_NAME, "18041");
    map.put(NODE_NAME_PROP_NAME, GREEN);
    map = new HashMap();
    systemProps.put(BLUE, map);
    map.put(HTTP_PORT_PROP_NAME, "18082");
    map.put(AJP_PORT_PROP_NAME, "18011");
    map.put(JNDI_PORT_PROP_NAME, "11101");
    map.put(STOP_PORT_PROP_NAME, "18042");
    map.put(NODE_NAME_PROP_NAME, BLUE);
    map = new HashMap();
    systemProps.put(YELLOW, map);
    map.put(HTTP_PORT_PROP_NAME, "18083");
    map.put(AJP_PORT_PROP_NAME, "18012");
    map.put(JNDI_PORT_PROP_NAME, "11102");
    map.put(STOP_PORT_PROP_NAME, "18043");
    map.put(NODE_NAME_PROP_NAME, YELLOW);
    map = new HashMap();
    systemProps.put(PINK, map);
    map.put(HTTP_PORT_PROP_NAME, "18084");
    map.put(AJP_PORT_PROP_NAME, "18013");
    map.put(JNDI_PORT_PROP_NAME, "11103");
    map.put(STOP_PORT_PROP_NAME, "18044");
    map.put(NODE_NAME_PROP_NAME, PINK);
    map = new HashMap();
    systemProps.put(ORANGE, map);
    map.put(HTTP_PORT_PROP_NAME, "18085");
    map.put(AJP_PORT_PROP_NAME, "18014");
    map.put(JNDI_PORT_PROP_NAME, "11104");
    map.put(STOP_PORT_PROP_NAME, "18045");
    map.put(NODE_NAME_PROP_NAME, ORANGE);
    map = new HashMap();
    systemProps.put(PURPLE, map);
    map.put(HTTP_PORT_PROP_NAME, "18086");
    map.put(AJP_PORT_PROP_NAME, "18015");
    map.put(JNDI_PORT_PROP_NAME, "11105");
    map.put(STOP_PORT_PROP_NAME, "18046");
    map.put(NODE_NAME_PROP_NAME, PURPLE);
    map = new HashMap();
    systemProps.put(BROWN, map);
    map.put(HTTP_PORT_PROP_NAME, "18087");
    map.put(AJP_PORT_PROP_NAME, "18016");
    map.put(JNDI_PORT_PROP_NAME, "11106");
    map.put(STOP_PORT_PROP_NAME, "18047");
    map.put(NODE_NAME_PROP_NAME, BROWN);
    map = new HashMap();
    systemProps.put(WHITE, map);
    map.put(HTTP_PORT_PROP_NAME, "18088");
    map.put(AJP_PORT_PROP_NAME, "18017");
    map.put(JNDI_PORT_PROP_NAME, "11107");
    map.put(STOP_PORT_PROP_NAME, "18048");
    map.put(NODE_NAME_PROP_NAME, WHITE);
    // install props
    /*
    installProps = new HashMap();
    map = new HashMap();
    installProps.put(TOMCAT50_KEY_NAME, map);
    map.put(CONFIG_BASE_PROP_NAME, CONTAINER_CONFIG_DIR_BASE + "tomcat50");
    map.put(INSTALL_DIR_PROP_NAME, "target/installs");
    map.put(CONTAINER_URL_PROP_NAME,
        "http://www.apache.org/dist/jakarta/tomcat-5/v5.0.30/bin/jakarta-tomcat-5.0.30.zip");
    map.put(MANAGER_CLASS_NAME_PROP_NAME,
        "org.codehaus.wadi.tomcat50.TomcatManager");
    map.put(CARGO_CONTAINER_NAME_PROP_NAME, "tomcat5x");
    map = new HashMap();
    installProps.put(TOMCAT55_KEY_NAME, map);
    map.put(CONFIG_BASE_PROP_NAME, CONTAINER_CONFIG_DIR_BASE + "tomcat55");
    map.put(INSTALL_DIR_PROP_NAME, "target/installs");
    map.put(CONTAINER_URL_PROP_NAME,
        "http://www.apache.org/dist/tomcat/tomcat-5/v5.5.12/bin/apache-tomcat-5.5.12.zip");
    map.put(MANAGER_CLASS_NAME_PROP_NAME,
        "org.codehaus.wadi.tomcat55.TomcatManager");
    map.put(CARGO_CONTAINER_NAME_PROP_NAME, "tomcat5x");
    map
        .put(
            "tomcat55JDK14CompaitilityURL",
            "http://apache.hoxt.com/tomcat/tomcat-5/v5.5.12/bin/apache-tomcat-5.5.12-compat.zip");
            */
  }
}
