package dexter.banking.booktransfers;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * The main entry point for the Book Transfers application.
 *
 * @SpringBootApplication enables component scanning and auto-configuration.
 * @EnableFeignClients explicitly activates the scanning for Feign client interfaces.
 */
@EnableFeignClients
@SpringBootApplication
public class BookTransfersApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookTransfersApplication.class, args);
    }
}

