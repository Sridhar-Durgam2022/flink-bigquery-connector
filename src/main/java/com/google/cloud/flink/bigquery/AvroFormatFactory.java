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
package com.google.cloud.flink.bigquery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.ReadableConfig;
import org.apache.flink.table.connector.format.DecodingFormat;
import org.apache.flink.table.connector.format.EncodingFormat;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.factories.DeserializationFormatFactory;
import org.apache.flink.table.factories.DynamicTableFactory.Context;
import org.apache.flink.table.factories.FactoryUtil;
import org.apache.flink.table.factories.SerializationFormatFactory;

/** Create decoding format for avro format */
public class AvroFormatFactory implements DeserializationFormatFactory, SerializationFormatFactory {
  private static final String IDENTIFIER = "avro";
  private String selectedFields = null;

  @Override
  public DecodingFormat<DeserializationSchema<RowData>> createDecodingFormat(
      Context context, ReadableConfig formatOptions) {
    Map<String, String> options = context.getCatalogTable().getOptions();
    FactoryUtil.validateFactoryOptions(this, formatOptions);
    this.selectedFields = options.get("selectedFields");
    if (this.selectedFields.endsWith(",")) {
      this.selectedFields =
          selectedFields.substring(0, selectedFields.length() - 1).replace("selectedFields=", "");
    }
    List<String> selectedFieldList = new ArrayList<String>();
    List<String> avroFieldList = new ArrayList<String>();
    avroFieldList = Arrays.asList(options.get("avroFields").split(","));
    String avroSchema = options.get("avroSchema");
    if (selectedFields != null) {
      selectedFieldList = Arrays.asList(selectedFields.split(","));
    }
    return new BigQueryAvroFormat(selectedFieldList, avroFieldList, avroSchema);
  }

  @Override
  public String factoryIdentifier() {
    return IDENTIFIER;
  }

  @Override
  public EncodingFormat<SerializationSchema<RowData>> createEncodingFormat(
      Context context, ReadableConfig formatOptions) {
    return null;
  }

  @Override
  public Set<ConfigOption<?>> requiredOptions() {
    return Collections.emptySet();
  }

  @Override
  public Set<ConfigOption<?>> optionalOptions() {
    return Collections.emptySet();
  }
}
