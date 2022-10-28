package com.pnap.bmc_plugin;

import static com.cloudbees.plugins.credentials.CredentialsMatchers.anyOf;
import static com.cloudbees.plugins.credentials.CredentialsMatchers.instanceOf;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;

import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import com.cloudbees.jenkins.plugins.sshcredentials.SSHAuthenticator;
import com.cloudbees.jenkins.plugins.sshcredentials.SSHUserPrivateKey;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.slaves.Cloud;
import hudson.slaves.ComputerLauncher;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;

public final class PhoenixNAPAgentTemplate implements Describable<PhoenixNAPAgentTemplate>, Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 6637317957062974546L;
    /**
     * name.
     */
    private String name;
    /**
     * hostName.
     */
    private String hostName;
    /**
     * location.
     */
    private String location;
    /**
     * sshPort.
     */
    private String sshPort;
    /**
     * idleTerminationInMinutes.
     */
    private String idleTerminationInMinutes = "10";
    /**
     * type.
     */
    private String type;
    /**
     * operatingSystem.
     */
    private String operatingSystem;
    /**
     * numExecutors.
     */
    private String numExecutors = "1";
    /**
     * labelString.
     */
    private String labelString;
    /**
     * labellessJobsAllowed.
     */
    private Boolean labellessJobsAllowed;
    /**
     * credentialsId.
     */
    private String credentialsId;
    /**
     * sshCredentialsId.
     */
    private String sshCredentialsId;

    /**
     *
     */
    private static final Logger LOGGER = Logger.getLogger(PhoenixNAPAgentTemplate.class.getName());

    /**
     * Constructor for class PhoenixNAPAgentTemplate.
     *
     * @param name
     * @param hostName
     * @param location
     * @param type
     * @param operatingSystem
     * @param sshPort
     * @param idleTerminationInMinutes
     * @param numExecutors
     * @param labelString
     * @param labellessJobsAllowed
     * @param credentialsId
     * @param sshCredentialsId
     */
    @DataBoundConstructor
    public PhoenixNAPAgentTemplate(final String name, final String hostName, final String location, final String type, final String operatingSystem,
            final String sshPort, final String idleTerminationInMinutes, final String numExecutors, final String labelString,
            final Boolean labellessJobsAllowed, final String credentialsId, final String sshCredentialsId) {

        LOGGER.log(Level.FINE, "Creating PhoenixNAPAgentTemplate with name = {0}, location = {1}, type = {2}",
                new Object[] {name, location, type});

        this.name = name;
        this.hostName = hostName;
        this.location = location;
        this.type = type;
        this.sshPort = sshPort;
        this.operatingSystem = operatingSystem;
        this.idleTerminationInMinutes = idleTerminationInMinutes;
        this.numExecutors = numExecutors;
        this.labelString = labelString;
        this.labellessJobsAllowed = labellessJobsAllowed;
        this.credentialsId = credentialsId;
        this.sshCredentialsId = sshCredentialsId;

    }

    @SuppressWarnings("unchecked")
    @Override
    public Descriptor<PhoenixNAPAgentTemplate> getDescriptor() {
        return Jenkins.get().getDescriptor(getClass());
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<PhoenixNAPAgentTemplate> {
        /**
         *
         */
        public DescriptorImpl() {
            load();
        }

        /**
         * Gets display name.
         *
         * @return display name
         */
        @Nonnull
        @Override
        public String getDisplayName() {
            return "phoenixNAP Agent Template";
        }

        /**
         * @param context
         * @param credentialsId
         * @return ListBoxModel
         */
        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath final Item context,
                @QueryParameter final String credentialsId) {
            StandardListBoxModel result = new StandardListBoxModel();
            Jenkins instance = Jenkins.getInstanceOrNull();
            if (context != null && instance != null) {
                if (!context.hasPermission(Item.EXTENDED_READ)
                        && !context.hasPermission(CredentialsProvider.USE_ITEM)) {
                    return result.includeCurrentValue(credentialsId);
                }
            } else {
                if (instance != null && !instance.hasPermission(Jenkins.ADMINISTER)) {
                    return result.includeCurrentValue(credentialsId);
                }
            }

            List<DomainRequirement> domainRequirements = new ArrayList<>();

            return result.includeMatchingAs(ACL.SYSTEM, context, PhoenixNAPCloudCredentialsImpl.class,
                    domainRequirements, anyOf(instanceOf(PhoenixNAPCloudCredentialsImpl.class)));
        }

        /**
         * @param context
         * @param sshCredentialsId
         * @return ListBoxModel
         */
        public ListBoxModel doFillSshCredentialsIdItems(@AncestorInPath final Item context,
                @QueryParameter final String sshCredentialsId) {
            StandardListBoxModel result = new StandardListBoxModel();
            Jenkins instance = Jenkins.getInstanceOrNull();
            if (context == null) {
                if (instance != null && !instance.hasPermission(Jenkins.ADMINISTER)) {
                    return result.includeCurrentValue(sshCredentialsId);
                }
            } else {
                if (!context.hasPermission(Item.EXTENDED_READ)
                        && !context.hasPermission(CredentialsProvider.USE_ITEM)) {
                    return result.includeCurrentValue(sshCredentialsId);
                }
            }

            List<DomainRequirement> domainRequirements = new ArrayList<DomainRequirement>();
            return result.includeMatchingAs(ACL.SYSTEM, context, SSHUserPrivateKey.class, domainRequirements,
                    SSHAuthenticator.matcher());
        }

        /**
         * Fills operating system items.
         *
         * @return ListBoxModel
         */
        public ListBoxModel doFillOperatingSystemItems() {
            ListBoxModel operatingSystem = new ListBoxModel();

            operatingSystem.add("ubuntu/bionic", "ubuntu/bionic");
            operatingSystem.add("centos/centos7", "centos/centos7");
            //operatingSystem.add("windows/srv2019std", "windows/srv2019std");

            return operatingSystem;
        }

        /**
         * Fills type items.
         *
         * @return ListBoxModel
         */
        public ListBoxModel doFillTypeItems() {
            ListBoxModel type = new ListBoxModel();

            type.add("s1.c1.small", "s1.c1.small");
            type.add("s1.c1.medium", "s1.c1.medium");
            type.add("s1.c2.medium", "s1.c2.medium");
            type.add("s1.c2.large", "s1.c2.large");
            type.add("d1.c1.small", "d1.c1.small");
            type.add("d1.c2.small", "d1.c2.small");
            type.add("d1.c3.small", "d1.c3.small");
            type.add("d1.c4.small", "d1.c4.small");
            type.add("d1.c1.medium", "d1.c1.medium");
            type.add("d1.c2.medium", "d1.c2.medium");
            type.add("d1.c3.medium", "d1.c3.medium");
            type.add("d1.c4.medium", "d1.c4.medium");
            type.add("d1.c1.large", "d1.c1.large");
            type.add("d1.c2.large", "d1.c2.large");
            type.add("d1.c3.large", "d1.c3.large");
            type.add("d1.c4.large", "d1.c4.large");
            type.add("d1.m1.medium", "d1.m1.medium");
            type.add("d1.m2.medium", "d1.m2.medium");
            type.add("d1.m3.medium", "d1.m3.medium");
            type.add("d1.m4.medium", "d1.m4.medium");

            return type;
        }

        /**
         * Fills location items.
         *
         * @return ListBoxModel
         */
        public ListBoxModel doFillLocationItems() {
            ListBoxModel location = new ListBoxModel();

            location.add("PHX", "PHX");
            location.add("ASH", "ASH");
            location.add("SGP", "SGP");
            location.add("NLD", "NLD");

            return location;
        }

        /**
         * Checks host name.
         *
         * @param value
         * @return FormValidation
         */
        public FormValidation doCheckHostName(@QueryParameter final String value) {
            if (value == null || "".equals(value)) {
                return FormValidation.error("Must not be empty!");
            } else {
                return FormValidation.ok();
            }
        }

        /**
         * Checks name.
         *
         * @param value
         * @return FormValidation
         */
        public FormValidation doCheckName(@QueryParameter final String value) {
            if (value == null || "".equals(value)) {
                return FormValidation.error("Must not be empty!");
            } else {
                return FormValidation.ok();
            }
        }

        /**
         * Checks number of executors.
         *
         * @param value
         * @return FormValidation
         * @throws IOException
         * @throws ServletException
         */
        public FormValidation doCheckNumExecutors(@QueryParameter final String value) throws IOException, ServletException {
            try {
                Integer.parseInt(value);
                return FormValidation.ok();
            } catch (NumberFormatException e) {
                return FormValidation.error("Must be a number");
            }
        }

        /**
         * Checks idle termination in minutes.
         *
         * @param value
         * @return FormValidation
         * @throws IOException
         * @throws ServletException
         */
        public FormValidation doCheckIdleTerminationInMinutes(@QueryParameter final String value)
                throws IOException, ServletException {
            try {
                Integer.parseInt(value);
                return FormValidation.ok();
            } catch (NumberFormatException e) {
                return FormValidation.error("Must be a number");
            }
        }
    }

    /**
     * Gets the name.
     *
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name.
     *
     * @param name
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Gets the location.
     *
     * @return location
     */
    public String getLocation() {
        return location;
    }

    /**
     * Sets the location.
     *
     * @param location
     */
    public void setLocation(final String location) {
        this.location = location;
    }

    /**
     * Gets the SSH port.
     *
     * @return sshPort
     */
    public String getSshPort() {
        return sshPort;
    }

    /**
     * Sets the SSH port.
     *
     * @param sshPort
     */
    public void setSshPort(final String sshPort) {
        this.sshPort = sshPort;
    }

    /**
     * Gets the idle termination in minutes.
     *
     * @return idleTerminationInMinutes
     */
    public String getIdleTerminationInMinutes() {
        return idleTerminationInMinutes;
    }

    /**
     * Sets the idle termination in minutes.
     *
     * @param idleTerminationInMinutes
     */
    public void setIdleTerminationInMinutes(final String idleTerminationInMinutes) {
        this.idleTerminationInMinutes = idleTerminationInMinutes;
    }

    /**
     * Gets the type.
     *
     * @return type
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the type.
     *
     * @param type
     */
    public void setType(final String type) {
        this.type = type;
    }

    /**
     * Gets the operating system.
     *
     * @return operatingSystem
     */
    public String getOperatingSystem() {
        return operatingSystem;
    }

    /**
     * Sets the operating system.
     *
     * @param operatingSystem
     */
    public void setOperatingSystem(final String operatingSystem) {
        this.operatingSystem = operatingSystem;
    }

    /**
     * Gets the number of executors.
     *
     * @return labelString
     */
    public String getNumExecutors() {
        return numExecutors;
    }

    /**
     * Sets the number of executors.
     *
     * @param numExecutors
     */
    public void setNumExecutors(final String numExecutors) {
        this.numExecutors = numExecutors;
    }

    /**
     * Gets the labelString.
     *
     * @return labelString
     */
    public String getLabelString() {
        return labelString;
    }

    /**
     * Sets the labelString.
     *
     * @param labelString
     */
    public void setLabelString(final String labelString) {
        this.labelString = labelString;
    }

    /**
     * Gets the labellessJobsAllowed.
     *
     * @return labellessJobsAllowed
     */
    public Boolean getLabellessJobsAllowed() {
        return labellessJobsAllowed;
    }

    /**
     * Sets the labellessJobsAllowed.
     *
     * @param labellessJobsAllowed
     */
    public void setLabellessJobsAllowed(final Boolean labellessJobsAllowed) {
        this.labellessJobsAllowed = labellessJobsAllowed;
    }

    /**
     * Gets the credentials id.
     *
     * @return credentialsId
     */
    public String getCredentialsId() {
        return credentialsId;
    }

    /**
     * Sets the credentials Id.
     *
     * @param credentialsId
     */
    public void setCredentialsId(final String credentialsId) {
        this.credentialsId = credentialsId;
    }

    /**
     * Gets the SSH credentials id.
     *
     * @return sshCredentialsId
     */
    public String getSshCredentialsId() {
        return sshCredentialsId;
    }

    /**
     * Sets the SSH credentials id.
     *
     * @param sshCredentialsId
     */
    public void setSshCredentialsId(final String sshCredentialsId) {
        this.sshCredentialsId = sshCredentialsId;
    }

    /**
     * Returns PhoenixNAPCloud object.
     *
     * @return PhoenixNAPCloud
     * @throws Exception
     */
    public PhoenixNAPCloud getCloud() throws Exception {

        final List<Cloud> clouds = Jenkins.get().clouds;
        // Cloud cloud = clouds.getCloud(name);
        for (Cloud cloud : clouds) {

            if (cloud instanceof PhoenixNAPCloud) {
                List<PhoenixNAPAgentTemplate> templates = ((PhoenixNAPCloud) cloud).getTemplates();
                for (PhoenixNAPAgentTemplate phoenixNAPAgentTemplate : templates) {
                    if (this.equals(phoenixNAPAgentTemplate)) {
                        return (PhoenixNAPCloud) cloud;
                    }
                }
            }
        }
        throw new Exception("Cloud can not be found for template: " + this.name);

    }

    /**
     * Returns PhoenixNAPComputerLauncher with implemented custom logic to launch pnap instances.
     *
     * @return PhoenixNAPComputerLauncher object.
     */
    public ComputerLauncher resolveLauncher() {
        return new PhoenixNAPComputerLauncher();
    }

    /**
     * Resolves the file system path.
     *
     * @return Resolved file system path.
     */
    public String resolveFS() {
        if (this.operatingSystem.contains("ubuntu")) {
            return "/home/ubuntu";
        } else if (this.operatingSystem.contains("centos")) {
            return "/home/centos";
        } else if (this.operatingSystem.contains("windows")) {
            return "/jenkins";
        }
        return "/";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((credentialsId == null) ? 0 : credentialsId.hashCode());
        result = prime * result + ((idleTerminationInMinutes == null) ? 0 : idleTerminationInMinutes.hashCode());
        result = prime * result + ((labelString == null) ? 0 : labelString.hashCode());
        result = prime * result + ((labellessJobsAllowed == null) ? 0 : labellessJobsAllowed.hashCode());
        result = prime * result + ((location == null) ? 0 : location.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((numExecutors == null) ? 0 : numExecutors.hashCode());
        result = prime * result + ((operatingSystem == null) ? 0 : operatingSystem.hashCode());
        result = prime * result + ((sshCredentialsId == null) ? 0 : sshCredentialsId.hashCode());
        result = prime * result + ((sshPort == null) ? 0 : sshPort.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        PhoenixNAPAgentTemplate other = (PhoenixNAPAgentTemplate) obj;
        if (credentialsId == null) {
            if (other.credentialsId != null) {
                return false;
            }
        } else if (!credentialsId.equals(other.credentialsId)) {
            return false;
        }
        if (idleTerminationInMinutes == null) {
            if (other.idleTerminationInMinutes != null) {
                return false;
            }
        } else if (!idleTerminationInMinutes.equals(other.idleTerminationInMinutes)) {
            return false;
        }
        if (labelString == null) {
            if (other.labelString != null) {
                return false;
            }
        } else if (!labelString.equals(other.labelString)) {
            return false;
        }
        if (labellessJobsAllowed == null) {
            if (other.labellessJobsAllowed != null) {
                return false;
            }
        } else if (!labellessJobsAllowed.equals(other.labellessJobsAllowed)) {
            return false;
        }
        if (location == null) {
            if (other.location != null) {
                return false;
            }
        } else if (!location.equals(other.location)) {
            return false;
        }
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        if (numExecutors == null) {
            if (other.numExecutors != null) {
                return false;
            }
        } else if (!numExecutors.equals(other.numExecutors)) {
            return false;
        }
        if (operatingSystem == null) {
            if (other.operatingSystem != null) {
                return false;
            }
        } else if (!operatingSystem.equals(other.operatingSystem)) {
            return false;
        }
        if (sshCredentialsId == null) {
            if (other.sshCredentialsId != null) {
                return false;
            }
        } else if (!sshCredentialsId.equals(other.sshCredentialsId)) {
            return false;
        }
        if (sshPort == null) {
            if (other.sshPort != null) {
                return false;
            }
        } else if (!sshPort.equals(other.sshPort)) {
            return false;
        }
        if (type == null) {
            if (other.type != null) {
                return false;
            }
        } else if (!type.equals(other.type)) {
            return false;
        }
        return true;
    }

    /**
     * Gets the host name.
     *
     * @return The host name.
     */
    public String getHostName() {
        return hostName;
    }

    /**
     * Sets the host name.
     *
     * @param hostName - the host name.
     */
    public void setHostName(final String hostName) {
        this.hostName = hostName;
    }

}
