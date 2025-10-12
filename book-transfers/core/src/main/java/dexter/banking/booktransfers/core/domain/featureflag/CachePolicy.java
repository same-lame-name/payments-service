package dexter.banking.booktransfers.core.domain.featureflag;

public record CachePolicy(String name, long ttlSeconds, long maxSize) {}
