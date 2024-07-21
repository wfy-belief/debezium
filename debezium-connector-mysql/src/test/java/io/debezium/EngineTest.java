package io.debezium;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.debezium.connector.binlog.BinlogConnectorConfig;
import io.debezium.connector.mysql.MySqlConnectorConfig;
import io.debezium.storage.kafka.history.KafkaSchemaHistory;
import org.apache.kafka.connect.runtime.distributed.DistributedConfig;
import org.apache.kafka.connect.storage.KafkaOffsetBackingStore;
import org.apache.kafka.connect.storage.MemoryOffsetBackingStore;
import org.junit.Test;

import io.debezium.connector.mysql.MySqlConnector;
import io.debezium.embedded.EmbeddedEngineConfig;
import io.debezium.embedded.async.AsyncEngineConfig;
import io.debezium.embedded.async.ConvertingAsyncEngineBuilderFactory;
import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.format.Json;
import io.debezium.engine.format.KeyValueChangeEventFormat;

import lombok.Cleanup;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@code @author:} wfy <br/>
 * {@code @date:} 2024/7/18
 **/
public class EngineTest {
    protected static final KeyValueChangeEventFormat<Json, Json> KV_EVENT_FORMAT = KeyValueChangeEventFormat.of(Json.class, Json.class);
    private static final Logger log = LoggerFactory.getLogger(EngineTest.class);

    @Test
    @SneakyThrows
    public void t1() {
        final Properties properties = new Properties();


        // MySQL CONNECTOR
        properties.put(EmbeddedEngineConfig.ENGINE_NAME.name(), "MySQL-CONNECTOR");
        properties.put(EmbeddedEngineConfig.CONNECTOR_CLASS.name(), MySqlConnector.class.getName());
        properties.put(MySqlConnectorConfig.HOSTNAME.name(), "mysql.acitrus.cn");
        properties.put(MySqlConnectorConfig.PORT.name(), "25664");
        properties.put(MySqlConnectorConfig.USER.name(), "reader");
        properties.put(MySqlConnectorConfig.PASSWORD.name(), "ww3eU7Y:3_WhLGf");
        properties.put(MySqlConnectorConfig.TOPIC_PREFIX.name(), "default");
        properties.put(MySqlConnectorConfig.SERVER_ID.name(), "1");
        properties.put(MySqlConnectorConfig.SNAPSHOT_MODE.name(), BinlogConnectorConfig.SnapshotMode.NO_DATA);

        // KAFKA SCHEMA HISTORY
        properties.put(KafkaSchemaHistory.TOPIC.name(), "schema_history");
        properties.put(KafkaSchemaHistory.BOOTSTRAP_SERVERS.name(), "kafka-service.default:9092");

        // 1.KAFKA OFFSET STORAGE | 如果想持久化调试用这个
        properties.put(AsyncEngineConfig.OFFSET_STORAGE.name(), KafkaOffsetBackingStore.class.getName());
        properties.put(DistributedConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka-service.default:9092");
        properties.put(DistributedConfig.OFFSET_STORAGE_TOPIC_CONFIG, "offset_storage_topic");
        properties.put(AsyncEngineConfig.OFFSET_STORAGE_KAFKA_TOPIC.name(), "offset_storage_topic");
        properties.put(DistributedConfig.OFFSET_STORAGE_PARTITIONS_CONFIG, "3");
        properties.put(DistributedConfig.OFFSET_STORAGE_REPLICATION_FACTOR_CONFIG, "1");

        // 2.KAFKA OFFSET MEMORY STORAGE | 如果想用内存调试用这个
//        properties.put(AsyncEngineConfig.OFFSET_STORAGE.name(), MemoryOffsetBackingStore.class.getName());

        ConvertingAsyncEngineBuilderFactory factory = new ConvertingAsyncEngineBuilderFactory();
        @Cleanup
        DebeziumEngine<ChangeEvent<String, String>> debeziumEngine = factory.builder(KV_EVENT_FORMAT)
                .using(properties)
                .notifying((records, committer) -> {
                    for (ChangeEvent<String, String> record : records) {
                        log.info("监听到记录: {}", record);
                        committer.markProcessed(record);
                    }
                    committer.markBatchFinished();
                })
                // .using(this.getClass().getClassLoader())
                .build();
        debeziumEngine.run();
    }

    @Test
    public void t2() {
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        List<CompletableFuture<Integer>> completableFutures = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            int finalI = i;
            CompletableFuture<Integer> future = CompletableFuture
                    .supplyAsync(() -> finalI, executorService)
                    .thenApply(item -> {
                        System.out.println(item);
                        return 1;
                    });
            completableFutures.add(future);
        }
        CompletableFuture.allOf(completableFutures.toArray(new CompletableFuture[]{})).join();
    }
}
