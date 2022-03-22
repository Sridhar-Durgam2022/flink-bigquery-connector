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
package com.google.cloud.flink.bigquery.integration;

import static org.apache.flink.table.api.Expressions.$;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

<<<<<<< HEAD
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.flink.bigquery.FlinkBigQueryException;
=======
import org.apache.flink.streaming.api.datastream.DataStream;
>>>>>>> 33053ff (Removed classes from util package which are not required at the moment)
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.TableResult;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.types.Row;
import org.apache.flink.util.CloseableIterator;
<<<<<<< HEAD
=======
import org.junit.Ignore;
>>>>>>> 33053ff (Removed classes from util package which are not required at the moment)
import org.junit.Test;

public class FlinkReadFromQueryIntegrationTest extends FlinkBigQueryIntegrationTestBase {

  private BigQuery bq;
  StreamTableEnvironment flinkTableEnv;

  public FlinkReadFromQueryIntegrationTest() {

    this.bq = BigQueryOptions.getDefaultInstance().getService();
    StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
    env.setParallelism(1); // source only supports parallelism of 1
    flinkTableEnv = StreamTableEnvironment.create(env);
  }

  @Test
  public void testReadFromQuery() {
    String bigqueryReadTable = "q-gcp-6750-pso-gs-flink-22-01.wordcount_dataset.wordcount_output";
    String flinkSrcTable = "FlinkSrcTable";
    String srcQueryString = "CREATE TABLE " + flinkSrcTable + " (word STRING , word_count BIGINT)";
    flinkTableEnv.executeSql(
        srcQueryString
            + "\n"
            + "WITH (\n"
            + "  'connector' = 'bigquery',\n"
            + "  'format' = 'arrow',\n"
            + "  'table' = '"
            + bigqueryReadTable
            + "',\n"
            + "  'filter' = 'word_count > 500',\n"
            + "  'credentialsFile' = '"
            + System.getenv("GOOGLE_APPLICATION_CREDENTIALS")
            + "' ,\n"
            + "  'selectedFields' = 'word,word_count' \n"
            + ")");
    Table result = flinkTableEnv.from(flinkSrcTable);
    Table datatable = result.select($("word"), $("word_count"));
    TableResult tableapi = datatable.execute();
    assertNotNull(tableapi);
    assertEquals(2, tableapi.getTableSchema().getFieldCount());
  }

  @Test
  public void testBadSql() {
    assertThrows(
        RuntimeException.class,
        () -> {
          String bigqueryReadTable =
              "q-gcp-6750-pso-gs-flink-22-01.wordcount_dataset.wordcount_output";
          String flinkSrcTable = "FlinkSrcTable";
          String flinkSrcTable1 = "FlinkSrcTable";
          String srcQueryString =
              "CREATE TABLE " + flinkSrcTable1 + " (word STRING , word_count BIGINT)";
          flinkTableEnv.executeSql(
              srcQueryString
                  + "\n"
                  + "WITH (\n"
                  + "  'connector' = 'bigquery',\n"
                  + "  'format' = 'arrow',\n"
                  + "  'table' = '"
                  + bigqueryReadTable
                  + "',\n"
                  + "  'filter' = 'word_count > 500',\n"
                  + "  'credentialsFile' = '"
                  + System.getenv("GOOGLE_APPLICATION_CREDENTIALS")
                  + "' ,\n"
                  + "  'selectedFields' = 'word_test' \n"
                  + ")");
          Table result = flinkTableEnv.from(flinkSrcTable1);
          Table datatable =
              result
                  .where($("word_count").isGreaterOrEqual(100))
                  .select($("word"), $("word_count"));
          TableResult tableapi = datatable.execute();
          throw new FlinkBigQueryException("Column not found");
        });
  }

<<<<<<< HEAD
  // We are passing all the configuration values and setting filter in flink and tableAPI both
  // together.
  @Test
  public void testReadFromQueryInternal() throws Exception {
    String bigqueryReadTable = "q-gcp-6750-pso-gs-flink-22-01.wordcount_dataset.wordcount_output";
    String flinkSrcTable = "FlinkSrcTable";
    String filter = "word_count > 500 and word=\"I\"";
    String srcQueryString = "CREATE TABLE " + flinkSrcTable + " (word STRING , word_count BIGINT)";
    flinkTableEnv.executeSql(
        srcQueryString
            + "\n"
            + "WITH (\n"
            + "  'connector' = 'bigquery',\n"
            + "  'format' = 'arrow',\n"
            + "  'table' = '"
            + bigqueryReadTable
            + "',\n"
            + "  'filter' = '"
            + filter
            + "',\n"
            + "  'credentialsFile' = '"
            + System.getenv("GOOGLE_APPLICATION_CREDENTIALS")
            + "' ,\n"
            + "  'selectedFields' = 'word,word_count' \n"
            + ")");
    Table result = flinkTableEnv.from(flinkSrcTable);
    Table datatable =
        result.where($("word_count").isGreaterOrEqual(100)).select($("word"), $("word_count"));
    int count = 0;
    TableResult tableResult = datatable.execute();
    try (CloseableIterator<Row> it = tableResult.collect()) {
      while (it.hasNext()) {
        it.next();
        count += 1;
      }
    }
    assertEquals(count, 16);
  }
}
=======
	@Test
	public void testReadFromQuery() {

		config.setQuery(
				"SELECT word, word_count FROM q-gcp-6750-pso-gs-flink-22-01.wordcount_dataset.wordcount_output WHERE word = \"speedy\"");
		config.setBigQueryReadTable("wordcount_output");
		config.setProjectId("q-gcp-6750-pso-gs-flink-22-01");
		config.setDataset("wordcount_dataset");
		String srcQueryString = "CREATE TABLE " + config.getBigQueryReadTable() + " (word STRING , word_count BIGINT)";
		flinkTableEnv.executeSql(srcQueryString + "\n" + "WITH (\n" + "  'connector' = 'bigquery',\n"
				+ "  'format' = 'arrow',\n" + "  'configOptions' = '" + config.getConfigMap() + "'\n" + ")");
		Table result = flinkTableEnv.from(config.getBigQueryReadTable());
		Table datatable = result.select($("word"), $("word_count"));
		TableResult tableapi = datatable.execute();		
		assertNotNull(tableapi);
		assertEquals(2, tableapi.getTableSchema().getFieldCount());
	}

	@Test
	public void testBadSql() {
		config.setQuery("SELECT word_bogus FROM q-gcp-6750-pso-gs-flink-22-01.wordcount_dataset.wordcount_output");
		assertThrows(RuntimeException.class, () -> {
			config.setBigQueryReadTable("wordcount_output");
			config.setProjectId("q-gcp-6750-pso-gs-flink-22-01");
			config.setDataset("wordcount_dataset");
			String srcQueryString = "CREATE TABLE " + config.getBigQueryReadTable()
					+ " (word STRING , word_count BIGINT)";
			flinkTableEnv.executeSql(srcQueryString + "\n" + "WITH (\n" + "  'connector' = 'bigquery',\n"
					+ "  'format' = 'arrow',\n" + "  'configOptions' = '" + config.getConfigMap() + "'\n" + ")");
			Table result = flinkTableEnv.from(config.getBigQueryReadTable());
			Table datatable = result.where($("word_count").isGreaterOrEqual(100)).select($("word"), $("word_count"));
			TableResult tableapi = datatable.execute();
			throw new FlinkBigQueryException("Column not found");
		});
	}
	
	//We are passing all the configuration values and setting filter in flink and tableAPI both together.
    @Ignore@Test
    public void testReadFromQueryInternal1() {
    		config.setFilter("word_count > 100");
            config.setProjectId("q-gcp-6750-pso-gs-flink-22-01");
            config.setDataset("wordcount_dataset");
            config.setBigQueryReadTable("wordcount_output");
            config.setSelectedFields("word,word_count");
            //config.setParallelism(10);
            String projectName = config.getProjectId() + "." + config.getDataset() + "." + config.getBigQueryReadTable();
            String query = "SELECT word, word_count FROM " + projectName + " WHERE " + config.getFilter();
            config.setQuery(query);

            String srcQueryString = "CREATE TABLE " + config.getBigQueryReadTable() + " (word STRING , word_count BIGINT)";
            flinkTableEnv.executeSql(srcQueryString + "\n" + "WITH (\n" + "  'connector' = 'bigquery',\n"
                            + "  'format' = 'arrow',\n" + "  'configOptions' = '" + config.getConfigMap() + "'\n" + ")");
            Table result = flinkTableEnv.from(config.getBigQueryReadTable());
            Table datatable = result.where($("word_count").isGreaterOrEqual(100)).select($("word"), $("word_count"));
            DataStream<Row> ds = flinkTableEnv.toDataStream(datatable);
            
            int count = 0;
            try {
                    CloseableIterator<Row> itr = ds.executeAndCollect();
                    while (itr.hasNext()) {
                            Row it = itr.next();
                            count += 1;
                    } 
            } catch (Exception e) {
                    e.printStackTrace();
            }
            System.out.print(count);
            assertEquals(count, 16);
    }
}
>>>>>>> 33053ff (Removed classes from util package which are not required at the moment)
