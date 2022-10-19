package com.pnap.bmc_plugin;

import java.io.IOException;

import org.apache.http.HttpStatus;
import org.jenkinsci.plugins.cloudstats.ProvisioningActivity;
import org.jenkinsci.plugins.cloudstats.ProvisioningActivity.Id;
import org.jenkinsci.plugins.cloudstats.TrackedItem;
import org.kohsuke.stapler.HttpRedirect;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.interceptor.RequirePOST;

import com.pnap.bmc_sdk.dto.ServerResponseDTO;

import hudson.slaves.AbstractCloudComputer;

public class PhoenixNAPComputer extends AbstractCloudComputer<PhoenixNAPAgent> implements TrackedItem {



    /**
     * Creates new instance of PhoenixNAPComputer.
     *
     * @param agent
     */
    public PhoenixNAPComputer(final PhoenixNAPAgent agent) {
        super(agent);
    }

    /**
     * Get unique identifier of the provisioning item.
     *
     * @return The identifier.
     */
    @Override
    public Id getId() {
        PhoenixNAPAgent node = getNode();
        if (node != null) {
            return node.getId();
        }
        return new ProvisioningActivity.Id("");
    }

    /**
     * When the agent is deleted, free the node right away.
     */
    @Override
    @RequirePOST
    public HttpResponse doDoDelete() throws IOException {
        checkPermission(DELETE);
        try {

            PhoenixNAPAgent node = getNode();

            ServerResponseDTO server = null;
            if (node != null) {
                server = node.getProvisionedServer();
            }
            if (server != null && "creating".equals(server.getStatus())) {
                return HttpResponses.error(HttpStatus.SC_INTERNAL_SERVER_ERROR,
                        "Server is in creating state and can not be deleted. Please wait for provisioning to complete.");
            }
            if (node != null) { // No need to terminate nodes again
                node.terminate();
            }
            return new HttpRedirect("..");
        } catch (Exception e) {
            return HttpResponses.error(HttpStatus.SC_INTERNAL_SERVER_ERROR, e);
        }
    }
    /*
     * @Override protected void onRemoved() { super.onRemoved();
     * //getNode().deleteServer(); }
     */
}
