package dexter.banking.booktransfers.app;

import dexter.banking.booktransfers.infrastructure.FacadeConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScans;
import org.springframework.context.annotation.FilterType;

/**
 * The main entry point for the Book Transfers application.
 *
 * @SpringBootApplication enables auto-configuration but its default component scan is disabled.
 * We use @ComponentScans to define two precise rules:
 * 1. A standard scan for all components within the 'core' and 'app' modules.
 * 2. A custom, filtered scan for the 'infrastructure' module that ONLY discovers beans
 * that implement the FacadeConfiguration marker interface. This is enforced by
 * useDefaultFilters = false.
 */
@SpringBootApplication
@ComponentScans({
    @ComponentScan(basePackages = {"dexter.banking.booktransfers.core", "dexter.banking.booktransfers.app"}),
    @ComponentScan(
        basePackages = "dexter.banking.booktransfers.infrastructure",
        includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = FacadeConfiguration.class),
        useDefaultFilters = false
    )
})
public class BookTransfersApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookTransfersApplication.class, args);
    }
}
