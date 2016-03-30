package bithazard.adaptor.augement.proxy;

/**
 * Exception that indicates that there is a problem with the proxy for an adaptor.
 */
public class AdaptorProxyException extends RuntimeException {
    /**
     * Create a new AdaptorProxyException with a message.
     * @param message    The message of the exception.
     */
    public AdaptorProxyException(final String message) {
        super(message);
    }

    /**
     * Create a new AdaptorProxyException with a message and a cause.
     * @param message    The message of the exception.
     * @param cause      The cause of the exception.
     */
    public AdaptorProxyException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Create a new AdaptorProxyException with a cause.
     * @param cause    The cause of the exception.
     */
    public AdaptorProxyException(final Throwable cause) {
        super(cause);
    }
}
