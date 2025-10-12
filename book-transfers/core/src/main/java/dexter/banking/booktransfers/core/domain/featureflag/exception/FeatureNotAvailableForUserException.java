package dexter.banking.booktransfers.core.domain.featureflag.exception;

public class FeatureNotAvailableForUserException extends RuntimeException {
    public FeatureNotAvailableForUserException(String journeyName, String userId) {
        super("The feature for journey '" + journeyName + "' is not available for user '" + userId + "'.");
    }
}
