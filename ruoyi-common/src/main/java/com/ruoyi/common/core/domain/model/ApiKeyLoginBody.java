package com.ruoyi.common.core.domain.model;

/**
 * API Key 密钥登录对象。
 */
public class ApiKeyLoginBody
{
    /** API Key 明文。 */
    private String apiKey;

    public String getApiKey()
    {
        return apiKey;
    }

    public void setApiKey(String apiKey)
    {
        this.apiKey = apiKey;
    }
}
