/**
 * This work was created by participants in the DataONE project, and is
 * jointly copyrighted by participating institutions in DataONE. For
 * more information on DataONE, see our web site at http://dataone.org.
 *
 *   Copyright ${year}
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * $Id$
 */

package org.dataone.integration.webTest;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.nio.channels.FileLock;
import java.security.ProtectionDomain;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Provides a main routine for running the web app from the command line.
 *
 * Accepts system properties: port : the port to listen on unasassembled : set
 * to true to run as an unpackaged app, useful for example for running the
 * debugger in eclipse.
 *
 * When running in unpackaged mode for debugging, please note that the
 * TestRunnerHttpServlet that it deploys does require d1_integration to be
 * packaged, otherwise it will not find the integration tests. Therefore, you
 * will still need to run 'mvn package' from the command line before running
 * through Eclipse (as well as setting unassembled mode). You will also need to
 * rerun 'mvn package' to incorporate any code changes you may have made between
 * runs.
 */
public final class Launcher {

    private final static String LOCKFILE = "/tmp/d1_integration_webTest.lock";
    protected static Logger log;
    private static Integer listenPort = 6680;
    protected static Boolean IS_UNASSEMBLED_DEFAULT = false; // please don't
    // change this from
    // false!
    protected static final String UNASSEMBLED_PROPERTY = "unasassembled";
    protected static final String PORT_PROPERTY = "port";
    private static Server server;

    public static void main(String[] args)
    {
        if ( !lockInstance(LOCKFILE) ) {
            System.out.println("Oops, another instance of the DataONE WebTester"
                    + " seems to be running on this machine.");
            System.out.println("If you believe that not to be accurate, then delete "
                    + LOCKFILE + " and try again.");
            System.exit(1);
        }

        log = LoggerFactory.getLogger(Launcher.class);

        listenPort = Integer.parseInt(System.getProperty(PORT_PROPERTY,
                listenPort.toString()));
        server = new Server(listenPort);
        log.info(String.format("Listening on port %d", listenPort));

        boolean isUnassembled = getBooleanProperty(UNASSEMBLED_PROPERTY,
                IS_UNASSEMBLED_DEFAULT);
        // unassembled mode is for running through IDE like Eclipse without
        // first packaging into .war
        // see
        // http://wiki.eclipse.org/Jetty/Tutorial/Embedding_Jetty#Setting_a_Web_Application_Context

        WebAppContext context = new WebAppContext();
        if ( isUnassembled ) {
            log.info("Running in unassaembled mode.");
            String webapp = "";// "src/main/webaapp";
            context.setDescriptor(webapp + "/WEB-INF/web.xml");
            context.setResourceBase("../d1_integration/src/main/webapp");
            context.setContextPath("/");
            context.setParentLoaderPriority(true);

            // the following also worked for running in Eclipse
            // log.info("Running as war at location: " +
            // location.toExternalForm());
            // context.setWar(location.toExternalForm() +
            // "../d1_integration-1.1-SNAPSHOT/");
            // context.setDescriptor(location.toExternalForm() +
            // "../d1_integration-1.1-SNAPSHOT/WEB-INF/web.xml");
            // context.setServer(server);

        }
        else {
            ProtectionDomain domain = Launcher.class.getProtectionDomain();
            URL location = domain.getCodeSource().getLocation();
            context.setContextPath("/");
            context.setWar(location.toExternalForm());

        }
        server.setHandler(context);

        Thread monitor = new MonitorThread(listenPort + 1);
        monitor.start();
        try {
            server.start();
            server.join();
        }
        catch (InterruptedException e) {
            try {
                server.stop();
                try {
                    Thread.sleep(1000);
                }
                catch (InterruptedException ee) {
                    //
                }
            }
            catch (Exception e1) {
                e1.printStackTrace();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * gets the System property value for the named property
     *
     * @param name
     *            - name of the system property to get
     * @param defaultValue
     *            - the default value for the property if not found
     * @return
     */
    public static boolean getBooleanProperty(String name, boolean defaultValue)
    {
        String prop = System.getProperty(name);
        if ( prop == null ) {
            return defaultValue;
        }
        String truths[] = { "true", "yes", "si", "ok", "1", "yep", "on" };
        for (String truth : truths) {
            if ( prop.equalsIgnoreCase(truth) ) {
                return true;
            }
        }
        return false;
    }

    /*
     * Simple check to ensure that only one instance is running
     */
    private static boolean lockInstance(final String lockFile)
    {
        try {
            final File file = new File(lockFile);
            final RandomAccessFile randomAccessFile = new RandomAccessFile(
                    file, "rw");
            final FileLock fileLock = randomAccessFile.getChannel().tryLock();
            if ( fileLock != null ) {
                Runtime.getRuntime().addShutdownHook(new Thread() {
                    public void run()
                    {
                        try {
                            fileLock.release();
                            randomAccessFile.close();
                            file.delete();
                            System.out.println("Lock file removed.");
                        }
                        catch (Exception e) {
                            System.out.println("Unable to remove lock file: "
                                    + lockFile + "\n" + e.getMessage());
                        }
                    }
                });
                return true;
            }
        }
        catch (Exception e) {
            log.error("Unable to create and/or lock file: " + lockFile, e);
        }
        return false;
    }

    /*
     * This thread provides a mechanism for a controlled shutdown of the Jetty
     * server, which in turn provides a clean shutdown of d1_integration
     * Servlet. To shut down, telnet to the port+1 that the jetty server is
     * listening on, and press enter. The jetty server should then shutdown.
     */
    private static class MonitorThread extends Thread {

        private ServerSocket socket;
        private int listenPort;

        public MonitorThread(int listenPort) {
            setDaemon(true);
            setName("StopMonitor");
            this.listenPort = listenPort;
            try {
                socket = new ServerSocket(listenPort, 1,
                        InetAddress.getByName("127.0.0.1"));
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void run()
        {
            log.info(String.format("Running WebTester stop thread on port %d",
                    this.listenPort));
            log.info(String.format(
                    "Telnet to localhost %d and press enter to shut me down",
                    this.listenPort));
            Socket accept;
            try {
                accept = socket.accept();
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(accept.getInputStream()));
                reader.readLine();
                log.info("Stopping jetty embedded server");
                server.stop();
                accept.close();
                socket.close();
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

}