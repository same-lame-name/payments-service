package dexter.banking.booktransfers.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The main entry point for the Book Transfers application.
 *
 * @SpringBootApplication enables component scanning and auto-configuration.
 */
@SpringBootApplication(scanBasePackages = "dexter.banking.booktransfers")
public class BookTransfersApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookTransfersApplication.class, args);
    }
}

