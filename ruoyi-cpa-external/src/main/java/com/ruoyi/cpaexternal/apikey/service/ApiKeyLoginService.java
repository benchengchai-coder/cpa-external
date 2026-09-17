package com.ruoyi.cpaexternal.apikey.service;

import java.util.Date;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.security.core.userdetails.UserDetails;
import com.ruoyi.cpaexternal.apikey.domain.CpaApiKey;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.exception.user.BlackListException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.MessageUtils;
import com.ruoyi.common.utils.ip.IpUtils;
import com.ruoyi.framework.manager.AsyncManager;
import com.ruoyi.framework.manager.factory.AsyncFactory;
import com.ruoyi.framework.web.service.TokenService;
import com.ruoyi.framework.web.service.UserDetailsServiceImpl;
import com.ruoyi.system.service.ISysConfigService;
import com.ruoyi.system.service.ISysUserService;

/** API Key 登录服务。 */
@Component
public class ApiKeyLoginService
{
    @Autowired
    private ICpaApiKeyService apiKeyService;

    @Autowired
    private ISysUserService userService;

    @Autowired
    private ISysConfigService configService;

    @Autowired
    private UserDetailsServiceImpl userDetailsServiceImpl;

    @Autowired
    private TokenService tokenService;

    /** 使用 API Key 换取管理端 Token。 */
    public String loginByApiKey(String apiKey)
    {
        if (apiKey == null || apiKey.isBlank())
        {
            throw new ServiceException("密钥不能为空");
        }
        CpaApiKey record = apiKeyService.selectByPlainKey(apiKey);
        if (record == null)
        {
            recordFailure("密钥无效");
            throw new ServiceException("密钥无效");
        }
        if (!"0".equals(record.getStatus()))
        {
            recordFailure("密钥已停用");
            throw new ServiceException("密钥已停用");
        }
        String blackStr = configService.selectConfigByKey("sys.login.blackIPList");
        if (IpUtils.isMatchedIp(blackStr, IpUtils.getIpAddr()))
        {
            recordFailure(MessageUtils.message("login.blocked"));
            throw new BlackListException();
        }
        SysUser user = userService.selectUserById(record.getUserId());
        if (user == null)
        {
            recordFailure("密钥关联用户不存在");
            throw new ServiceException("密钥无效");
        }
        UserDetails userDetails = userDetailsServiceImpl.createLoginUser(user);
        LoginUser loginUser = (LoginUser) userDetails;
        AsyncManager.me().execute(AsyncFactory.recordLogininfor(user.getUserName(), Constants.LOGIN_SUCCESS, "密钥登录成功"));
        userService.updateLoginInfo(loginUser.getUserId(), IpUtils.getIpAddr(), DateUtils.getNowDate());
        return tokenService.createToken(loginUser);
    }

    private void recordFailure(String message)
    {
        AsyncManager.me().execute(AsyncFactory.recordLogininfor("apikey", Constants.LOGIN_FAIL, message));
    }
}
