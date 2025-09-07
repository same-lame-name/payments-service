package dexter.banking.booktransfers.infrastructure.adapter.out.http.payment.feign;

import dexter.banking.booktransfers.infrastructure.FacadeConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackageClasses = HttpOutboundAdapterConfig.class)
@ComponentScan(basePackageClasses = HttpOutboundAdapterConfig.class)
public class HttpOutboundAdapterConfig implements FacadeConfiguration {
}
