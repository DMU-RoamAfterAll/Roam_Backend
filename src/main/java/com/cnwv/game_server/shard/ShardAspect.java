package com.cnwv.game_server.shard;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.reflect.CodeSignature;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Aspect
@Configuration
@Order(0) // 트랜잭션보다 먼저
public class ShardAspect {

    private final ShardResolver resolver = new ShardResolver(2); // s0, s1

    @Around("@within(com.cnwv.game_server.shard.WithUserShard) || @annotation(com.cnwv.game_server.shard.WithUserShard)")
    public Object routeByUserId(ProceedingJoinPoint pjp) throws Throwable {
        // 1) 메서드에서 먼저 찾고
        MethodSignature ms = (MethodSignature) pjp.getSignature();
        WithUserShard ann = ms.getMethod().getAnnotation(WithUserShard.class);
        // 2) 없으면 클래스에서 찾기
        if (ann == null) {
            ann = pjp.getTarget().getClass().getAnnotation(WithUserShard.class);
        }

        String keyName = (ann != null) ? ann.userIdParam() : "username";

        CodeSignature sig = (CodeSignature) pjp.getSignature();
        String[] names = sig.getParameterNames();
        Object[] args = pjp.getArgs();

        String userId = null;
        for (int i = 0; i < names.length; i++) {
            if (keyName.equals(names[i]) && args[i] != null) {
                userId = String.valueOf(args[i]);
                break;
            }
        }

        if (userId != null) {
            ShardContext.set(resolver.fromUserId(userId));
        } else {
            ShardContext.set("s0"); // fallback
        }

        try {
            return pjp.proceed();
        } finally {
            ShardContext.clear();
        }
    }
}
