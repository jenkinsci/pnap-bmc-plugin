package com.pnap.bmc_plugin.exception;

public class CloudNotFoundException extends RuntimeException {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     *
     */
    public CloudNotFoundException() {
    }

    /**
     * @param message
     */
    public CloudNotFoundException(final String message) {
        super(message);
    }

    /**
     * @param message
     * @param cause
     */
    public CloudNotFoundException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * @param cause
     */
    public CloudNotFoundException(final Throwable cause) {
        super(cause);
    }

    /**
     * @param message
     * @param cause
     * @param enableSuppression
     * @param writableStackTrace
     */
    public CloudNotFoundException(final String message, final Throwable cause, final boolean enableSuppression,
            final boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
