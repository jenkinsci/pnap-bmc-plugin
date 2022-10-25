package com.pnap.bmc_plugin;

import static java.lang.String.format;

import java.io.IOException;
import java.io.PrintStream;
import java.net.SocketTimeoutException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.CheckForNull;

import org.apache.commons.lang.StringUtils;

import com.cloudbees.jenkins.plugins.sshcredentials.SSHUserPrivateKey;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.google.common.base.Strings;
import com.pnap.bmc_plugin.exception.CloudNotFoundException;
import com.pnap.bmc_plugin.exception.NodeNotFoundException;
import com.pnap.bmc_plugin.exception.ServerNotFoundException;
import com.pnap.bmc_sdk.dto.ServerResponseDTO;
import com.trilead.ssh2.Connection;
import com.trilead.ssh2.SCPClient;
import com.trilead.ssh2.Session;

import hudson.model.TaskListener;
import hudson.remoting.Channel;
import hudson.security.ACL;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.SlaveComputer;
import hudson.util.Secret;
import jenkins.model.Jenkins;

/**
 * @author pavlej
 * PhoenixNAPComputerLauncher extends Computer launcher and implement custom logic to launch pnap instances.
 */
public final class PhoenixNAPComputerLauncher extends ComputerLauncher {

    /**
     * NUM_OF_SECONDS.
     */
    private static final int NUM_OF_SECONDS = 10;

    /**
     * MILLIS_IN_SECOND.
     */
    private static final int MILLIS_IN_SECOND = 1000;

    /**
     * SSH_PORT.
     */
    private static final int SSH_PORT = 22;

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(PhoenixNAPComputerLauncher.class.getName());

    /**
     * VALID_VERSIONS.
     */
    private static final List<String> VALID_VERSIONS = Arrays.asList(/*"1.8", "1.9",*/ "11");

    private abstract class JavaInstaller {
        protected abstract String getInstallCommand(String javaVersion);

        protected abstract String checkPackageManager();

        protected boolean isUsable(final Connection conn, final PrintStream logger) throws IOException, InterruptedException {
            return checkCommand(conn, logger, checkPackageManager());
        }

        private boolean checkCommand(final Connection conn, final PrintStream logger, final String command)
                throws IOException, InterruptedException {
            logger.println("Checking: " + command);
            return conn.exec(command, logger) == 0;
        }

        protected int installJava(final Connection conn, final PrintStream logger) throws IOException, InterruptedException {
            int result = 1;
            for (String version : VALID_VERSIONS) {
                result = conn.exec(getInstallCommand(version), logger);
                if (result == 0) {
                    return result;
                }
            }
            return result;
        }
    }

    /**
     * Collection of Installers, objects that are responsible to install java on different OS.
     */
    private final Collection<JavaInstaller> iNSTALLERS = new HashSet<JavaInstaller>() {
        {
            add(new JavaInstaller() { // apt
                @Override
                protected String getInstallCommand(final String javaVersion) {
                    return "sudo apt-get update -q && sudo apt-get install -y " + getPackageName(javaVersion);
                }

                @Override
                protected String checkPackageManager() {
                    return "which apt-get";
                }

                private String getPackageName(final String javaVersion) {
                    //String s = "openjdk-" + javaVersion.replaceFirst("1.", "") + "-jre-headless";
                    return "openjdk-" + javaVersion + "-jre-headless";
                }
            });
            add(new JavaInstaller() { // yum
                @Override
                protected String getInstallCommand(final String javaVersion) {
                    return "sudo yum install -y " + getPackageName(javaVersion);
                }

                @Override
                protected String checkPackageManager() {
                    return "which yum";
                }

                private String getPackageName(final String javaVersion) {
                    return "java-" + javaVersion + ".0-openjdk-headless";
                }
            });
        }
    };

    @Override
    public void launch(final SlaveComputer ccomputer, final TaskListener listener) {
        PrintStream logger = listener.getLogger();

        if (!(ccomputer instanceof PhoenixNAPComputer)) {
            logger.println("Cannot handle, agent not instance of PhoenixNAP Computer.");
            return;
        }
        PhoenixNAPComputer phoenixNAPComputer = (PhoenixNAPComputer) ccomputer;
        Date startDate = new Date();
        logger.println("Launch Start time: " + getUtcDate(startDate));

        final Connection conn;
        Connection cleanupConn = null;
        boolean successful = false;
        PhoenixNAPAgent node = null;
        try {
            conn = getSSHConnection(phoenixNAPComputer, listener);
            cleanupConn = conn;
            node = phoenixNAPComputer.getNode();

            if (node == null) {
                throw new Exception("PhoenixNAPAgent is null.");
            }
            SSHUserPrivateKey sshCredentials = getSshCredentials(node.getTemplate().getSshCredentialsId());
            if (sshCredentials == null) {
                throw new Exception("SSH credentials are null.");
            }
            logger.println("Authenticating as " + sshCredentials.getUsername());

            if (!conn.authenticateWithPublicKey(sshCredentials.getUsername(),
                    sshCredentials.getPrivateKeys().get(0).toCharArray(),
                    Secret.toString(sshCredentials.getPassphrase()))) {
                logger.println("Authentication failed");
                throw new Exception("Authentication failed");
            }
            final SCPClient scp = conn.createSCPClient();
            if (!installJava(logger, conn)) {
                LOGGER.severe("Failed to launch: java installation failed to run " + phoenixNAPComputer.getName());
                throw new Exception("Installing java failed.");
            }
            logger.println("Copying agent.jar");
            scp.put(Jenkins.get().getJnlpJars("agent.jar").readFully(), "agent.jar", "/tmp");
            String launchString = "java " + " -jar /tmp/agent.jar";
            logger.println("Launching agent agent: " + launchString);
            final Session sess = conn.openSession();
            sess.execCommand(launchString);
            phoenixNAPComputer.setChannel(sess.getStdout(), sess.getStdin(), logger, new Channel.Listener() {
                @Override
                public void onClosed(final Channel channel, final IOException cause) {
                    sess.close();
                    conn.close();
                }
            });

            successful = true;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, e.getMessage(), e);
            deprovisionNode(logger, node);
            e.printStackTrace(logger);
        } finally {
            Date endDate = new Date();
            logger.println("Launch End time: " + getUtcDate(endDate));
            logger.println(
                    "Done in " + TimeUnit.MILLISECONDS.toSeconds(endDate.getTime() - startDate.getTime()) + " seconds");
            if (cleanupConn != null && !successful) {
                cleanupConn.close();
            }
        }
    }

    /**
     * @param logger
     * @param node
     * Removes PhoenixNAP node.
     */
    private void deprovisionNode(final PrintStream logger, final PhoenixNAPAgent node) {
        try {
            node.deleteServer();
            Jenkins.get().removeNode(node);
        } catch (Exception ee) {
            ee.printStackTrace(logger);
        }

    }

    /**
     * @param computer
     * @param listener
     * @return Connection
     */
    public Connection getSSHConnection(final PhoenixNAPComputer computer, final TaskListener listener) {

        PhoenixNAPAgent node = computer.getNode();
        PhoenixNAPCloud cloud = getCloud(node);
        final long timeout = TimeUnit.MINUTES.toMillis(Integer.parseInt(cloud.getTimeoutMinutes()));
        final long startTime = System.currentTimeMillis();
        final int sleepTime = Integer.parseInt(cloud.getConnectionRetryWait());
        long waitTime;
        PrintStream logger = listener.getLogger();
        while ((waitTime = System.currentTimeMillis() - startTime) < timeout) {
            try {
                if (node == null) {
                    throw new NodeNotFoundException();

                }
                ServerResponseDTO server = getServer(node);
                if (!"powered-on".equals(server.getStatus())) {
                    logger.println("Waiting for server to enter Powered on state. Sleeping " + sleepTime + " seconds.");
                } else {
                    final String host = server.getPublicIpAddresses().get(0);

                    if (Strings.isNullOrEmpty(host) || "0.0.0.0".equals(host)) {
                        logger.println("No ip address yet, your host is most likely waiting for an ip address.");
                    } else {
                        logger.println("Connecting to " + host + " on port " + SSH_PORT + ". ");
                        Connection conn = new Connection(host, SSH_PORT);

                        conn.connect(null, NUM_OF_SECONDS * MILLIS_IN_SECOND, NUM_OF_SECONDS * MILLIS_IN_SECOND);
                        logger.println("Connected via SSH.");
                        return conn;

                    }
                }

            } catch (CloudNotFoundException e) {
                logger.println("Failed to get cloud. Retrying");
            } catch (NodeNotFoundException e) {
                logger.println("Failed to get node. Retrying");
            } catch (ServerNotFoundException e) {
                logger.println("Failed to get server. Retrying");
            } catch (SocketTimeoutException e) {
                logger.println("Waiting for SSH to come up. Sleeping " + sleepTime + " seconds.");
            } catch (IOException e) {
                logger.println("Waiting for SSH to come up. Sleeping " + sleepTime + " seconds.");
            }
            sleep(sleepTime);
        }

        throw new RuntimeException(format(
                "Timed out after %d seconds of waiting for ssh to become available (max timeout configured is %s)",
                waitTime / MILLIS_IN_SECOND, timeout / MILLIS_IN_SECOND));

    }

    private PhoenixNAPCloud getCloud(final PhoenixNAPAgent node) throws CloudNotFoundException {
        try {
            PhoenixNAPCloud cloud = node.getTemplate().getCloud();
            return cloud;
        } catch (Exception e1) {
            throw new CloudNotFoundException();
        }

    }

    private ServerResponseDTO getServer(final PhoenixNAPAgent node) throws ServerNotFoundException {
        ServerResponseDTO server;
        try {
            server = node.getProvisionedServer();
            return server;
        } catch (Exception e) {
            throw new ServerNotFoundException(e.getMessage());
        }
    }

    private void sleep(final int seconds) {
        try {
            Thread.sleep(seconds * MILLIS_IN_SECOND);
        } catch (InterruptedException e) {
            // Ignore
        }
    }

    private boolean installJava(final PrintStream logger, final Connection conn)
            throws IOException, InterruptedException {
        logger.println("Verifying that java exists");
        if (conn.exec("java -fullversion", logger) != 0) {
            logger.println("Try to install one of these Java-versions: " + VALID_VERSIONS);
            // TODO Web UI to let users install a custom java (or any other type of tool)
            // package.
            logger.println("Trying to find a working package manager");

            for (JavaInstaller installer : iNSTALLERS) {
                if (!installer.isUsable(conn, logger)) {
                    continue;
                }
                if (installer.installJava(conn, logger) == 0) {
                    return true;
                }
            }

            logger.println("Java could not be installed using any of the supported package managers");
            return false;
        }
        return true;
    }

    private String getUtcDate(final Date date) {
        SimpleDateFormat utcFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
        utcFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return utcFormat.format(date);
    }

    @CheckForNull
    private SSHUserPrivateKey getSshCredentials(@CheckForNull final String credentialsId) {
        if (StringUtils.isBlank(credentialsId)) {
            return null;
        }
        return CredentialsMatchers.firstOrNull(CredentialsProvider.lookupCredentials(SSHUserPrivateKey.class,
                Jenkins.get(), ACL.SYSTEM, Collections.emptyList()), CredentialsMatchers.withId(credentialsId));
    }

}
