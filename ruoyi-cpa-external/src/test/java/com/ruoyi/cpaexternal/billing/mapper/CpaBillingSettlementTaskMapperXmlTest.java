package com.ruoyi.cpaexternal.billing.mapper;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.reflection.Reflector;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import com.ruoyi.cpaexternal.billing.domain.CpaBillingSettlementTask;

/**
 * 结算任务 Mapper 的 resultMap 与域类属性一致性测试。
 *
 * <p>resultMap 声明了域类不存在的属性时，所有查询会在结果映射阶段抛
 * ReflectionException（例如结算 Worker 轮询领取任务失败），此处在构建期拦截该类问题。</p>
 */
class CpaBillingSettlementTaskMapperXmlTest
{
    @Test
    void resultMapPropertiesShouldHaveMatchingSettersOnDomain() throws Exception
    {
        Configuration configuration = new Configuration();
        configuration.getTypeAliasRegistry().registerAlias("CpaBillingSettlementTask", CpaBillingSettlementTask.class);
        String resource = "mapper/cpa/CpaBillingSettlementTaskMapper.xml";
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource))
        {
            assertNotNull(inputStream, "找不到映射文件: " + resource);
            XMLMapperBuilder mapperBuilder = new XMLMapperBuilder(
                inputStream, configuration, resource, configuration.getSqlFragments());
            mapperBuilder.parse();
        }

        ResultMap resultMap = configuration.getResultMap(
            CpaBillingSettlementTaskMapper.class.getName() + ".CpaBillingSettlementTaskResult");
        Set<String> mappedProperties = new HashSet<>();
        for (ResultMapping mapping : resultMap.getResultMappings())
        {
            mappedProperties.add(mapping.getProperty());
        }
        assertTrue(mappedProperties.contains("createTime"), "resultMap 应映射 create_time");
        assertTrue(mappedProperties.contains("updateTime"), "resultMap 应映射 update_time");

        Reflector reflector = new Reflector(CpaBillingSettlementTask.class);
        for (String property : mappedProperties)
        {
            assertTrue(reflector.hasSetter(property),
                "CpaBillingSettlementTask 缺少 resultMap 属性的 setter: " + property);
        }
    }
}
