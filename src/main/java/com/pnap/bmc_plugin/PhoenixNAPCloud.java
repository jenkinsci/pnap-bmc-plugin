package com.pnap.bmc_plugin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import org.jenkinsci.plugins.cloudstats.TrackedPlannedNode;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.interceptor.RequirePOST;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Label;
import hudson.model.Descriptor.FormException;
import hudson.slaves.Cloud;
import hudson.slaves.NodeProvisioner;
import hudson.slaves.NodeProvisioner.PlannedNode;
import hudson.util.FormValidation;
import hudson.util.HttpResponses;

/**
 * @author pavlej
 *
 */
public final class PhoenixNAPCloud extends Cloud {

    /**
     *
     */
    private String instanceCap = "0";
    /**
     *
     */
    private String timeoutMinutes = "5";
    /**
     *
     */
    private String connectionRetryWait = "10";
    /**
     * List of the templates for phoenixNAP cloud.
     */
    private List<PhoenixNAPSlaveTemplate> templates = Collections.emptyList();

    /**
     *
     */
    private static final Logger LOGGER = Logger.getLogger(PhoenixNAPCloud.class.getName());

    protected PhoenixNAPCloud(final String name) {
        super(name);
        // TODO Auto-generated constructor stub
    }

    /**
     * @param name
     * @param instanceCap
     * @param timeoutMinutes
     * @param connectionRetryWait
     * @param templates
     */
    @DataBoundConstructor
    public PhoenixNAPCloud(final String name, final String instanceCap, final String timeoutMinutes,
            final String connectionRetryWait, final List<PhoenixNAPSlaveTemplate> templates) {
        super(name);

        LOGGER.log(Level.INFO, "Constructing new PhoenixNAPCloud(name = {0} instanceCap = {1}, ...)",
                new Object[] {name, instanceCap});

        this.instanceCap = instanceCap;

        this.timeoutMinutes = timeoutMinutes;
        this.connectionRetryWait = connectionRetryWait;
        if (templates == null) {
            this.templates = Collections.emptyList();
        } else {
            this.templates = templates;
        }

        LOGGER.info("Creating PhoenixNAPCloud cloud with " + this.templates.size() + " templates");
    }

    /*
     * (non-Javadoc)
     *
     * @see hudson.slaves.Cloud#provision(hudson.slaves.Cloud.CloudState, int)
     */
    @Override
    @SuppressFBWarnings(value = "DLS_DEAD_LOCAL_STORE")
    public Collection<PlannedNode> provision(final CloudState state, final int excessWorkload) {
        try {
            int excessWL = excessWorkload;
            System.out.println("new provisioning started " + state.getLabel());
            List<NodeProvisioner.PlannedNode> provisioningNodes = new ArrayList<>();
            while (excessWL > 0) {
                final PhoenixNAPSlaveTemplate template = getTemplates(state.getLabel()).get(0);

                final PhoenixNAPSlave slave = new PhoenixNAPSlave(template);
                provisioningNodes.add(new TrackedPlannedNode(slave.getId(),
                        Integer.parseInt(template.getNumExecutors()), Computer.threadPoolForRemoting.submit(() -> {
                            slave.provision();
                            return slave;
                        })));
                excessWL -= Integer.parseInt(template.getNumExecutors());
                return provisioningNodes;

            }
            return provisioningNodes;
        } catch (FormException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return Collections.emptyList();

    }

    /**
     * @param template
     * @return HTTP Response
     * @throws Exception
     */
    @RequirePOST
    public HttpResponse doProvision(final @QueryParameter String template) throws Exception {
        System.out.println("new provisioning started " + template);
        final PhoenixNAPSlaveTemplate tplPnap = getTemplate(template);

        PhoenixNAPSlave slave = new PhoenixNAPSlave(tplPnap);
        slave.provision();
        return HttpResponses.redirectViaContextPath("/computer/");
        // return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see hudson.slaves.Cloud#canProvision(hudson.model.Label)
     */
    @Override
    public boolean canProvision(final CloudState state) {
        // NB: this must be lock-less, lest it causes deadlocks. This method may get
        // called from Queue locked
        // call-chains (e.g. Node adding/removing) and since they may not be lock
        // protected they have the potential
        // of deadlocking on our provisionLock.
        // Also we'll treat this like a hint. We *may* be able to provision a node, but
        // we *can* provision that type
        // of label in general. It may later turn out that we can not currently
        // provision though, provision() would
        // then return an empty list of planned nodes, and from what I can tell jenkins
        // core will eventually retry
        // provisioning. It's very much unclear if this is indeed better, a lock timeout
        // may proof more effectively.
        // This also doesn't take into account the actual capacity, as that too gets
        // checked during provision.
        boolean can = !getTemplates(state.getLabel()).isEmpty();
        LOGGER.log(Level.INFO, "canProvision  " + state.getLabel() + " :: " + can);
        return can;
        // return true;
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<hudson.slaves.Cloud> {

        /**
         * Limit for number of instances that can be run.
         */
        private static final int CAPACITY_LIMIT = 5;

        /**
         *
         */
        public DescriptorImpl() {
            load();
        }

        @Override
        public String getDisplayName() {
            return "phoenixNAP";
        }

        /**
         * @param value
         * @return FormValidation
         */
        public FormValidation doCheckName(final @QueryParameter String value) {
            if (value == null || "".equals(value)) {
                return FormValidation.error("Must not be empty!");
            } else {
                return FormValidation.ok();
            }
        }

        /**
         * @param value
         * @return FormValidation
         * @throws IOException
         * @throws ServletException
         */
        public FormValidation doCheckInstanceCap(final @QueryParameter String value)
                throws IOException, ServletException {
            try {
                Integer instanceCap = Integer.parseInt(value);
                if (instanceCap > CAPACITY_LIMIT) {
                    return FormValidation.error("Must not be bigger than 5");
                }
                return FormValidation.ok();
            } catch (NumberFormatException e) {
                return FormValidation.error("Must be a number");
            }
        }

        /**
         * @param value
         * @return FormValidation
         * @throws IOException
         * @throws ServletException
         */
        public FormValidation doCheckTimeoutMinutes(final @QueryParameter String value)
                throws IOException, ServletException {
            try {
                Integer.parseInt(value);
                return FormValidation.ok();
            } catch (NumberFormatException e) {
                return FormValidation.error("Must be a number");
            }
        }

        /**
         * @param value
         * @return FormValidation
         * @throws IOException
         * @throws ServletException
         */
        public FormValidation doCheckConnectionRetryWait(final @QueryParameter String value)
                throws IOException, ServletException {
            try {
                Integer.parseInt(value);
                return FormValidation.ok();
            } catch (NumberFormatException e) {
                return FormValidation.error("Must be a number");
            }
        }
    }

    private List<PhoenixNAPSlaveTemplate> getTemplates(final Label label) {
        List<PhoenixNAPSlaveTemplate> matchingTemplates = new ArrayList<>();

        for (PhoenixNAPSlaveTemplate t : templates) {
            if ((label == null && t.getLabelString().isEmpty()) || (label == null && t.getLabellessJobsAllowed())
            // TODO CHECK THIS
                    || // TODO CHECK THIS
                    (label != null && label.getName().equals(t.getLabelString()))) {
                matchingTemplates.add(t);
            }
        }

        return matchingTemplates;
    }

    private PhoenixNAPSlaveTemplate getTemplate(final String name) throws Exception {
        // List<PhoenixNAPSlaveTemplate> matchingTemplates = new ArrayList<>();

        for (PhoenixNAPSlaveTemplate t : templates) {
            if (t.getName().equals(name)) {
                return t;
            }
        }

        throw new Exception("Template with the name " + name + "can not be found.");
    }

    /**
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * @return instanceCap
     */
    public String getInstanceCap() {
        return instanceCap;
    }

    /**
     * @return timeoutMinutes
     */
    public String getTimeoutMinutes() {
        return timeoutMinutes;
    }

    /**
     * @return connectionRetryWait
     */
    public String getConnectionRetryWait() {
        return connectionRetryWait;
    }

    /**
     * @return templates
     */
    public List<PhoenixNAPSlaveTemplate> getTemplates() {
        return templates;
    }

    /**
     * @param instanceCap
     */
    public void setInstanceCap(final String instanceCap) {
        this.instanceCap = instanceCap;
    }

    /**
     * @param timeoutMinutes
     */
    public void setTimeoutMinutes(final String timeoutMinutes) {
        this.timeoutMinutes = timeoutMinutes;
    }

    /**
     * @param connectionRetryWait
     */
    public void setConnectionRetryWait(final String connectionRetryWait) {
        this.connectionRetryWait = connectionRetryWait;
    }

    /**
     * @param templates
     */
    public void setTemplates(final List<PhoenixNAPSlaveTemplate> templates) {
        this.templates = templates;
    }

}
