package com.ruoyi.cpaexternal.log.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import org.junit.jupiter.api.Test;

/** CLIProxyAPI 上报时间解析测试：ISO-8601 字符串需要格式化成入库用的 datetime。 */
class CpaLogTimestampParserTest
{
    @Test
    void shouldParseNanosecondTimestampWithOffset()
    {
        Date parsed = CpaLogTimestampParser.parse("2026-09-10T10:59:53.738095654+08:00");

        // 纳秒精度超出 Date 的毫秒精度，按毫秒截断；时区偏移换算成绝对时间点。
        assertEquals(Instant.parse("2026-09-10T02:59:53.738Z"), parsed.toInstant());
    }

    @Test
    void shouldParseUtcTimestamp()
    {
        Date parsed = CpaLogTimestampParser.parse("2026-04-25T00:00:00Z");

        assertEquals(Instant.parse("2026-04-25T00:00:00Z"), parsed.toInstant());
    }

    @Test
    void shouldParseTimestampWithoutOffsetAsLocalTime()
    {
        Date parsed = CpaLogTimestampParser.parse("2026-09-10 10:59:53");

        assertEquals(LocalDateTime.of(2026, 9, 10, 10, 59, 53)
                .atZone(ZoneId.systemDefault()).toInstant(), parsed.toInstant());
    }

    @Test
    void shouldReturnNullWhenTimestampMissingOrUnrecognized()
    {
        assertNull(CpaLogTimestampParser.parse(null));
        assertNull(CpaLogTimestampParser.parse("   "));
        assertNull(CpaLogTimestampParser.parse("2026/09/10 10:59:53"));
    }
}
