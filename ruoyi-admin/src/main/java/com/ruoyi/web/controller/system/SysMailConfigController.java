package com.ruoyi.web.controller.system;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.web.service.SysMailService;
import com.ruoyi.system.domain.SysMailConfig;
import com.ruoyi.system.service.ISysMailConfigService;

/**
 * 邮件配置 信息操作处理
 */
@RestController
@RequestMapping("/system/mail/config")
public class SysMailConfigController extends BaseController
{
    @Autowired
    private ISysMailConfigService mailConfigService;

    @Autowired
    private SysMailService mailService;

    /**
     * 获取邮件配置
     */
    @PreAuthorize("@ss.hasPermi('system:mail:query')")
    @GetMapping
    public AjaxResult getInfo()
    {
        return success(mailConfigService.selectMailConfig());
    }

    /**
     * 修改邮件配置
     */
    @PreAuthorize("@ss.hasPermi('system:mail:edit')")
    @Log(title = "邮件配置", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SysMailConfig config)
    {
        config.setCreateBy(getUsername());
        config.setUpdateBy(getUsername());
        return toAjax(mailConfigService.saveMailConfig(config));
    }

    /**
     * 发送测试邮件
     */
    @PreAuthorize("@ss.hasPermi('system:mail:test')")
    @Log(title = "邮件配置", businessType = BusinessType.OTHER)
    @PostMapping("/test")
    public AjaxResult test(@RequestBody Map<String, String> body)
    {
        String email = StringUtils.trim(body == null ? null : body.get("email"));
        if (StringUtils.isEmpty(email))
        {
            return error("测试邮箱不能为空");
        }
        mailService.sendTestMail(email);
        return success();
    }
}
