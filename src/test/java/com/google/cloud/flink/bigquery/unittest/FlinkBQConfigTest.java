/*
 * Copyright 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.cloud.flink.bigquery.unittest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.cloud.bigquery.TableId;
import com.google.cloud.bigquery.storage.v1.ArrowSerializationOptions.CompressionCodec;
import com.google.cloud.flink.bigquery.BigQueryDynamicTableFactory;
import com.google.cloud.flink.bigquery.FlinkBigQueryConfig;
import com.google.common.collect.ImmutableMap;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.*;
import net.sf.jsqlparser.JSQLParserException;
import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.ConfigOptions;
import org.apache.flink.configuration.ReadableConfig;
import org.apache.hadoop.conf.Configuration;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class FlinkBQConfigTest {
  public static final int DEFAULT_PARALLELISM = 10;
  public static final String FLINK_VERSION = "1.11.0";
  ImmutableMap<String, String> defaultOptions = ImmutableMap.of("table", "dataset.table");

  @Test
  public void testSerializability() throws IOException {
    Configuration hadoopConfiguration = new Configuration();
    org.apache.flink.configuration.Configuration options =
        new org.apache.flink.configuration.Configuration();
    ConfigOption<String> table = ConfigOptions.key("table").stringType().noDefaultValue();
    ConfigOption<String> selectedFields =
        ConfigOptions.key("selectedFields").stringType().noDefaultValue();
    options.set(table, "bigquery-public-data.samples.shakespeare");
    options.set(selectedFields, "word,word_count");

    BigQueryDynamicTableFactory factory = new BigQueryDynamicTableFactory();
    new ObjectOutputStream(new ByteArrayOutputStream())
        .writeObject(
            FlinkBigQueryConfig.from(
                factory.requiredOptions(),
                factory.optionalOptions(),
                (ReadableConfig) options,
                defaultOptions,
                hadoopConfiguration,
                DEFAULT_PARALLELISM,
                new org.apache.flink.configuration.Configuration(),
                FLINK_VERSION,
                Optional.empty()));
  }

  @Test
  public void testDefaults() throws IOException, JSQLParserException {
    Configuration hadoopConfiguration = new Configuration();
    org.apache.flink.configuration.Configuration options =
        new org.apache.flink.configuration.Configuration();
    ConfigOption<String> table = ConfigOptions.key("table").stringType().noDefaultValue();
    ConfigOption<String> selectedFields =
        ConfigOptions.key("selectedFields").stringType().noDefaultValue();
    options.set(table, "bigquery-public-data.samples.shakespeare");
    options.set(selectedFields, "word,word_count");

    BigQueryDynamicTableFactory factory = new BigQueryDynamicTableFactory();
    FlinkBigQueryConfig config =
        FlinkBigQueryConfig.from(
            factory.requiredOptions(),
            factory.optionalOptions(),
            (ReadableConfig) options,
            defaultOptions,
            hadoopConfiguration,
            DEFAULT_PARALLELISM,
            new org.apache.flink.configuration.Configuration(),
            FLINK_VERSION,
            Optional.empty());

    assertEquals(config.getTableId(), TableId.of("bigquery-public-data", "samples", "shakespeare"));
    assertEquals(config.getFilter(), Optional.empty());
    assertEquals(config.getSchema(), Optional.empty());
    assertEquals(config.getMaxParallelism(), OptionalInt.empty());
    assertEquals(config.getQuery(), Optional.empty());
    assertEquals(config.getPartitionField(), Optional.empty());
    assertEquals(config.getPartitionType(), Optional.empty());
    assertEquals(config.getPartitionExpirationMs(), OptionalLong.empty());
    assertEquals(config.getPartitionRequireFilter(), Optional.empty());
    assertEquals(config.getDefaultParallelism(), 10);
    assertEquals(config.getCredentialsFile(), Optional.empty());
    assertEquals(config.getAccessToken(), Optional.empty());
    assertEquals(config.getCredentialsKey(), Optional.empty());
    assertEquals(config.getArrowCompressionCodec(), CompressionCodec.COMPRESSION_UNSPECIFIED);
    assertEquals(config.getClusteredFields(), Optional.empty());
    assertEquals(config.getMaterializationDataset(), Optional.empty());
    assertEquals(config.getMaterializationExpirationTimeInMinutes(), 1440);
    assertEquals(config.getMaterializationProject(), Optional.empty());
    assertEquals(config.getReadDataFormat(), com.google.cloud.bigquery.storage.v1.DataFormat.ARROW);
    assertEquals(config.getSelectedFields(), "word,word_count");
    assertEquals(config.getEndpoint(), Optional.empty());
  }

  @Test
  public void testConfigFromOptions() throws JSQLParserException {
    Configuration hadoopConfiguration = new Configuration();
    org.apache.flink.configuration.Configuration options =
        new org.apache.flink.configuration.Configuration();
    ConfigOption<String> table = ConfigOptions.key("table").stringType().noDefaultValue();
    ConfigOption<String> query = ConfigOptions.key("query").stringType().noDefaultValue();
    ConfigOption<String> filter = ConfigOptions.key("filter").stringType().defaultValue("");
    ConfigOption<String> partition_field =
        ConfigOptions.key("partitionField").stringType().defaultValue("");
    ConfigOption<String> partition_type =
        ConfigOptions.key("partitionType").stringType().defaultValue("");
    ConfigOption<String> partition_expiration_ms =
        ConfigOptions.key("partitionExpirationMs").stringType().defaultValue("");
    ConfigOption<String> partition_require_filter =
        ConfigOptions.key("partitionRequireFilter").stringType().defaultValue("");
    ConfigOption<String> flink_version =
        ConfigOptions.key("flinkVersion").stringType().defaultValue("1.11");
    ConfigOption<Integer> max_parallelism =
        ConfigOptions.key("maxParallelism").intType().defaultValue(1);
    ConfigOption<String> selected_fields =
        ConfigOptions.key("selectedFields").stringType().noDefaultValue();
    ConfigOption<String> materialization_project =
        ConfigOptions.key("materializationProject").stringType().noDefaultValue();
    ConfigOption<String> materialization_dataset =
        ConfigOptions.key("materializationDataset").stringType().noDefaultValue();
    options.set(table, "bigquery-public-data.samples.shakespeare");
    options.set(query, "select word,word_count from table");
    options.set(filter, "word_count>100");
    options.set(partition_field, "a");
    options.set(partition_type, "DAY");
    options.set(partition_expiration_ms, "999");
    options.set(partition_require_filter, "true");
    options.set(flink_version, "1.11.0");
    options.set(max_parallelism, 99);
    options.set(selected_fields, "word,word_count");
    options.set(materialization_project, "vmp");
    options.set(materialization_dataset, "vmd");

    BigQueryDynamicTableFactory factory = new BigQueryDynamicTableFactory();
    FlinkBigQueryConfig config =
        FlinkBigQueryConfig.from(
            factory.requiredOptions(),
            factory.optionalOptions(),
            (ReadableConfig) options,
            defaultOptions,
            hadoopConfiguration,
            DEFAULT_PARALLELISM,
            new org.apache.flink.configuration.Configuration(),
            FLINK_VERSION,
            Optional.empty());

    assertEquals(config.getTableId(), TableId.of("bigquery-public-data", "samples", "shakespeare"));
    assertEquals(config.getFilter(), Optional.ofNullable("word_count>100"));
    assertEquals(config.getMaxParallelism(), OptionalInt.of(99));
    assertEquals(config.getQuery(), Optional.empty());
    assertEquals(config.getPartitionField(), Optional.ofNullable("a"));
    // assertEquals(config.getPartitionType(), Optional.of("DAY"));
    assertEquals(config.getPartitionExpirationMs(), OptionalLong.of(999));
    assertEquals(config.getPartitionRequireFilter(), Optional.of(true));
    assertEquals(config.getDefaultParallelism(), 10);
    assertEquals(config.getMaterializationDataset(), Optional.ofNullable("vmd"));
    assertEquals(config.getMaterializationExpirationTimeInMinutes(), 1440);
    assertEquals(config.getMaterializationProject(), Optional.ofNullable("vmp"));
    assertEquals(config.getSelectedFields(), "word,word_count");
  }

  @Ignore
  @Test
  public void testInvalidCompressionCodec() {
    Configuration hadoopConfiguration = new Configuration();
    org.apache.flink.configuration.Configuration options =
        new org.apache.flink.configuration.Configuration();
    ConfigOption<String> table = ConfigOptions.key("table").stringType().noDefaultValue();
    ConfigOption<String> selectedFields =
        ConfigOptions.key("selectedFields").stringType().noDefaultValue();
    ConfigOption<String> arrowCompressionCodec =
        ConfigOptions.key("arrowCompressionCodec").stringType().noDefaultValue();
    options.set(table, "bigquery-public-data.samples.shakespeare");
    options.set(selectedFields, "word,word_count");
    options.set(arrowCompressionCodec, "randomCompression");

    BigQueryDynamicTableFactory factory = new BigQueryDynamicTableFactory();
    IllegalArgumentException exception =
        Assert.assertThrows(
            IllegalArgumentException.class,
            () ->
                FlinkBigQueryConfig.from(
                    factory.requiredOptions(),
                    factory.optionalOptions(),
                    (ReadableConfig) options,
                    defaultOptions,
                    hadoopConfiguration,
                    DEFAULT_PARALLELISM,
                    new org.apache.flink.configuration.Configuration(),
                    FLINK_VERSION,
                    Optional.empty()));
    assertTrue(
        exception
            .toString()
            .contains(
                "Compression codec 'RANDOMCOMPRESSION' for Arrow is not supported."
                    + " Supported formats are "
                    + Arrays.toString(CompressionCodec.values())));
  }
}