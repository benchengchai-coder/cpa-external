package com.ruoyi.cpaexternal.log.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** CLIProxyAPI 日志中的 fail 对象，记录失败请求的状态码与响应体。 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CpaAiLogFail
{
    @JsonProperty("status_code")
    private Integer statusCode;
    private String body;

    public Integer getStatusCode() { return statusCode; }
    public void setStatusCode(Integer statusCode) { this.statusCode = statusCode; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
}
