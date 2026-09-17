package com.ruoyi.system.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.AesUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.SysMailConfig;
import com.ruoyi.system.mapper.SysMailConfigMapper;
import com.ruoyi.system.service.ISysMailConfigService;

/**
 * 邮件配置 服务层实现
 */
@Service
public class SysMailConfigServiceImpl implements ISysMailConfigService {
    private static final String DEFAULT_DISABLED = "N";

    private static final int DEFAULT_TIMEOUT = 10000;

    @Autowired
    private SysMailConfigMapper mailConfigMapper;

    @Value("${ruoyi.mail.secret:}")
    private String mailSecret;

    @Override
    public SysMailConfig selectMailConfig() {
        SysMailConfig config = mailConfigMapper.selectMailConfig();
        if (StringUtils.isNull(config)) {
            config = new SysMailConfig();
            config.setEnabled(DEFAULT_DISABLED);
            config.setAuthEnable(UserConstants.YES);
            config.setSslEnable(DEFAULT_DISABLED);
            config.setStarttlsEnable(UserConstants.YES);
            config.setEmailVerifyEnabled(UserConstants.YES);
            config.setTimeout(DEFAULT_TIMEOUT);
        }
        fillPasswordState(config);
        return config;
    }

    @Override
    public SysMailConfig selectEnabledMailConfig() {
        SysMailConfig config = mailConfigMapper.selectMailConfig();
        if (StringUtils.isNull(config) || !UserConstants.YES.equals(config.getEnabled())) {
            throw new ServiceException("邮件服务未启用，请先配置SMTP");
        }
        validateSendConfig(config);
        return config;
    }

    @Override
    public boolean isMailServiceAvailable() {
        try {
            selectEnabledMailConfig();
            return true;
        } catch (ServiceException e) {
            return false;
        }
    }

    @Override
    public int saveMailConfig(SysMailConfig config) {
        if (StringUtils.isNull(config)) {
            throw new ServiceException("邮件配置不能为空");
        }
        SysMailConfig oldConfig = mailConfigMapper.selectMailConfig();
        normalizeConfig(config);
        if (StringUtils.isNotEmpty(config.getPassword())) {
            config.setEncryptedPassword(AesUtils.encrypt(config.getPassword(), mailSecret));
        } else if (StringUtils.isNotNull(oldConfig)) {
            config.setEncryptedPassword(oldConfig.getEncryptedPassword());
        }
        if (StringUtils.isNull(oldConfig)) {
            return mailConfigMapper.insertMailConfig(config);
        }
        config.setConfigId(oldConfig.getConfigId());
        return mailConfigMapper.updateMailConfig(config);
    }

    @Override
    public String decryptPassword(SysMailConfig config) {
        if (StringUtils.isNull(config) || StringUtils.isEmpty(config.getEncryptedPassword())) {
            return "";
        }
        return AesUtils.decrypt(config.getEncryptedPassword(), mailSecret);
    }

    @Override
    public boolean selectEmailVerifyEnabled() {
        SysMailConfig config = mailConfigMapper.selectMailConfig();
        if (StringUtils.isNull(config)) {
            return true;
        }
        return UserConstants.YES.equals(config.getEmailVerifyEnabled());
    }

    private void normalizeConfig(SysMailConfig config) {
        config.setHost(StringUtils.trim(config.getHost()));
        config.setUsername(StringUtils.trim(config.getUsername()));
        config.setFromEmail(StringUtils.lowerCase(StringUtils.trim(config.getFromEmail())));
        config.setFromName(StringUtils.trim(config.getFromName()));
        config.setEnabled(UserConstants.YES.equals(config.getEnabled()) ? UserConstants.YES : DEFAULT_DISABLED);
        config.setAuthEnable(UserConstants.YES.equals(config.getAuthEnable()) ? UserConstants.YES : DEFAULT_DISABLED);
        config.setSslEnable(UserConstants.YES.equals(config.getSslEnable()) ? UserConstants.YES : DEFAULT_DISABLED);
        config.setStarttlsEnable(UserConstants.YES.equals(config.getStarttlsEnable()) ? UserConstants.YES : DEFAULT_DISABLED);
        config.setEmailVerifyEnabled(UserConstants.YES.equals(config.getEmailVerifyEnabled()) ? UserConstants.YES : DEFAULT_DISABLED);
        if (StringUtils.isNull(config.getPort()) || config.getPort() <= 0) {
            config.setPort(25);
        }
        if (StringUtils.isNull(config.getTimeout()) || config.getTimeout() <= 0) {
            config.setTimeout(DEFAULT_TIMEOUT);
        }
    }

    private void validateSendConfig(SysMailConfig config) {
        if (StringUtils.isEmpty(config.getHost()) || StringUtils.isNull(config.getPort()) || config.getPort() <= 0) {
            throw new ServiceException("SMTP服务器和端口未配置");
        }
        if (StringUtils.isEmpty(config.getFromEmail())) {
            throw new ServiceException("发件邮箱未配置");
        }
        if (UserConstants.YES.equals(config.getAuthEnable())
                && (StringUtils.isEmpty(config.getUsername()) || StringUtils.isEmpty(config.getEncryptedPassword()))) {
            throw new ServiceException("SMTP认证信息未配置");
        }
    }

    private void fillPasswordState(SysMailConfig config) {
        boolean hasPassword = StringUtils.isNotEmpty(config.getEncryptedPassword());
        config.setHasPassword(hasPassword);
        config.setPasswordMask(hasPassword ? "******" : "");
        config.setPassword(null);
    }
}
