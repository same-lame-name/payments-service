package dexter.banking.limit.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mock/regulator")
public class RegulatorMockController {

    @GetMapping(value = "/payees", produces = "application/vnd.api+json")
    public String getPayees() {
        return """
            {
              "data": [
                {
                  "type": "payees",
                  "id": "reg-001",
                  "attributes": {
                    "name": "Regulator Payee One",
                    "iban": "FR1420041010050500013M02606"
                  }
                },
                {
                  "type": "payees",
                  "id": "reg-002",
                  "attributes": {
                    "name": "Regulator Payee Two",
                    "iban": "IT60X0542811101000000123456"
                  }
                }
              ]
            }
            """;
    }
}