package com.ruoyi.common.enums;

/**
 * 业务操作类型
 * 
 * @author ruoyi
 */
public enum BusinessType
{
    /**
     * 其它
     */
    OTHER(0),

    /**
     * 新增
     */
    INSERT(1),

    /**
     * 修改
     */
    UPDATE(2),

    /**
     * 删除
     */
    DELETE(3),

    /**
     * 授权
     */
    GRANT(4),

    /**
     * 导出
     */
    EXPORT(5),

    /**
     * 导入
     */
    IMPORT(6),

    /**
     * 强退
     */
    FORCE(7),

    /**
     * 清空数据
     */
    CLEAN(9);

    private final int code;

    BusinessType(int code)
    {
        this.code = code;
    }

    /**
     * 返回持久化到操作日志的业务类型编码。
     *
     * <p>编码沿用历史数据库值，代码生成类型移除后仍保留清空数据的编码 9，避免已有日志含义发生变化。</p>
     */
    public int getCode()
    {
        return code;
    }
}
