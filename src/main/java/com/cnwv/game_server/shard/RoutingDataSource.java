package com.cnwv.game_server.shard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

public class RoutingDataSource extends AbstractRoutingDataSource {
    private static final Logger log = LoggerFactory.getLogger(RoutingDataSource.class);

    @Override
    protected Object determineCurrentLookupKey() {
        String key = ShardContext.get();
        String resolved = (key == null) ? "s0" : key; // 기본 s0
        if (log.isDebugEnabled()) {
            log.debug("[RoutingDataSource] resolved shard = {}", resolved);
        }
        return resolved;
    }
}
