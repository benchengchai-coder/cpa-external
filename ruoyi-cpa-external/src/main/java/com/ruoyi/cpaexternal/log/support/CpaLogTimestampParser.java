package com.ruoyi.cpaexternal.log.support;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Date;

/**
 * CLIProxyAPI 上报时间解析器。
 *
 * <p>usage 消息里的 timestamp 是 ISO-8601 字符串，且秒的小数位可能超过 3 位
 * （例如 {@code 2026-09-10T10:59:53.738095654+08:00}），无法直接存入 MySQL 的 datetime 列。
 * 入库前统一在这里解析成时间对象，由数据库按 datetime 保存。</p>
 *
 * <p>该解析只处理 CLIProxyAPI 已上报的用量数据，不参与 AI 请求转发。</p>
 */
public final class CpaLogTimestampParser
{
    private CpaLogTimestampParser()
    {
    }

    /**
     * 把 CLIProxyAPI 上报的时间字符串解析为入库用的时间。
     *
     * @param raw 上报的 ISO-8601 时间，例如 2026-09-10T10:59:53.738095654+08:00
     * @return 解析结果；为空或格式无法识别时返回 null，由调用方决定回退值
     */
    public static Date parse(String raw)
    {
        if (raw == null)
        {
            return null;
        }
        String text = raw.trim();
        if (text.isEmpty())
        {
            return null;
        }
        // 上报值用 T 分隔，这里同时接受 MySQL datetime 的“日期 时间”写法。
        String normalized = text.replace(' ', 'T');
        Date offsetDateTime = parseOffsetDateTime(normalized);
        if (offsetDateTime != null)
        {
            return offsetDateTime;
        }
        return parseLocalDateTime(normalized);
    }

    /** 解析带时区偏移的时间：2026-09-10T10:59:53.738095654+08:00、2026-04-25T00:00:00Z。 */
    private static Date parseOffsetDateTime(String text)
    {
        try
        {
            return Date.from(OffsetDateTime.parse(text).toInstant());
        }
        catch (DateTimeParseException exception)
        {
            return null;
        }
    }

    /** 解析不带偏移的时间，按本机默认时区解释。 */
    private static Date parseLocalDateTime(String text)
    {
        try
        {
            LocalDateTime localDateTime = LocalDateTime.parse(text);
            return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
        }
        catch (DateTimeParseException exception)
        {
            return null;
        }
    }
}
