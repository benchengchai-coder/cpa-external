package com.ruoyi.cpaexternal.apikey.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.cpaexternal.apikey.domain.CpaApiKey;

/** API Key 数据访问层。 */
public interface CpaApiKeyMapper
{
    CpaApiKey selectById(Long keyId);

    CpaApiKey selectByPlainKey(String apiKey);

    CpaApiKey selectByUserId(Long userId);

    List<CpaApiKey> selectList(CpaApiKey query);

    int countByUserId(Long userId);

    int insert(CpaApiKey apiKey);

    int update(CpaApiKey apiKey);

    int updateSecret(@Param("keyId") Long keyId, @Param("apiKey") String apiKey);

    int deleteById(Long keyId);

    int deleteByIds(@Param("keyIds") Long[] keyIds);
}
