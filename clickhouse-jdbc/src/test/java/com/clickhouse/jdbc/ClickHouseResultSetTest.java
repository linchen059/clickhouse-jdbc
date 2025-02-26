package com.clickhouse.jdbc;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.testng.Assert;
import org.testng.annotations.Test;

public class ClickHouseResultSetTest extends JdbcIntegrationTest {
    @Test(groups = "integration")
    public void testBigDecimal() throws SQLException {
        try (ClickHouseConnection conn = newConnection(new Properties());
                Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("select toDecimal64(number / 10, 1) from numbers(10)");
            BigDecimal v = BigDecimal.valueOf(0L).setScale(1);
            while (rs.next()) {
                Assert.assertEquals(rs.getBigDecimal(1), v);
                Assert.assertEquals(rs.getObject(1), v);
                v = v.add(new BigDecimal("0.1"));
            }
        }
    }

    @Test(groups = "integration")
    public void testArray() throws SQLException {
        try (ClickHouseConnection conn = newConnection(new Properties());
                Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                    "select [1,2,3] v1, ['a','b', 'c'] v2, arrayZip(v1, v2) v3, cast(['2021-11-01 01:02:03', '2021-11-02 02:03:04'] as Array(DateTime32)) v4");
            Assert.assertTrue(rs.next());

            Assert.assertEquals(rs.getObject(1), new short[] { 1, 2, 3 });
            Assert.assertEquals(rs.getArray(1).getArray(), new short[] { 1, 2, 3 });
            Assert.assertTrue(rs.getArray(1).getArray() == rs.getObject(1));

            Assert.assertEquals(rs.getObject(2), new String[] { "a", "b", "c" });
            Assert.assertEquals(rs.getArray(2).getArray(), new String[] { "a", "b", "c" });
            Assert.assertTrue(rs.getArray(2).getArray() == rs.getObject(2));

            Assert.assertEquals(rs.getObject(3), new List[] { Arrays.asList((short) 1, "a"),
                    Arrays.asList((short) 2, "b"), Arrays.asList((short) 3, "c") });
            Assert.assertEquals(rs.getArray(3).getArray(), new List[] { Arrays.asList((short) 1, "a"),
                    Arrays.asList((short) 2, "b"), Arrays.asList((short) 3, "c") });
            Assert.assertTrue(rs.getArray(3).getArray() == rs.getObject(3));

            Assert.assertEquals(rs.getObject(4), new LocalDateTime[] { LocalDateTime.of(2021, 11, 1, 1, 2, 3),
                    LocalDateTime.of(2021, 11, 2, 2, 3, 4) });
            Assert.assertEquals(rs.getArray(4).getArray(), new LocalDateTime[] { LocalDateTime.of(2021, 11, 1, 1, 2, 3),
                    LocalDateTime.of(2021, 11, 2, 2, 3, 4) });
            Assert.assertTrue(rs.getArray(4).getArray() == rs.getObject(4));

            Assert.assertFalse(rs.next());
        }
    }

    @Test(groups = "integration")
    public void testTuple() throws SQLException {
        try (ClickHouseConnection conn = newConnection(new Properties());
                Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt
                    .executeQuery(
                            "select (toInt16(1), 'a', toFloat32(1.2), cast([1,2] as Array(Nullable(UInt8))), map(toUInt32(1),'a')) v");
            Assert.assertTrue(rs.next());
            List<?> v = rs.getObject(1, List.class);
            Assert.assertEquals(v.size(), 5);
            Assert.assertEquals(v.get(0), Short.valueOf((short) 1));
            Assert.assertEquals(v.get(1), "a");
            Assert.assertEquals(v.get(2), Float.valueOf(1.2F));
            Assert.assertEquals(v.get(3), new Short[] { 1, 2 });
            Assert.assertEquals(v.get(4), Collections.singletonMap(1L, "a"));
            Assert.assertFalse(rs.next());
        }
    }
}
