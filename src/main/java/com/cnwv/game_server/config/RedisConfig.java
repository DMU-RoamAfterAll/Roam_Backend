package com.cnwv.game_server.config;

import com.cnwv.game_server.redis.RedisSubscriber;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@RequiredArgsConstructor
public class RedisConfig {

    private final RedisSubscriber redisSubscriber;

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory factory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);
        container.addMessageListener(messageListenerAdapter(), topic());
        return container;
    }

    @Bean
    public MessageListenerAdapter messageListenerAdapter() {
        return new MessageListenerAdapter(redisSubscriber, "onMessage");
    }

    @Bean
    public ChannelTopic topic() {
        return new ChannelTopic("chat");
    }

    @Bean
    public StringRedisTemplate redisTemplate(RedisConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }
}
