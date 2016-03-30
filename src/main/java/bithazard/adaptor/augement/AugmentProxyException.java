package bithazard.adaptor.augement;

/**
 * Exception that indicates that there is a general problem.
 */
public class AugmentProxyException extends RuntimeException {
    /**
     * Create a new AugmentProxyException with a message.
     * @param message    The message of the exception.
     */
    public AugmentProxyException(final String message) {
        super(message);
    }

    /**
     * Create a new AugmentProxyException with a message and a cause.
     * @param message    The message of the exception.
     * @param cause      The cause of the exception.
     */
    public AugmentProxyException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Create a new AugmentProxyException with a cause.
     * @param cause    The cause of the exception.
     */
    public AugmentProxyException(final Throwable cause) {
        super(cause);
    }
}
