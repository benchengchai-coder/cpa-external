package com.ruoyi.web.controller.system;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.RateLimiter;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.model.RegisterBody;
import com.ruoyi.common.enums.LimitType;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.web.service.SysRegisterService;
import com.ruoyi.system.service.ISysConfigService;
import com.ruoyi.system.service.ISysMailConfigService;

/**
 * 注册验证
 *
 * @author ruoyi
 */
@RestController
public class SysRegisterController extends BaseController
{
    @Autowired
    private SysRegisterService registerService;

    @Autowired
    private ISysConfigService configService;

    @Autowired
    private ISysMailConfigService mailConfigService;

    @GetMapping("/register/status")
    public AjaxResult status()
    {
        AjaxResult ajax = success();
        ajax.put("registerEnabled", isRegisterEnabled());
        ajax.put("emailVerifyEnabled", mailConfigService.selectEmailVerifyEnabled());
        return ajax;
    }

    @RateLimiter(time = 60, count = 3, limitType = LimitType.IP)
    @PostMapping("/register/email/code")
    public AjaxResult sendEmailCode(@RequestBody EmailCodeBody body)
    {
        if (!isRegisterEnabled())
        {
            return error("当前系统没有开启注册功能！");
        }
        if (!mailConfigService.selectEmailVerifyEnabled())
        {
            return error("当前系统未开启邮箱验证功能！");
        }
        String msg = registerService.sendEmailCode(StringUtils.isNull(body) ? null : body.getEmail());
        return StringUtils.isEmpty(msg) ? success() : error(msg);
    }

    @PostMapping("/register")
    public AjaxResult register(@RequestBody RegisterBody user)
    {
        if (!isRegisterEnabled())
        {
            return error("当前系统没有开启注册功能！");
        }
        String msg = registerService.register(user);
        return StringUtils.isEmpty(msg) ? success() : error(msg);
    }

    private boolean isRegisterEnabled()
    {
        return "true".equals(configService.selectConfigByKey("sys.account.registerUser"));
    }

    public static class EmailCodeBody
    {
        private String email;

        public String getEmail()
        {
            return email;
        }

        public void setEmail(String email)
        {
            this.email = email;
        }
    }
}
