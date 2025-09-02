package dexter.banking.booktransfers.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * The main entry point for the Book Transfers application.
 *
 * @SpringBootApplication enables component scanning and auto-configuration.
 * @EnableFeignClients explicitly activates the scanning for Feign client interfaces.
 */
@EnableFeignClients(basePackages = "dexter.banking.booktransfers")
@SpringBootApplication(scanBasePackages = "dexter.banking.booktransfers")

public class BookTransfersApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookTransfersApplication.class, args);
    }
}

