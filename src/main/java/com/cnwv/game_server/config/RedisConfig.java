package com.cnwv.game_server.config;

import com.cnwv.game_server.redis.RedisSubscriber;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Configuration
@RequiredArgsConstructor
public class RedisConfig {

    private final RedisSubscriber redisSubscriber;

    /**
     * Redis 메시지 수신 처리용 Listener 등록
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory factory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);
        container.addMessageListener(messageListenerAdapter(), topic()); // 전역 채널
        return container;
    }

    /**
     * RedisSubscriber 클래스의 onMessage 메서드에 메시지 위임
     */
    @Bean
    public MessageListenerAdapter messageListenerAdapter() {
        return new MessageListenerAdapter(redisSubscriber, "onMessage");
    }

    /**
     * 전역 채널 Topic 이름 설정
     */
    @Bean
    public ChannelTopic topic() {
        return new ChannelTopic("chat");
    }

    /**
     * RedisTemplate 설정 (String 기반)
     */
    @Bean
    public StringRedisTemplate redisTemplate(RedisConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }
}
