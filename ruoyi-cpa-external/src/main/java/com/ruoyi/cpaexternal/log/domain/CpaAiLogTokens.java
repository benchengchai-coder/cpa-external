package com.ruoyi.cpaexternal.log.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** CLIProxyAPI 日志中的 tokens 对象。 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CpaAiLogTokens
{
    @JsonProperty("input_tokens")
    private Integer inputTokens;
    @JsonProperty("output_tokens")
    private Integer outputTokens;
    @JsonProperty("reasoning_tokens")
    private Integer reasoningTokens;
    @JsonProperty("cached_tokens")
    private Integer cachedTokens;
    @JsonProperty("cache_read_tokens")
    private Integer cacheReadTokens;
    @JsonProperty("cache_read_tokens_present")
    private Boolean cacheReadTokensPresent;
    @JsonProperty("cache_creation_tokens")
    private Integer cacheCreationTokens;
    @JsonProperty("total_tokens")
    private Integer totalTokens;

    public Integer getInputTokens() { return inputTokens; }
    public void setInputTokens(Integer inputTokens) { this.inputTokens = inputTokens; }
    public Integer getOutputTokens() { return outputTokens; }
    public void setOutputTokens(Integer outputTokens) { this.outputTokens = outputTokens; }
    public Integer getReasoningTokens() { return reasoningTokens; }
    public void setReasoningTokens(Integer reasoningTokens) { this.reasoningTokens = reasoningTokens; }
    public Integer getCachedTokens() { return cachedTokens; }
    public void setCachedTokens(Integer cachedTokens) { this.cachedTokens = cachedTokens; }
    public Integer getCacheReadTokens() { return cacheReadTokens; }
    public void setCacheReadTokens(Integer cacheReadTokens) { this.cacheReadTokens = cacheReadTokens; }
    public Boolean getCacheReadTokensPresent() { return cacheReadTokensPresent; }
    public void setCacheReadTokensPresent(Boolean cacheReadTokensPresent) { this.cacheReadTokensPresent = cacheReadTokensPresent; }
    public Integer getCacheCreationTokens() { return cacheCreationTokens; }
    public void setCacheCreationTokens(Integer cacheCreationTokens) { this.cacheCreationTokens = cacheCreationTokens; }
    public Integer getTotalTokens() { return totalTokens; }
    public void setTotalTokens(Integer totalTokens) { this.totalTokens = totalTokens; }
}
