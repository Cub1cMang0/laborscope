package com.laborscope.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

@Configuration
public class RedisConfig {

    // Extract redis host from application.yml
    @Value ("${spring.data.redis.host}")
    private String redisHost;
    
    // Extract redis port form application.yml
    @Value ("${spring.data.redis.port}")
    private int redisPort;

    // Extract max active from application.yml
    @Value ("${spring.data.redis.lettuce.pool.max-active}")
    private int maxActive;

    // Extract max idle from application.yml
    @Value ("${spring.data.redis.lettuce.pool.max-idle}")
    private int maxIdle;

    // Extract min idle from application.yml
    @Value ("${spring.data.redis.lettuce.pool.min-idle}")
    private int minIdle;

    // Initialize GenericObkectPoolConfig utilizing previously defined @Values
    @Bean
    public GenericObjectPoolConfig<?> genericObjectPoolConfig() {
        GenericObjectPoolConfig<?> config = new GenericObjectPoolConfig<>();
        config.setMaxTotal(maxActive);
        config.setMaxIdle(maxIdle);
        config.setMinIdle(minIdle);
        return config;
    }

    // Create LettuceConnectionFactory using redis host + port and GenericObjectPoolConfig to manage connections
    @Bean
    public LettuceConnectionFactory redisConnectionFactory(GenericObjectPoolConfig<?> genericObjectPoolConfig) {
        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration(redisHost, redisPort);
        LettuceClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()
        .poolConfig(genericObjectPoolConfig).build();
        LettuceConnectionFactory lettuceConnectionFactory = new LettuceConnectionFactory(redisStandaloneConfiguration, clientConfig);
        lettuceConnectionFactory.setShareNativeConnection(false);
        return lettuceConnectionFactory;
    }

    // Create Redis template from key + value serializers and lettuceconnectionfactory for simple connection management 
    @Bean
    public RedisTemplate<String, Object> redisCacheTemplate(LettuceConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setConnectionFactory(redisConnectionFactory);
        template.afterPropertiesSet();
        return template;
    }
}
