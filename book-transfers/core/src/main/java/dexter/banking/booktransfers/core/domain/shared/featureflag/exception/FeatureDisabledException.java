package dexter.banking.booktransfers.core.domain.shared.featureflag.exception;

public class FeatureDisabledException extends RuntimeException {
    public FeatureDisabledException(String journeyName) {
        super("The feature for journey '" + journeyName + "' is currently disabled.");
    }
}
