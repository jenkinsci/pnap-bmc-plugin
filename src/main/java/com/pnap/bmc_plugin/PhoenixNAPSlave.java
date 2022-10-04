package com.pnap.bmc_plugin;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.security.Security;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.jenkinsci.plugins.cloudstats.ProvisioningActivity;
import org.jenkinsci.plugins.cloudstats.ProvisioningActivity.Id;
import org.jenkinsci.plugins.cloudstats.TrackedItem;
import org.kohsuke.stapler.DataBoundConstructor;

import com.cloudbees.jenkins.plugins.sshcredentials.SSHUserPrivateKey;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.pnap.bmc_sdk.client.PNAPClient;
import com.pnap.bmc_sdk.command.CreateServerCommand;
import com.pnap.bmc_sdk.command.DeleteServerCommand;
import com.pnap.bmc_sdk.command.GetServerCommand;
import com.pnap.bmc_sdk.dto.CreateServerRequestDTO;
import com.pnap.bmc_sdk.dto.PNAPClientDTO;
import com.pnap.bmc_sdk.dto.ServerResponseDTO;

import hudson.Extension;
import hudson.model.Computer;
import hudson.model.TaskListener;
import hudson.security.ACL;
import hudson.slaves.AbstractCloudComputer;
import hudson.slaves.AbstractCloudSlave;
import hudson.slaves.Cloud;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import jenkins.model.Jenkins;

/**
 * @author pavlej
 *
 */
public final class PhoenixNAPSlave extends AbstractCloudSlave implements TrackedItem {

    /**
     *
     */
    private static final int BUFFER_SIZE = 4;
    /**
     *
     */
    private final ProvisioningActivity.Id provisioningId;
    /**
     *
     */
    private final PhoenixNAPSlaveTemplate template;
    /**
     *
     */
    private String provisionedServerID;
    /**
     *
     */
    private String templateName;

    /*
     * public PhoenixNAPSlave(String name, String remoteFS, ComputerLauncher
     * launcher, ProvisioningActivity.Id provisioningId) throws FormException,
     * IOException { super(name, remoteFS, launcher);template; this.template =
     * this.provisioningId = provisioningId; // TODO Auto-generated constructor stub
     * }
     */
    /**
     * Constructor for class PhoenixNAPSlave.
     *
     * @param template
     * @throws Exception
     */
    public PhoenixNAPSlave(final PhoenixNAPSlaveTemplate template) throws Exception {
        super(template.getName(), template.resolveFS(), template.resolveLauncher());

        String serverName = generateServerName(template.getCloud().getName(), template.getName());
        final ProvisioningActivity.Id provisioningId = new ProvisioningActivity.Id(template.getCloud().getName(),
                template.getName(), serverName);
        this.provisioningId = provisioningId;
        this.template = template;
        this.provisionedServerID = "0";
        // TODO add validation
        this.setNumExecutors(Integer.parseInt(template.getNumExecutors()));
        this.setLabelString(template.getLabelString());
        this.setNodeDescription("PhoenixNAPComputer running with name: " + template.getHostName());
        this.templateName = template.getName();
        this.setRetentionStrategy(new PhoenixNAPRetentionStrategy(getIdleterminationTime()));
    }

    /**
     * Constructor for class PhoenixNAPSlave.
     *
     * @param name
     * @param templateName
     * @param numExecutors
     * @param labelString
     * @param provisionedServerID
     * @throws Exception
     */
    @DataBoundConstructor
    public PhoenixNAPSlave(final String name, final String templateName, final String numExecutors, final String labelString,
            final String provisionedServerID) throws Exception {
        super(name, getTemplateByName(templateName).resolveFS(), getTemplateByName(templateName).resolveLauncher());
        this.template = getTemplateByName(templateName);
        this.templateName = template.getName();
        String serverName = generateServerName(template.getCloud().getName(), template.getName());
        final ProvisioningActivity.Id provisioningId = new ProvisioningActivity.Id(template.getCloud().getName(),
                template.getName(), serverName);
        this.provisioningId = provisioningId;
        this.provisionedServerID = provisionedServerID;
        // TODO add validation
        this.setNumExecutors(Integer.parseInt(numExecutors));
        this.setLabelString(labelString);
        this.setNodeDescription("PhoenixNAPComputer running with name: " + template.getHostName());
        this.setRetentionStrategy(new PhoenixNAPRetentionStrategy(getIdleterminationTime()));
    }

    @Override
    protected void _terminate(final TaskListener listener) throws IOException, InterruptedException {
        deleteServer();
    }

    @Extension
    public static final class DescriptorImpl extends SlaveDescriptor {
        @Nonnull
        @Override
        public String getDisplayName() {
            return "phoenixNAP Slave";
        }

        @Override
        public boolean isInstantiable() {
            return false;
        }

        /**
         * Fills template names.
         *
         * @return ListBoxModel of template names.
         */
        public ListBoxModel doFillTemplateNameItems() {

            ListBoxModel r = new ListBoxModel();
            final List<Cloud> clouds = Jenkins.get().clouds;
            // Cloud cloud = clouds.getCloud(name);
            for (Cloud cloud : clouds) {

                if (cloud instanceof PhoenixNAPCloud) {
                    List<PhoenixNAPSlaveTemplate> templates = ((PhoenixNAPCloud) cloud).getTemplates();
                    for (PhoenixNAPSlaveTemplate phoenixNAPSlaveTemplate : templates) {
                        r.add(phoenixNAPSlaveTemplate.getName(), phoenixNAPSlaveTemplate.getName());
                    }
                }
            }
            return r;
        }
    }

    @Override
    public AbstractCloudComputer<PhoenixNAPSlave> createComputer() {
        return new PhoenixNAPComputer(this);
    }

    @Override
    public Id getId() {
        System.out.println("Provisioning id called! " + provisioningId);
        // TODO Auto-generated method stub
        return provisioningId;
    }

    /**
     * Generates the server name.
     *
     * @param cloudName
     * @param slaveName
     * @return Generated server name.
     */
    public String generateServerName(final String cloudName, final String slaveName) {
        return "jenkins" + "-" + cloudName + "-" + slaveName + "-" + UUID.randomUUID().toString();
    }

    /**
     * Provisions the server.
     *
     * @throws IllegalStateException
     * @throws IOException
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public void provision() throws IllegalStateException, IOException, InterruptedException, ExecutionException {
        PNAPClientDTO dto = new PNAPClientDTO();
        dto.setAccessTokenURI("https://auth.phoenixnap.com/auth/realms/BMC/protocol/openid-connect/token");

        PhoenixNAPCloudCredentialsImpl credentials = getCredentials(template.getCredentialsId());
        if (credentials == null) {
            throw new IOException("Credentials are null for template.");
        }
        dto.setClientID(credentials.getClientID());
        dto.setClientSecret(credentials.getClientSecret());
        dto.setApiBaseURL("https://api.phoenixnap.com/bmc/v1");

        PNAPClient cl = new PNAPClient(dto);
        CreateServerRequestDTO server = new CreateServerRequestDTO();
        server.setHostname(template.getHostName());
        server.setLocation(template.getLocation());
        server.setOs(template.getOperatingSystem());
        server.setType(template.getType());
        SSHUserPrivateKey sshCred = getSshCredentials(template.getSshCredentialsId());
        if (sshCred == null) {
            throw new IOException("SSH Credentials are null for template.");
        }
        List<String> sshKeys = sshCred.getPrivateKeys();
        String passphrase = Secret.toString(sshCred.getPassphrase());
        String publicKey = getPublicKey(sshKeys.get(0), passphrase);
        List<String> keys = new ArrayList<>(1);
        keys.add(publicKey);
        server.setSshKeys(keys);
        CreateServerCommand createServerComand = new CreateServerCommand(cl, server);
        String response = createServerComand.execute();
        ServerResponseDTO createdServer = new ServerResponseDTO();
        createdServer.fromString(response);
        this.provisionedServerID = createdServer.getId();
        this.name = createdServer.getHostname() + "-" + createdServer.getId();

        Jenkins.get().addNode(this);

        Computer computer = toComputer();
        if (computer == null) {
            throw new IOException("Computer is null.");
        }
        computer.connect(false).get();

    }

    /**
     * Gets the ServerResponseDTO object.
     *
     * @return Gets the ServerResponseDTO object.
     * @throws Exception
     */
    public ServerResponseDTO getProvisionedServer() throws Exception {
        if (this.provisionedServerID != null && !"0".equals(this.provisionedServerID)) {
            PNAPClientDTO dto = new PNAPClientDTO();
            dto.setAccessTokenURI("https://auth.phoenixnap.com/auth/realms/BMC/protocol/openid-connect/token");
            PhoenixNAPCloudCredentialsImpl credentials = getCredentials(template.getCredentialsId());
            if (credentials == null) {
                throw new Exception("Credentials are null for template.");
            }
            dto.setClientID(credentials.getClientID());
            dto.setClientSecret(credentials.getClientSecret());
            dto.setApiBaseURL("https://api.phoenixnap.com/bmc/v1");
            PNAPClient cl = new PNAPClient(dto);
            GetServerCommand getServerComand = new GetServerCommand(cl, provisionedServerID);
            String response = getServerComand.execute();
            ServerResponseDTO server = new ServerResponseDTO();
            server.fromString(response);
            return server;
        }
        return null;
    }

    @CheckForNull
    private PhoenixNAPCloudCredentialsImpl getCredentials(@CheckForNull final String credentialsId) {
        if (StringUtils.isBlank(credentialsId)) {
            return null;
        }
        return CredentialsMatchers
                .firstOrNull(CredentialsProvider.lookupCredentials(PhoenixNAPCloudCredentialsImpl.class, Jenkins.get(),
                        ACL.SYSTEM, Collections.emptyList()), CredentialsMatchers.withId(credentialsId));
    }

    @CheckForNull
    private SSHUserPrivateKey getSshCredentials(@CheckForNull final String credentialsId) {
        if (StringUtils.isBlank(credentialsId)) {
            return null;
        }
        return CredentialsMatchers.firstOrNull(CredentialsProvider.lookupCredentials(SSHUserPrivateKey.class,
                Jenkins.get(), ACL.SYSTEM, Collections.emptyList()), CredentialsMatchers.withId(credentialsId));
    }

    private int getIdleterminationTime() {
        try {
            return Integer.parseInt(template.getIdleTerminationInMinutes());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Deletes the server.
     *
     * @throws IOException
     */
    public void deleteServer() throws IOException {

        if (this.provisionedServerID != null && !"0".equals(this.provisionedServerID)) {
            PNAPClientDTO dto = new PNAPClientDTO();
            dto.setAccessTokenURI("https://auth.phoenixnap.com/auth/realms/BMC/protocol/openid-connect/token");
            PhoenixNAPCloudCredentialsImpl credentials = getCredentials(template.getCredentialsId());
            if (credentials == null) {
                throw new IOException("Credentials are null for template.");
            }
            dto.setClientID(credentials.getClientID());
            dto.setClientSecret(credentials.getClientSecret());
            dto.setApiBaseURL("https://api.phoenixnap.com/bmc/v1");
            PNAPClient cl = new PNAPClient(dto);
            DeleteServerCommand deleteServerComand = new DeleteServerCommand(cl, provisionedServerID);
            String response = deleteServerComand.execute();
            this.provisionedServerID = "0";
            System.out.println(response);
        }
    }

    /**
     * Gets the PhoenixNAPSlaveTemplate object.
     *
     * @return The PhoenixNAPSlaveTemplate object.
     */
    public PhoenixNAPSlaveTemplate getTemplate() {
        return template;
    }

    private String getPublicKey(final String privateSshKey, final String privateSshKeyPassphrase)
            throws IOException, NotImplementedException {

        PEMKeyPair keyPair;
        RSAPublicKey rsaPubKey;
        Object keyObj = new PEMParser(new StringReader(privateSshKey)).readObject();

        // Key may be encrypted
        if (keyObj instanceof PEMKeyPair) {
            keyPair = (PEMKeyPair) keyObj;
        } else {
            // We need id_hmacWithSHA3_224 for encrypted ssh keys
            Security.addProvider(new BouncyCastleProvider());
            PEMEncryptedKeyPair encKeyPair = (PEMEncryptedKeyPair) keyObj;
            PEMDecryptorProvider decryptionProv = new JcePEMDecryptorProviderBuilder().setProvider("BC")
                    .build(privateSshKeyPassphrase.toCharArray());
            keyPair = encKeyPair.decryptKeyPair(decryptionProv);
        }

        try {
            rsaPubKey = (RSAPublicKey) new JcaPEMKeyConverter().getPublicKey(keyPair.getPublicKeyInfo());
        } catch (ClassCastException e) {
            throw new NotImplementedException("Only RSA SSH keys are currently supported");
        }

        byte[] pubKeyBody = getSshPublicKeyBody(rsaPubKey);
        String b64PubkeyBody = new String(java.util.Base64.getEncoder().encode(pubKeyBody), "UTF-8");

        return "ssh-rsa " + b64PubkeyBody;
    }

    private byte[] getSshPublicKeyBody(final RSAPublicKey rsaPubKey) throws IOException {
        byte[] algorithmName = "ssh-rsa".getBytes("UTF-8");
        byte[] algorithmNameLength = ByteBuffer.allocate(BUFFER_SIZE).putInt(algorithmName.length).array();
        byte[] e = rsaPubKey.getPublicExponent().toByteArray(); // Usually 65,537
        byte[] eLength = ByteBuffer.allocate(BUFFER_SIZE).putInt(e.length).array();
        byte[] m = rsaPubKey.getModulus().toByteArray();
        byte[] mLength = ByteBuffer.allocate(BUFFER_SIZE).putInt(m.length).array();

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        os.write(algorithmNameLength);
        os.write(algorithmName);
        os.write(eLength);
        os.write(e);
        os.write(mLength);
        os.write(m);

        return os.toByteArray();
    }

    private static PhoenixNAPSlaveTemplate getTemplateByName(final String templateName) {
        final List<Cloud> clouds = Jenkins.get().clouds;
        // Cloud cloud = clouds.getCloud(name);
        for (Cloud cloud : clouds) {

            if (cloud instanceof PhoenixNAPCloud) {
                List<PhoenixNAPSlaveTemplate> templates = ((PhoenixNAPCloud) cloud).getTemplates();
                for (PhoenixNAPSlaveTemplate phoenixNAPSlaveTemplate : templates) {
                    if (phoenixNAPSlaveTemplate.getName().equals(templateName)) {
                        return phoenixNAPSlaveTemplate;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Gets the template name.
     *
     * @return templateName
     */
    public String getTemplateName() {
        return templateName;
    }

    /**
     * Sets the template name.
     *
     * @param templateName
     */
    public void setTemplateName(final String templateName) {
        this.templateName = templateName;
    }

    /**
     * Gets the ID of provisioned server.
     *
     * @return provisionedServerID
     */
    public String getProvisionedServerID() {
        return provisionedServerID;
    }

    /**
     * Sets the provisioned server ID.
     *
     * @param provisionedServerID
     */
    public void setProvisionedServerID(final String provisionedServerID) {
        this.provisionedServerID = provisionedServerID;
    }

}
