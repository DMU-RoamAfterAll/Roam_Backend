package com.cnwv.game_server.redis;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class RedisSubscriber implements MessageListener {

    private static final Logger logger = LoggerFactory.getLogger(RedisSubscriber.class);
    private static Consumer<String> messageHandler;

    public static void setMessageHandler(Consumer<String> handler) {
        messageHandler = handler;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String msg = message.toString();
        logger.info("[RedisSubscriber] Redis 메시지 수신: {}", msg);

        if (messageHandler != null) {
            messageHandler.accept(msg);
        } else {
            logger.warn("[RedisSubscriber] 메시지 핸들러가 설정되지 않았습니다.");
        }
    }
}