package de.sebastianhesse.pbf.dropwizard;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;


/**
 * Configuration class for {@link DropwizardApplication}. Currently only supports setting the {@link ReaderStrategy}.
 */
public class DropwizardConfiguration extends Configuration {

    private ReaderStrategy readerStrategy = ReaderStrategy.SIMPLE;

    @JsonProperty
    public ReaderStrategy getReaderStrategy() {
        return readerStrategy;
    }


    @JsonProperty
    public void setReaderStrategy(String readerStrategy) {
        this.readerStrategy = ReaderStrategy.valueOf(readerStrategy);
    }

    @JsonProperty
    public void setReaderStrategy(ReaderStrategy readerStrategy) {
        this.readerStrategy = readerStrategy;
    }


    public enum ReaderStrategy {
        SIMPLE, OPTIMIZED
    }
}
