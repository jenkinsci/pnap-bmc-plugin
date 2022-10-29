package io.jenkins.plugins.pnap_bmc;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;
import org.springframework.security.oauth2.client.resource.OAuth2AccessDeniedException;
import org.springframework.web.client.HttpClientErrorException;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import com.pnap.bmc_sdk.client.PNAPClient;
import com.pnap.bmc_sdk.command.GetServersCommand;
import com.pnap.bmc_sdk.dto.PNAPClientDTO;
import com.pnap.bmc_sdk.dto.ServersResponseDTO;

import hudson.Extension;
import hudson.util.FormValidation;
import hudson.util.Secret;

public final class PhoenixNAPCloudCredentialsImpl extends BaseStandardCredentials {
    /**
     *
     */
    private static final long serialVersionUID = 8798352706232563219L;

    /**
     * clientID from BMC Portal Application credentials.
     */
    private String clientID;
    /**
     * clientSecret from BMC Portal Application credentials.
     */
    private Secret clientSecret;

    /**
     * @param scope
     * @param id
     * @param description
     * @param clientID
     * @param clientSecret
     */
    @DataBoundConstructor
    public PhoenixNAPCloudCredentialsImpl(final CredentialsScope scope, final String id, final String description,
            final String clientID, final Secret clientSecret) {
        super(scope, id, description);
        // TODO Auto-generated constructor stub
        this.clientID = clientID;
        this.clientSecret = clientSecret;
    }

    protected String getEncryptedValue(final String str) {
        return Secret.fromString(str).getEncryptedValue();
    }

    protected String getPlainText(final Secret secret) {
        if (secret != null) {
            //Secret secret = Secret.decrypt(str);
           // if (secret != null) {
                return secret.getPlainText();
            //}
        }
        return null;
    }

    @Extension
    public static final class DescriptorImpl extends BaseStandardCredentials.BaseStandardCredentialsDescriptor {

        @Override
        public String getDisplayName() {
            return "phoenixNAP Credentials";
        }

        /**
         * @param clientID
         * @param clientSecret
         * @return FormValidation
         */
        @POST
        public FormValidation doTestConnection(@QueryParameter final String clientID,
                @QueryParameter final String clientSecret) {
            try {
                PNAPClientDTO dto = new PNAPClientDTO();
                dto.setAccessTokenURI("https://auth.phoenixnap.com/auth/realms/BMC/protocol/openid-connect/token");
                dto.setClientID(clientID);
                dto.setClientSecret(clientSecret);
                //System.out.println(Secret.toString(Secret.decrypt(clientSecret)));
                dto.setApiBaseURL("https://api.phoenixnap.com/bmc/v1");

                PNAPClient cl = new PNAPClient(dto);
                GetServersCommand getServersComand = new GetServersCommand(cl);
                String response = getServersComand.execute();
                ServersResponseDTO servers = new ServersResponseDTO();
                servers.fromString(response);
                //System.out.println(response);
                return FormValidation.ok("Connection succesfully established.");
            } catch (OAuth2AccessDeniedException e) {
                return FormValidation.error("Connection can not be established, please check the credentials.");
            } catch (HttpClientErrorException e) {
                return FormValidation.error("Connection can not be established, please check the credentials.");
            } catch (Exception e) {
                return FormValidation.error("Connection can not be established, please check the credentials.");
            }
        }
    }


    /**
     * @return clientID.
     */
    public String getClientID() {
        return clientID;
    }

    /**
     * @param clientID
     */
    public void setClientID(final String clientID) {
        this.clientID = clientID;
    }

    /**
     * @return clientSecret.
     */
    public Secret getClientSecret() {
        return clientSecret;
    }

    /**
     * @param clientSecret
     */
    public void setClientSecret(final Secret clientSecret) {
        this.clientSecret = clientSecret;
    }


}
