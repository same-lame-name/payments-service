package dexter.banking.booktransfers.infrastructure.adapter.out.cache.idempotency.redis;

import dexter.banking.booktransfers.core.domain.shared.idempotency.IdempotencyData;
import dexter.banking.booktransfers.infrastructure.FacadeConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@Profile("idempotency-redis")
@ComponentScan(basePackageClasses = RedisIdempotencyConfig.class)
public class RedisIdempotencyConfig implements FacadeConfiguration {

    @Bean
    public RedisTemplate<String, IdempotencyData> idempotencyRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, IdempotencyData> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }
}
