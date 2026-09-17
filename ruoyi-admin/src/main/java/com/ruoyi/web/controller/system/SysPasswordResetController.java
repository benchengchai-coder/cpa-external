package com.ruoyi.web.controller.system;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.RateLimiter;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.model.PasswordResetBody;
import com.ruoyi.common.core.domain.model.PasswordResetEmailCodeBody;
import com.ruoyi.common.enums.LimitType;
import com.ruoyi.framework.web.service.SysPasswordResetService;

/**
 * 匿名密码重置接口。
 */
@RestController
public class SysPasswordResetController extends BaseController
{
    @Autowired
    private SysPasswordResetService passwordResetService;

    /**
     * 获取密码重置功能状态。
     */
    @GetMapping("/password/reset/status")
    public AjaxResult status()
    {
        AjaxResult ajax = success();
        ajax.put("resetEnabled", passwordResetService.isResetEnabled());
        ajax.put("captchaEnabled", passwordResetService.isCaptchaEnabled());
        ajax.put("pwdChrtype", passwordResetService.getPasswordChrtype());
        return ajax;
    }

    /**
     * 发送密码重置邮箱验证码。
     */
    @RateLimiter(time = 60, count = 3, limitType = LimitType.IP)
    @PostMapping("/password/reset/email/code")
    public AjaxResult sendEmailCode(@RequestBody PasswordResetEmailCodeBody body)
    {
        passwordResetService.sendEmailCode(body);
        return success();
    }

    /**
     * 重置密码。
     */
    @RateLimiter(time = 60, count = 10, limitType = LimitType.IP)
    @PostMapping("/password/reset")
    public AjaxResult resetPassword(@RequestBody PasswordResetBody body)
    {
        passwordResetService.resetPassword(body);
        return success();
    }
}
