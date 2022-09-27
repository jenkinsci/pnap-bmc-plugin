package com.pnap.bmc_plugin;

import hudson.slaves.CloudRetentionStrategy;

/**
 *
 * The {@link PhoenixNAPRetentionStrategy} is mainly used to determine when an idle server
 * can be destroyed.
 *
 * 
 */
public class PhoenixNAPRetentionStrategy extends CloudRetentionStrategy {
/*
	
	private static class DescriptorImpl extends Descriptor<hudson.slaves.RetentionStrategy<?>> {
		@Nonnull
		@Override
		public String getDisplayName() {
			return "phoenixNAP";
		}
	}

	public void start(PhoenixNAPComputer pnapComputer) {
		pnapComputer.connect(false);
	}

	@Override
	protected long checkCycle() {
		return 1; // ask Jenkins to check every 1 minute, though it might decide to check in 2 or
					// 3 (or longer?)
	}

	@Override
	protected boolean isIdleForTooLong(PhoenixNAPComputer pnapComputer) {
		PhoenixNAPSlave node = pnapComputer.getNode();

		if (node == null) {
			return false;
		}

		int idleTerminationTime = 0;
		try {
			idleTerminationTime = Integer.parseInt(node.getTemplate().getIdleTerminationInMinutes());
		} catch (NumberFormatException e) {

			e.printStackTrace();
		}

		if (idleTerminationTime > 0) {
			return System.currentTimeMillis() - pnapComputer.getIdleStartMilliseconds() > TimeUnit.MINUTES
					.toMillis(idleTerminationTime);
		}

		return false;
	}*/
	 public PhoenixNAPRetentionStrategy(int idleMinutes) {
	        super(idleMinutes);
	    }
}
