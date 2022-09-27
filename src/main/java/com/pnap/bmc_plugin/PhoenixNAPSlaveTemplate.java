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

public final class PhoenixNAPSlaveTemplate implements Describable<PhoenixNAPSlaveTemplate>, Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 6637317957062974546L;
    private String name;
    private String hostName;
    private String location;
    private String sshPort;
    private String idleTerminationInMinutes = "10";
    private String type;
    private String operatingSystem;
    private String numExecutors = "1";
    private String labelString;
    private Boolean labellessJobsAllowed;
    private String credentialsId;
    private String sshCredentialsId;

    private static final Logger LOGGER = Logger.getLogger(PhoenixNAPSlaveTemplate.class.getName());

    @DataBoundConstructor
    public PhoenixNAPSlaveTemplate(String name, String hostName, String location, String type, String operatingSystem,
            String sshPort, String idleTerminationInMinutes, String numExecutors, String labelString,
            Boolean labellessJobsAllowed, String credentialsId, String sshCredentialsId) {

        LOGGER.log(Level.INFO, "Creating PhoenixNAPSlaveTemplate with name = {0}, location = {1}, type = {2}",
                new Object[] { name, location, type });

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
    public Descriptor<PhoenixNAPSlaveTemplate> getDescriptor() {
        return Jenkins.get().getDescriptor(getClass());
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<PhoenixNAPSlaveTemplate> {
        public DescriptorImpl() {
            load();
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "phoenixNAP Slave Template";
        }

        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item context,
                @QueryParameter String credentialsId) {
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

        public ListBoxModel doFillSshCredentialsIdItems(@AncestorInPath Item context,
                @QueryParameter String sshCredentialsId) {
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

        public ListBoxModel doFillOperatingSystemItems() {
            ListBoxModel operatingSystem = new ListBoxModel();

            operatingSystem.add("ubuntu/bionic", "ubuntu/bionic");
            operatingSystem.add("centos/centos7", "centos/centos7");
            operatingSystem.add("windows/srv2019std", "windows/srv2019std");

            return operatingSystem;
        }

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

        public ListBoxModel doFillLocationItems() {
            ListBoxModel location = new ListBoxModel();

            location.add("PHX", "PHX");
            location.add("ASH", "ASH");
            location.add("SGP", "SGP");
            location.add("NLD", "NLD");

            return location;
        }

        public FormValidation doCheckHostName(@QueryParameter String value) {
            if (value == null || "".equals(value)) {
                return FormValidation.error("Must not be empty!");
            } else {
                return FormValidation.ok();
            }
        }

        public FormValidation doCheckName(@QueryParameter String value) {
            if (value == null || "".equals(value)) {
                return FormValidation.error("Must not be empty!");
            } else {
                return FormValidation.ok();
            }
        }

        public FormValidation doCheckNumExecutors(@QueryParameter String value) throws IOException, ServletException {
            try {
                Integer.parseInt(value);
                return FormValidation.ok();
            } catch (NumberFormatException e) {
                return FormValidation.error("Must be a number");
            }
        }

        public FormValidation doCheckIdleTerminationInMinutes(@QueryParameter String value)
                throws IOException, ServletException {
            try {
                Integer.parseInt(value);
                return FormValidation.ok();
            } catch (NumberFormatException e) {
                return FormValidation.error("Must be a number");
            }
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getSshPort() {
        return sshPort;
    }

    public void setSshPort(String sshPort) {
        this.sshPort = sshPort;
    }

    public String getIdleTerminationInMinutes() {
        return idleTerminationInMinutes;
    }

    public void setIdleTerminationInMinutes(String idleTerminationInMinutes) {
        this.idleTerminationInMinutes = idleTerminationInMinutes;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getOperatingSystem() {
        return operatingSystem;
    }

    public void setOperatingSystem(String operatingSystem) {
        this.operatingSystem = operatingSystem;
    }

    public String getNumExecutors() {
        return numExecutors;
    }

    public void setNumExecutors(String numExecutors) {
        this.numExecutors = numExecutors;
    }

    public String getLabelString() {
        return labelString;
    }

    public void setLabelString(String labelString) {
        this.labelString = labelString;
    }

    public Boolean getLabellessJobsAllowed() {
        return labellessJobsAllowed;
    }

    public void setLabellessJobsAllowed(Boolean labellessJobsAllowed) {
        this.labellessJobsAllowed = labellessJobsAllowed;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    public String getSshCredentialsId() {
        return sshCredentialsId;
    }

    public void setSshCredentialsId(String sshCredentialsId) {
        this.sshCredentialsId = sshCredentialsId;
    }

    public PhoenixNAPCloud getCloud() throws Exception {

        final List<Cloud> clouds = Jenkins.get().clouds;
        // Cloud cloud = clouds.getCloud(name);
        for (Cloud cloud : clouds) {

            if (cloud instanceof PhoenixNAPCloud) {
                List<PhoenixNAPSlaveTemplate> templates = ((PhoenixNAPCloud) cloud).getTemplates();
                for (PhoenixNAPSlaveTemplate phoenixNAPSlaveTemplate : templates) {
                    if (this.equals(phoenixNAPSlaveTemplate)) {
                        return (PhoenixNAPCloud) cloud;
                    }
                }
            }
        }
        throw new Exception("Cloud can not be found for template: " + this.name);

    }

    public ComputerLauncher resolveLauncher() {
        return new PhoenixNAPComputerLauncher();
    }

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
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        PhoenixNAPSlaveTemplate other = (PhoenixNAPSlaveTemplate) obj;
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

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

}
