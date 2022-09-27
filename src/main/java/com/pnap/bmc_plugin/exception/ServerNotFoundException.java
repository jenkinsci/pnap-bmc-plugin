package com.pnap.bmc_plugin.exception;

public class ServerNotFoundException extends Exception {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     *
     */
    public ServerNotFoundException() {
    }

    /**
     * @param message
     */
    public ServerNotFoundException(final String message) {
        super(message);
    }

    /**
     * @param message
     * @param cause
     */
    public ServerNotFoundException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * @param cause
     */
    public ServerNotFoundException(final Throwable cause) {
        super(cause);
    }

    /**
     * @param message
     * @param cause
     * @param enableSuppression
     * @param writableStackTrace
     */
    public ServerNotFoundException(final String message, final Throwable cause, final boolean enableSuppression,
            final boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
