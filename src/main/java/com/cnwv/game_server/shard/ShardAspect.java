package com.cnwv.game_server.shard;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.reflect.CodeSignature;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Aspect
@Configuration
@Order(0) // 트랜잭션보다 먼저
@Slf4j
public class ShardAspect {

    private final ShardResolver resolver = new ShardResolver(2); // s0, s1

    @Around("@within(com.cnwv.game_server.shard.WithUserShard) || @annotation(com.cnwv.game_server.shard.WithUserShard)")
    public Object routeByUserId(ProceedingJoinPoint pjp) throws Throwable {
        // 0) Jwt 필터가 이미 넣어놓은 shard가 있나?
        String preResolved = ShardContext.get();

        // 애노테이션에서 userIdParam 이름 얻기 (없으면 "username")
        MethodSignature ms = (MethodSignature) pjp.getSignature();
        WithUserShard ann = ms.getMethod().getAnnotation(WithUserShard.class);
        if (ann == null) {
            ann = pjp.getTarget().getClass().getAnnotation(WithUserShard.class);
        }
        String keyName = (ann != null) ? ann.userIdParam() : "username";

        // 메서드 파라미터에서 해당 이름의 값을 찾아본다 (로그용/보조용)
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

        boolean setByAspect = false;

        if (preResolved == null) {
            // 필터가 shard를 안 정해준 경우에만 여기서 계산
            String shardKey = (userId != null) ? resolver.fromUserId(userId) : "s0";
            ShardContext.set(shardKey);
            MDC.put("shard", shardKey);
            if (userId != null) MDC.put("userId", userId);
            setByAspect = true;

            if (log.isDebugEnabled()) {
                log.debug("[Shard] resolved shard={} (userIdParam='{}', value={})", shardKey, keyName, userId);
            }
        } else {
            // 필터가 정한 shard를 그대로 사용
            MDC.put("shard", preResolved);
            if (userId != null) MDC.put("userId", userId);
            if (log.isDebugEnabled()) {
                log.debug("[Shard] using pre-resolved shard={} (from Jwt filter), method userIdParam='{}', value={}",
                        preResolved, keyName, userId);
            }
        }

        try {
            return pjp.proceed();
        } finally {
            if (setByAspect) {
                ShardContext.clear();
            }
            MDC.remove("shard");
            MDC.remove("userId");
        }
    }
}
