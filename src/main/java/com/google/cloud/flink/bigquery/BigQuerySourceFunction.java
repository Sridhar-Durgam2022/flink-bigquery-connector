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
 * numOfStreamsations under the License.
 */
package com.google.cloud.flink.bigquery;

import com.google.cloud.bigquery.connector.common.BigQueryClientFactory;
import com.google.cloud.bigquery.connector.common.ReadRowsHelper;
import com.google.cloud.bigquery.connector.common.ReadRowsHelper.Options;
import com.google.cloud.bigquery.storage.v1.ReadRowsRequest;
import com.google.cloud.bigquery.storage.v1.ReadRowsResponse;
import com.google.cloud.flink.bigquery.exception.FlinkBigQueryException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import org.apache.flink.api.common.functions.util.ListCollector;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.serialization.RuntimeContextInitializationContextAdapters;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.ResultTypeQueryable;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.source.RichParallelSourceFunction;
import org.apache.flink.table.data.RowData;
import org.apache.flink.util.Collector;
import org.apache.flink.util.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runtime execution that reads data from big query read streams, passing for deserialization and
 * adding to the flink context.
 */
public final class BigQuerySourceFunction extends RichParallelSourceFunction<RowData>
    implements ResultTypeQueryable<RowData> {

  boolean running = true;
  private static final long serialVersionUID = 1;
  private static final Logger log = LoggerFactory.getLogger(BigQuerySourceFunction.class);
  private int numOfStreams;
  private int executerIndex;
  private DeserializationSchema<RowData> deserializer;
  private ArrayList<String> readSessionStreamList = new ArrayList<>();
  private BigQueryClientFactory bigQueryReadClientFactory;
  private List<String> streamNames = new ArrayList<String>();
  private int numOfExecutors;

  public BigQuerySourceFunction() {}

  public BigQuerySourceFunction(
      DeserializationSchema<RowData> deserializer,
      ArrayList<String> readSessionStreams,
      BigQueryClientFactory bigQueryReadClientFactory) {

    this.deserializer = deserializer;
    this.readSessionStreamList = readSessionStreams;
    this.numOfStreams = readSessionStreamList.size();
    this.bigQueryReadClientFactory = bigQueryReadClientFactory;
  }

  @Override
  public TypeInformation<RowData> getProducedType() {
    return deserializer.getProducedType();
  }

  @Override
  public void open(Configuration parameters) throws Exception {
    deserializer.open(
        RuntimeContextInitializationContextAdapters.deserializationAdapter(getRuntimeContext()));
    this.executerIndex = getRuntimeContext().getIndexOfThisSubtask();
    this.numOfExecutors = getRuntimeContext().getNumberOfParallelSubtasks();
    this.numOfStreams = readSessionStreamList.size();
    this.streamNames.clear();
    for (int i = executerIndex; i < numOfStreams; i += numOfExecutors) {
      if (running) {
        this.streamNames.add(readSessionStreamList.get(i));
      }
    }
  }

  @SuppressWarnings("resource")
  @Override
  public void run(SourceContext<RowData> ctx) throws Exception {
    List<RowData> outputCollector = new ArrayList<>();
    ListCollector<RowData> listCollector = new ListCollector<>(outputCollector);
    Options options =
        new ReadRowsHelper.Options(
            /* maxReadRowsRetries= */ 5,
            Optional.of("endpoint"),
            /* backgroundParsingThreads= */ 5,
            1);

    for (String streamName : streamNames) {
      ReadRowsRequest.Builder readRowsRequest =
          ReadRowsRequest.newBuilder().setReadStream(streamName);
      ReadRowsHelper readRowsHelper =
          new ReadRowsHelper(bigQueryReadClientFactory, readRowsRequest, options);

      Iterator<ReadRowsResponse> readRows = readRowsHelper.readRows();
      while (readRows.hasNext()) {
        ReadRowsResponse response = readRows.next();
        try {
          if (response.hasArrowRecordBatch()) {
            Preconditions.checkState(response.hasArrowRecordBatch());
            deserializer.deserialize(response.toByteArray(), (Collector<RowData>) listCollector);
            for (int i = 0; i < outputCollector.size(); i++) {
              ctx.collect((RowData) outputCollector.get(i));
            }
            outputCollector.clear();
          } else if (response.hasAvroRows()) {
            Preconditions.checkState(response.hasAvroRows());
            long numOfRows = response.getRowCount();
            for (int i = 0; i < numOfRows; i++) {
              ctx.collect(
                  deserializer.deserialize(
                      response.getAvroRows().getSerializedBinaryRows().toByteArray()));
            }
          }
        } catch (IOException ex) {
          log.error("Error while deserialization:", ex);
          throw new FlinkBigQueryException("Error while deserialization:", ex);
        }
      }
      readRowsHelper.close();
    }
  }

  @Override
  public void close() {
    running = false;
  }

  @Override
  public void cancel() {}
}
