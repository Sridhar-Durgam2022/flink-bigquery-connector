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

import com.google.cloud.flink.bigquery.exception.FlinkBigQueryException;
import com.google.cloud.flink.bigquery.util.arrow.ArrowSchemaConverter;
import com.google.cloud.flink.bigquery.util.arrow.ArrowToRowDataConverters;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.types.pojo.Schema;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.types.logical.LogicalType;
import org.apache.flink.table.types.logical.RowType;
import org.apache.flink.util.Collector;

/** Reading the deserialized arrow data and converting into flink RowData */
public class ArrowRowDataDeserializationSchema
    implements DeserializationSchema<RowData>, Serializable {

  public static final long serialVersionUID = 1L;
  private TypeInformation<RowData> typeInfo;
  private DeserializationSchema<VectorSchemaRoot> nestedSchema;
  private ArrowToRowDataConverters.ArrowToRowDataConverter runtimeConverter;
  final List<String> readSessionFieldNames = new ArrayList<String>();
  private String arrowSchemaJson;

  public ArrowRowDataDeserializationSchema(
      RowType rowType,
      TypeInformation<RowData> typeInfo,
      List<String> selectedFieldList,
      List<String> arrowFieldList) {
    this.typeInfo = typeInfo;
    Schema arrowSchema =
        ArrowSchemaConverter.convertToSchema(
            getRowTypeForArrowSchema(rowType, selectedFieldList, arrowFieldList));
    arrowSchema.getFields().stream()
        .forEach(
            field -> {
              this.readSessionFieldNames.add(field.getName());
            });
    this.arrowSchemaJson = arrowSchema.toJson().toString();
    this.nestedSchema = ArrowDeserializationSchema.forGeneric(arrowSchemaJson, typeInfo);
    this.runtimeConverter =
        ArrowToRowDataConverters.createRowConverter(rowType, readSessionFieldNames);
  }

  private RowType getRowTypeForArrowSchema(
      RowType rowType, List<String> selectedFieldList, List<String> arrowFieldList) {
    List<String> rowFieldNames = rowType.getFieldNames();
    List<LogicalType> rowFields = rowType.getChildren();
    List<LogicalType> updatedRowFields = new ArrayList<LogicalType>();
    List<String> updatedRowFieldNames = new ArrayList<String>();
    for (int i = 0; i < rowFieldNames.size(); i++) {
      if (selectedFieldList.get(i).equals(arrowFieldList.get(i))) {
        updatedRowFieldNames.add(rowFieldNames.get(i));
        updatedRowFields.add(rowFields.get(i));
      } else {
        String arrowFieldsName = arrowFieldList.get(i);
        int rowTypeFieldIndex = selectedFieldList.indexOf(arrowFieldsName);
        updatedRowFieldNames.add(rowFieldNames.get(rowTypeFieldIndex));
        updatedRowFields.add(rowFields.get(rowTypeFieldIndex));
      }
    }
    RowType updatedRowType =
        RowType.of(
            updatedRowFields.toArray(new LogicalType[0]),
            updatedRowFieldNames.toArray(new String[0]));
    return updatedRowType;
  }

  @Override
  public TypeInformation<RowData> getProducedType() {
    return typeInfo;
  }

  @Override
  public void open(InitializationContext context) throws Exception {
    this.nestedSchema.open(context);
  }

  @Override
  public void deserialize(@Nullable byte[] responseByteMessage, Collector<RowData> out)
      throws IOException {
    if (responseByteMessage == null) {
      throw new FlinkBigQueryException("Deserializing message is empty");
    }
    VectorSchemaRoot root = null;
    try {
      root = nestedSchema.deserialize(responseByteMessage);
      List<GenericRowData> rowdatalist = (List<GenericRowData>) runtimeConverter.convert(root);
      for (int i = 0; i < rowdatalist.size(); i++) {
        out.collect(rowdatalist.get(i));
      }

    } catch (Exception ex) {
      throw new FlinkBigQueryException("Error while deserializing Arrow type", ex);
    } finally {
      if (root != null) {
        root.close();
      }
    }
  }

  @Override
  public RowData deserialize(@Nullable byte[] message) throws IOException {
    if (message == null) {
      return null;
    }
    RowData rowData;
    try {
      VectorSchemaRoot root = nestedSchema.deserialize(message);
      rowData = (RowData) runtimeConverter.convert(root);
    } catch (Exception ex) {
      throw new FlinkBigQueryException("Error while deserializing Arrow type", ex);
    }
    return rowData;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ArrowRowDataDeserializationSchema that = (ArrowRowDataDeserializationSchema) o;
    return nestedSchema.equals(that.nestedSchema) && typeInfo.equals(that.typeInfo);
  }

  @Override
  public int hashCode() {
    return Objects.hash(nestedSchema, typeInfo);
  }

  @Override
  public boolean isEndOfStream(RowData nextElement) {
    return nextElement == null;
  }
}
