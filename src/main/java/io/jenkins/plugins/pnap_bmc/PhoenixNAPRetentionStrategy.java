package io.jenkins.plugins.pnap_bmc;

import hudson.slaves.CloudRetentionStrategy;

/**
 *
 * The {@link PhoenixNAPRetentionStrategy} is mainly used to determine when an
 * idle server can be destroyed.
 *
 *
 */
public class PhoenixNAPRetentionStrategy extends CloudRetentionStrategy {

    /**
     * @param idleMinutes - defines inactivity time in minutes after which node will
     *                    be automatically deleted.
     */
    public PhoenixNAPRetentionStrategy(final int idleMinutes) {
        super(idleMinutes);
    }
}
