package com.ruoyi.cpaexternal.log.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** CLIProxyAPI 日志中的 token_breakdown 对象（v2 计费结构）。 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CpaAiLogTokenBreakdown
{
    @JsonProperty("schema_version")
    private Integer schemaVersion;
    private String quality;
    @JsonProperty("total_tokens")
    private Integer totalTokens;
    private Input input;
    private Output output;
    @JsonProperty("unclassified_tokens")
    private Integer unclassifiedTokens;

    public Integer getSchemaVersion() { return schemaVersion; }
    public void setSchemaVersion(Integer schemaVersion) { this.schemaVersion = schemaVersion; }
    public String getQuality() { return quality; }
    public void setQuality(String quality) { this.quality = quality; }
    public Integer getTotalTokens() { return totalTokens; }
    public void setTotalTokens(Integer totalTokens) { this.totalTokens = totalTokens; }
    public Input getInput() { return input; }
    public void setInput(Input input) { this.input = input; }
    public Output getOutput() { return output; }
    public void setOutput(Output output) { this.output = output; }
    public Integer getUnclassifiedTokens() { return unclassifiedTokens; }
    public void setUnclassifiedTokens(Integer unclassifiedTokens) { this.unclassifiedTokens = unclassifiedTokens; }

    /** 输入 Token 分类明细。 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Input
    {
        @JsonProperty("total_tokens")
        private Integer totalTokens;
        @JsonProperty("uncached_tokens")
        private Integer uncachedTokens;
        @JsonProperty("cache_read_tokens")
        private Integer cacheReadTokens;
        @JsonProperty("cache_write_tokens")
        private Integer cacheWriteTokens;

        public Integer getTotalTokens() { return totalTokens; }
        public void setTotalTokens(Integer totalTokens) { this.totalTokens = totalTokens; }
        public Integer getUncachedTokens() { return uncachedTokens; }
        public void setUncachedTokens(Integer uncachedTokens) { this.uncachedTokens = uncachedTokens; }
        public Integer getCacheReadTokens() { return cacheReadTokens; }
        public void setCacheReadTokens(Integer cacheReadTokens) { this.cacheReadTokens = cacheReadTokens; }
        public Integer getCacheWriteTokens() { return cacheWriteTokens; }
        public void setCacheWriteTokens(Integer cacheWriteTokens) { this.cacheWriteTokens = cacheWriteTokens; }
    }

    /** 输出 Token 分类明细。 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Output
    {
        @JsonProperty("total_tokens")
        private Integer totalTokens;
        @JsonProperty("non_reasoning_tokens")
        private Integer nonReasoningTokens;
        @JsonProperty("reasoning_tokens")
        private Integer reasoningTokens;

        public Integer getTotalTokens() { return totalTokens; }
        public void setTotalTokens(Integer totalTokens) { this.totalTokens = totalTokens; }
        public Integer getNonReasoningTokens() { return nonReasoningTokens; }
        public void setNonReasoningTokens(Integer nonReasoningTokens) { this.nonReasoningTokens = nonReasoningTokens; }
        public Integer getReasoningTokens() { return reasoningTokens; }
        public void setReasoningTokens(Integer reasoningTokens) { this.reasoningTokens = reasoningTokens; }
    }
}
