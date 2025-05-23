package com.clickhouse.client.api.data_formats;

import com.clickhouse.client.api.data_formats.internal.SerializerUtils;
import com.clickhouse.client.api.metadata.TableSchema;
import com.clickhouse.data.ClickHouseColumn;
import com.clickhouse.data.ClickHouseFormat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;


/**
 * This class is intended to help writing data in row binary format.
 * It handles Nullable and Defaults.
 * It implements record and commit approach row-by-row. It means that data is not written immediately but it is stored
 * until {@link #commitRow()} is called.
 * <p>
 * Experimental API
 */
public class RowBinaryFormatWriter implements ClickHouseBinaryFormatWriter {

    private final OutputStream out;

    private final TableSchema tableSchema;

    private final Object[] row;

    private final boolean defaultSupport;

    private int rowCount = 0;

    private boolean rowStarted = false; // indicates if at least one value was written to a row

    public RowBinaryFormatWriter(OutputStream out, TableSchema tableSchema, ClickHouseFormat format) {
        if (format != ClickHouseFormat.RowBinary && format != ClickHouseFormat.RowBinaryWithDefaults) {
            throw new IllegalArgumentException("Only RowBinary and RowBinaryWithDefaults are supported");
        }

        this.out = out;
        this.tableSchema = tableSchema;
        this.row = new Object[tableSchema.getColumns().size()];
        this.defaultSupport = format == ClickHouseFormat.RowBinaryWithDefaults;
    }

    @Override
    public OutputStream getOutputStream() {
        return out;
    }

    @Override
    public int getRowCount() {
        return rowCount;
    }

    @Override
    public ClickHouseFormat getFormat() {
        return defaultSupport ? ClickHouseFormat.RowBinaryWithDefaults : ClickHouseFormat.RowBinary;
    }

    @Override
    public void clearRow() {
        Arrays.fill(row, null);
        rowStarted = false;
    }

    @Override
    public void setValue(String column, Object value) {
        setValue(tableSchema.nameToColumnIndex(column), value);
    }

    @Override
    public void setValue(int colIndex, Object value) {
        row[colIndex - 1] = value;
        if (!rowStarted) {
            rowStarted = true;
        }
    }

    @Override
    public void commitRow() throws IOException {
        if (rowStarted) {
            List<ClickHouseColumn> columnList = tableSchema.getColumns();
            for (int i = 0; i < row.length; i++) {
                ClickHouseColumn column = columnList.get(i);
                // here we skip if we have a default value that is MATERIALIZED or ALIAS or ...
                if (column.hasDefault() && column.getDefaultValue() != ClickHouseColumn.DefaultValue.DEFAULT)
                    continue;
                if (RowBinaryFormatSerializer.writeValuePreamble(out, defaultSupport, column, row[i])) {
                    SerializerUtils.serializeData(out, row[i], column);
                }
            }
            clearRow();
            rowCount++;
        }
    }

    @Override
    public void setByte(String column, byte value) {
        setValue(column, value);
    }

    @Override
    public void setByte(int colIndex, byte value) {
        setValue(colIndex, value);
    }

    @Override
    public void setShort(String column, short value) {
        setValue(column, value);
    }

    @Override
    public void setShort(int colIndex, short value) {
        setValue(colIndex, value);
    }

    @Override
    public void setInteger(String column, int value) {
        setValue(column, value);
    }

    @Override
    public void setInteger(int colIndex, int value) {
        setValue(colIndex, value);
    }

    @Override
    public void setLong(String column, long value) {
        setValue(column, value);
    }

    @Override
    public void setLong(int colIndex, long value) {
        setValue(colIndex, value);
    }

    @Override
    public void setBigInteger(int colIndex, BigInteger value) {
        setValue(colIndex, value);
    }

    @Override
    public void setBigInteger(String column, BigInteger value) {
        setValue(column, value);
    }

    @Override
    public void setFloat(int colIndex, float value) {
        setValue(colIndex, value);
    }

    @Override
    public void setFloat(String column, float value) {
        setValue(column, value);
    }

    @Override
    public void setDouble(int colIndex, double value) {
        setValue(colIndex, value);
    }

    @Override
    public void setDouble(String column, double value) {
        setValue(column, value);
    }

    @Override
    public void setBigDecimal(int colIndex, BigDecimal value) {
        setValue(colIndex, value);
    }

    @Override
    public void setBigDecimal(String column, BigDecimal value) {
        setValue(column, value);
    }

    @Override
    public void setBoolean(int colIndex, boolean value) {
        setValue(colIndex, value);
    }

    @Override
    public void setBoolean(String column, boolean value) {
        setValue(column, value);
    }

    @Override
    public void setString(String column, String value) {
        setValue(column, value);
    }

    @Override
    public void setString(int colIndex, String value) {
        setValue(colIndex, value);
    }

    @Override
    public void setDate(String column, LocalDate value) {
        setValue(column, value);
    }

    @Override
    public void setDate(int colIndex, LocalDate value) {
        setValue(colIndex, value);
    }

    @Override
    public void setDateTime(String column, LocalDateTime value) {
        setValue(column, value);
    }

    @Override
    public void setDateTime(int colIndex, LocalDateTime value) {
        setValue(colIndex, value);
    }

    @Override
    public void setDateTime(String column, ZonedDateTime value) {
        setValue(column, value);
    }

    @Override
    public void setDateTime(int colIndex, ZonedDateTime value) {
        setValue(colIndex, value);
    }

    @Override
    public void setList(String column, List<?> value) {
        setValue(column, value);
    }

    @Override
    public void setList(int colIndex, List<?> value) {
        setValue(colIndex, value);
    }

    @Override
    public void setInputStream(int colIndex, InputStream in, long len) {
        setValue(colIndex, new InputStreamHolder(in, len));
    }

    @Override
    public void setInputStream(String column, InputStream in, long len) {
        setValue(column, new InputStreamHolder(in, len));
    }

    @Override
    public void setReader(int colIndex, Reader reader, long len) {
        setValue(colIndex, new ReaderHolder(reader, len));
    }

    @Override
    public void setReader(String column, Reader reader, long len) {
        setValue(column, new ReaderHolder(reader, len));
    }

    private static class InputStreamHolder {
        final InputStream stream;
        final long length;
        InputStreamHolder(InputStream stream, long length) {
            this.stream = stream;
            this.length = length;
        }
    }

    private static class ReaderHolder {
        final Reader read;
        final long length;
        ReaderHolder(Reader reader, long length) {
            this.read = reader;
            this.length = length;
        }
    }
}
