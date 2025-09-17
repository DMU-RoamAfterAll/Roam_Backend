package com.cnwv.game_server.shard;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface WithUserShard {
    String userIdParam() default "username";
}
