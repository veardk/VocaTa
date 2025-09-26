package com.vocata.common.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * UUID类型处理器
 * 用于处理UUID类型与数据库字段的转换
 */
@MappedTypes({UUID.class})
@MappedJdbcTypes({JdbcType.OTHER, JdbcType.VARCHAR})
public class UuidTypeHandler extends BaseTypeHandler<UUID> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, UUID parameter, JdbcType jdbcType) throws SQLException {
        ps.setObject(i, parameter);
    }

    @Override
    public UUID getNullableResult(ResultSet rs, String columnName) throws SQLException {
        Object object = rs.getObject(columnName);
        if (object == null) {
            return null;
        }
        if (object instanceof UUID) {
            return (UUID) object;
        }
        if (object instanceof String) {
            return UUID.fromString((String) object);
        }
        return null;
    }

    @Override
    public UUID getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        Object object = rs.getObject(columnIndex);
        if (object == null) {
            return null;
        }
        if (object instanceof UUID) {
            return (UUID) object;
        }
        if (object instanceof String) {
            return UUID.fromString((String) object);
        }
        return null;
    }

    @Override
    public UUID getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        Object object = cs.getObject(columnIndex);
        if (object == null) {
            return null;
        }
        if (object instanceof UUID) {
            return (UUID) object;
        }
        if (object instanceof String) {
            return UUID.fromString((String) object);
        }
        return null;
    }
}