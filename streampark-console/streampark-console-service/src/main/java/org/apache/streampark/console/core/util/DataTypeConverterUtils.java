/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.streampark.console.core.util;

import org.apache.streampark.console.core.bean.FlinkDataType;

import org.apache.flink.table.api.DataTypes;
import org.apache.flink.table.types.DataType;
import org.apache.flink.table.types.logical.CharType;
import org.apache.flink.table.types.logical.DecimalType;
import org.apache.flink.table.types.logical.LogicalType;
import org.apache.flink.table.types.logical.VarCharType;

public class DataTypeConverterUtils {

    public static DataType convertToDataType(FlinkDataType flinkDataType) {
        DataType dataType;

        String type = flinkDataType.getType().toLowerCase();
        boolean isNullable = flinkDataType.isNullable();
        Integer precision = flinkDataType.getPrecision();
        Integer scale = flinkDataType.getScale();

        switch (type) {
            // Integer types
            case "tinyint":
            case "int1":
                dataType = DataTypes.TINYINT();
                break;
            case "smallint":
            case "int2":
                dataType = DataTypes.SMALLINT();
                break;
            case "int":
            case "integer":
            case "int4":
                dataType = DataTypes.INT();
                break;
            case "bigint":
            case "int8":
                dataType = DataTypes.BIGINT();
                break;

            // Floating-point types
            case "float":
            case "real":
                dataType = DataTypes.FLOAT();
                break;
            case "double":
            case "float8":
                dataType = DataTypes.DOUBLE();
                break;

            // Decimal and Numeric types
            case "decimal":
            case "numeric":
                if (precision != null && scale != null) {
                    dataType = DataTypes.DECIMAL(precision, scale);
                } else {
                    dataType = DataTypes.DECIMAL(38, 18); // Default precision and scale
                }
                break;

            // Character types
            case "char":
                if (precision != null) {
                    dataType = DataTypes.CHAR(precision);
                } else {
                    dataType = DataTypes.CHAR(1); // Default size
                }
                break;
            case "varchar":
            case "string":
            case "text":
                dataType = DataTypes.STRING();
                break;

            // Binary data types
            case "binary":
            case "varbinary":
            case "blob":
                dataType = DataTypes.BYTES();
                break;

            // Date and time types
            case "date":
                dataType = DataTypes.DATE();
                break;
            case "timestamp":
                if (precision != null) {
                    dataType = DataTypes.TIMESTAMP(precision);
                } else {
                    dataType = DataTypes.TIMESTAMP(3); // Default precision
                }
                break;
            case "time":
                dataType = DataTypes.TIME();
                break;

            // Boolean type
            case "boolean":
            case "bool":
                dataType = DataTypes.BOOLEAN();
                break;

            // JSON and other types
            case "json":
                dataType = DataTypes.STRING(); // JSON as STRING in Flink
                break;
            case "uuid":
                dataType = DataTypes.STRING(); // UUID as STRING
                break;

            // Default case for unsupported types
            default:
                throw new IllegalArgumentException("Unsupported type: " + type);
        }

        // Apply nullability
        return isNullable ? dataType.nullable() : dataType.notNull();
    }

    public static FlinkDataType convertorToFlinkDataType(DataType dataType) {
        LogicalType logicalType = dataType.getLogicalType();
        boolean isNullable = dataType.getLogicalType().isNullable();
        String typeName = logicalType.getTypeRoot().name().toLowerCase();
        Integer precision = null;
        Integer scale = null;

        switch (logicalType.getTypeRoot()) {
            case CHAR:
            case VARCHAR:
                if (logicalType instanceof CharType) {
                    precision = ((CharType) logicalType).getLength();
                } else if (logicalType instanceof VarCharType) {
                    precision = ((VarCharType) logicalType).getLength();
                }
                break;
            case DECIMAL:
                if (logicalType instanceof DecimalType) {
                    precision = ((DecimalType) logicalType).getPrecision();
                    scale = ((DecimalType) logicalType).getScale();
                }
                break;
            case TINYINT:
            case SMALLINT:
            case INTEGER:
            case BIGINT:
            case FLOAT:
            case DOUBLE:
            case DATE:
            case TIME_WITHOUT_TIME_ZONE:
            case TIMESTAMP_WITHOUT_TIME_ZONE:
            case TIMESTAMP_WITH_LOCAL_TIME_ZONE:
            case BOOLEAN:
            case BINARY:
            case VARBINARY:
            case ARRAY:
            case MULTISET:
            case MAP:
            case ROW:
            case RAW:
            case NULL:
            case SYMBOL:
            case UNRESOLVED:
            case INTERVAL_YEAR_MONTH:
            case INTERVAL_DAY_TIME:
            case TIMESTAMP_WITH_TIME_ZONE:
            case DISTINCT_TYPE:
            case STRUCTURED_TYPE:
                // case JSON:
                // case UUID:
                // These types do not have precision or scale
                break;
            default:
                throw new IllegalArgumentException("Unsupported type: " + logicalType);
        }

        return new FlinkDataType(typeName, isNullable, precision, scale);
    }
}
