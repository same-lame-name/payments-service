package dexter.banking.limit.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MarketAwareForwardedHeaderFilter extends OncePerRequestFilter {

    private static final String MARKET_HEADER = "X-Market";
    private static final String HOST_TEMPLATE = "api.bank.%s.com";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String market = request.getHeader(MARKET_HEADER);

        if (market != null && !market.isBlank()) {
            String publicHost = String.format(HOST_TEMPLATE, market.toLowerCase());

            HttpServletRequest wrappedRequest = new HttpServletRequestWrapper(request) {
                @Override
                public String getServerName() {
                    return publicHost;
                }

                @Override
                public String getScheme() {
                    return "https";
                }

                @Override
                public int getServerPort() {
                    return 443;
                }
                
                // If you need to override context path, you can do it here too:
                // @Override
                // public String getContextPath() {
                //     return "/payments"; 
                // }
            };
            
            filterChain.doFilter(wrappedRequest, response);
        } else {
            filterChain.doFilter(request, response);
        }
    }
}