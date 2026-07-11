package com.tenpo.challenge.infrastructure.adapter.out.external;

import com.tenpo.challenge.domain.exception.ExternalServiceException;
import com.tenpo.challenge.domain.port.out.PercentageProviderPort;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.springboot3.retry.autoconfigure.RetryAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test de integración: levanta el contexto real de Spring con AOP y la
 * config de Resilience4j para verificar que la política de retry "percentageProvider",
 * realmente reintenta y, al agotar los intentos, dispara el fallback.
 */
class PercentageProviderRetryIntegrationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RetryAutoConfiguration.class, AopAutoConfiguration.class))
            .withPropertyValues(
                    "resilience4j.retry.instances.percentageProvider.max-attempts=3",
                    "resilience4j.retry.instances.percentageProvider.wait-duration=10ms",
                    "resilience4j.retry.instances.percentageProvider.enable-exponential-backoff=true",
                    "resilience4j.retry.instances.percentageProvider.exponential-backoff-multiplier=2"
            );

    @Test
    void shouldRecoverAfterTwoTransientFailures() {
        contextRunner.withUserConfiguration(FailTwiceThenSucceedProvider.class).run(context -> {
            PercentageProviderPort provider = context.getBean(PercentageProviderPort.class);

            BigDecimal percentage = provider.getPercentage();

            assertThat(percentage).isEqualByComparingTo(BigDecimal.TEN);
            assertThat(context.getBean(FailTwiceThenSucceedProvider.class).getAttemptCount()).isEqualTo(3);
        });
    }

    @Test
    void shouldThrowExternalServiceExceptionWhenRetriesAreExhausted() {
        contextRunner.withUserConfiguration(AlwaysFailingProvider.class).run(context -> {
            PercentageProviderPort provider = context.getBean(PercentageProviderPort.class);

            assertThatThrownBy(provider::getPercentage)
                    .isInstanceOf(ExternalServiceException.class);
            assertThat(context.getBean(AlwaysFailingProvider.class).getAttemptCount()).isEqualTo(3);
        });
    }

    static class FailTwiceThenSucceedProvider implements PercentageProviderPort {

        private final AtomicInteger attemptCount = new AtomicInteger(0);

        @Override
        @Retry(name = "percentageProvider", fallbackMethod = "fallbackPercentage")
        public BigDecimal getPercentage() {
            int attempt = attemptCount.incrementAndGet();
            if (attempt <= 2) {
                throw new RuntimeException("Simulated external service failure #" + attempt);
            }
            return BigDecimal.TEN;
        }

        private BigDecimal fallbackPercentage(Throwable throwable) {
            throw new ExternalServiceException("Percentage provider failed after exhausting retries", throwable);
        }

        int getAttemptCount() {
            return attemptCount.get();
        }
    }

    static class AlwaysFailingProvider implements PercentageProviderPort {

        private final AtomicInteger attemptCount = new AtomicInteger(0);

        @Override
        @Retry(name = "percentageProvider", fallbackMethod = "fallbackPercentage")
        public BigDecimal getPercentage() {
            attemptCount.incrementAndGet();
            throw new RuntimeException("Simulated external service failure");
        }

        private BigDecimal fallbackPercentage(Throwable throwable) {
            throw new ExternalServiceException("Percentage provider failed after exhausting retries", throwable);
        }

        int getAttemptCount() {
            return attemptCount.get();
        }
    }

}
