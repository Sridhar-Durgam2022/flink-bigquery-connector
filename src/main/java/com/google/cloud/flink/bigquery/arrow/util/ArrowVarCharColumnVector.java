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
package com.google.cloud.flink.bigquery.arrow.util;

import org.apache.arrow.vector.VarCharVector;
import org.apache.flink.table.data.vector.BytesColumnVector;
import org.apache.flink.util.Preconditions;

/** Arrow column vector for VarChar. */

public final class ArrowVarCharColumnVector implements BytesColumnVector {

	/**
	 * Container which is used to store the sequence of varchar values of a column
	 * to read.
	 */
	private final VarCharVector varCharVector;

	public ArrowVarCharColumnVector(VarCharVector varCharVector) {
		this.varCharVector = Preconditions.checkNotNull(varCharVector, "VarCharVector is null");
	}

	@Override
	public Bytes getBytes(int i) {
		byte[] bytes = varCharVector.get(i);
		return new Bytes(bytes, 0, bytes.length);
	}

	@Override
	public boolean isNullAt(int i) {
		return varCharVector.isNull(i);
	}
}
