package bithazard.adaptor.augement.config;

/**
 * Exception that indicates that there is a problem with the augment configuration.
 */
public class AugmentConfigException extends RuntimeException {
    /**
     * Create a new AugmentConfigException with a message.
     * @param message    The message of the exception.
     */
    public AugmentConfigException(final String message) {
        super(message);
    }

    /**
     * Create a new AugmentConfigException with a message and a cause.
     * @param message    The message of the exception.
     * @param cause      The cause of the exception.
     */
    public AugmentConfigException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Create a new AugmentConfigException with a cause.
     * @param cause    The cause of the exception.
     */
    public AugmentConfigException(final Throwable cause) {
        super(cause);
    }
}
