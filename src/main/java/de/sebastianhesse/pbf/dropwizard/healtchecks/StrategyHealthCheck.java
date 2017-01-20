package de.sebastianhesse.pbf.dropwizard.healtchecks;

import com.codahale.metrics.health.HealthCheck;
import de.sebastianhesse.pbf.dropwizard.DropwizardConfiguration;
import de.sebastianhesse.pbf.dropwizard.DropwizardConfiguration.ReaderStrategy;


/**
 * Checks that a {@link ReaderStrategy} was set on startup.
 */
public class StrategyHealthCheck extends HealthCheck {

    private DropwizardConfiguration configuration;


    public StrategyHealthCheck(DropwizardConfiguration configuration) {
        this.configuration = configuration;
    }


    @Override
    protected Result check() throws Exception {
        if (configuration != null && configuration.getReaderStrategy() != null) {
            boolean isSimple = ReaderStrategy.SIMPLE.equals(configuration.getReaderStrategy());
            boolean isOptimized = ReaderStrategy.OPTIMIZED.equals(configuration.getReaderStrategy());
            if (isSimple || isOptimized) {
                return Result.healthy();
            }
        }
        return Result.unhealthy("Either configuration is null or a wrong reader strategy is configured.");
    }
}
