package com.tenpo.challenge.domain.exception;

public class RateLimitExceededException extends RuntimeException {

    public RateLimitExceededException(int requestsPerMinute, long retryAfterSeconds) {
        super("Rate limit exceeded: maximum " + requestsPerMinute + " requests per minute allowed. "
                + "Retry after " + retryAfterSeconds + " seconds.");
    }

}
