package com.cnwv.game_server.shard;

public final class ShardContext {
    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();
    private ShardContext() {}
    public static void set(String key) { CURRENT.set(key); }
    public static String get() { return CURRENT.get(); }
    public static void clear() { CURRENT.remove(); }
}
