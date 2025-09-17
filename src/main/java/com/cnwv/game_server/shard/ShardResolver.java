package com.cnwv.game_server.shard;

import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;

/** username 기반 해시 샤딩 → "s0", "s1" ... 반환 */
public class ShardResolver {
    private final int shardCount;
    public ShardResolver(int shardCount) { this.shardCount = shardCount; }

    public String fromUserId(String userId) {
        CRC32 crc = new CRC32();
        crc.update(userId.getBytes(StandardCharsets.UTF_8));
        long h = crc.getValue();
        int idx = (int) (Math.abs(h) % shardCount);
        return "s" + idx;
    }
}
