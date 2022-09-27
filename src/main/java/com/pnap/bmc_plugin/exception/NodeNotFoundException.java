package com.pnap.bmc_plugin.exception;

public class NodeNotFoundException extends Exception {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     *
     */
    public NodeNotFoundException() {
    }

    /**
     * @param message
     */
    public NodeNotFoundException(final String message) {
        super(message);
    }

    /**
     * @param message
     * @param cause
     */
    public NodeNotFoundException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * @param cause
     */
    public NodeNotFoundException(final Throwable cause) {
        super(cause);
    }

    /**
     * @param message
     * @param cause
     * @param enableSuppression
     * @param writableStackTrace
     */
    public NodeNotFoundException(final String message, final Throwable cause, final boolean enableSuppression,
            final boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
