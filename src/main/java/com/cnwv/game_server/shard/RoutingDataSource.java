package com.cnwv.game_server.shard;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

public class RoutingDataSource extends AbstractRoutingDataSource {
    @Override
    protected Object determineCurrentLookupKey() {
        String key = ShardContext.get();
        return (key == null) ? "s0" : key; // 기본 s0
    }
}
