package com.ruoyi.cpaexternal.billing.mapper;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.reflection.Reflector;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import com.ruoyi.common.core.domain.entity.SysDept;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.system.mapper.SysUserMapper;

/**
 * SysUserMapper 并发计数语句的 XML 解析与绑定测试。
 *
 * <p>并发自增/递减是预占热路径的关键语句，此处拦截 XML 拼写、resultMap
 * 漂移与参数绑定问题（对应 ruoyi-system 模块无独立测试目录的现状）。</p>
 */
class SysUserConcurrencyMapperXmlTest
{
    @Test
    void concurrencyStatementsShouldParseAndBind() throws Exception
    {
        Configuration configuration = new Configuration();
        configuration.getTypeAliasRegistry().registerAlias("SysUser", SysUser.class);
        configuration.getTypeAliasRegistry().registerAlias("SysDept", SysDept.class);
        configuration.getTypeAliasRegistry().registerAlias("SysRole", SysRole.class);
        String resource = "mapper/system/SysUserMapper.xml";
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource))
        {
            assertNotNull(inputStream, "找不到映射文件: " + resource);
            XMLMapperBuilder mapperBuilder = new XMLMapperBuilder(
                inputStream, configuration, resource, configuration.getSqlFragments());
            mapperBuilder.parse();
        }

        // resultMap 映射齐全且域类存在对应 setter
        ResultMap resultMap = configuration.getResultMap(SysUserMapper.class.getName() + ".SysUserResult");
        boolean mapped = false;
        Reflector reflector = new Reflector(SysUser.class);
        for (ResultMapping mapping : resultMap.getResultMappings())
        {
            if ("activeRequestCount".equals(mapping.getProperty()))
            {
                mapped = true;
            }
            assertTrue(reflector.hasSetter(mapping.getProperty()),
                "SysUser 缺少 resultMap 属性的 setter: " + mapping.getProperty());
        }
        assertTrue(mapped, "SysUserResult 应映射 active_request_count");

        Map<String, Object> params = new HashMap<>();
        params.put("userId", 100L);

        MappedStatement increment = configuration.getMappedStatement(
            SysUserMapper.class.getName() + ".incrementUserActiveRequestCount");
        String incrementSql = increment.getBoundSql(params).getSql();
        assertTrue(incrementSql.contains("active_request_count + 1"), "自增语句应累加计数列");
        assertTrue(incrementSql.contains("ai_concurrency_limit"), "自增语句应受并发上限条件守卫");

        MappedStatement decrement = configuration.getMappedStatement(
            SysUserMapper.class.getName() + ".decrementUserActiveRequestCount");
        String decrementSql = decrement.getBoundSql(params).getSql();
        assertTrue(decrementSql.contains("active_request_count - 1"), "递减语句应累减计数列");
        assertTrue(decrementSql.contains("active_request_count > 0"), "递减语句应带非负守卫");
    }
}
