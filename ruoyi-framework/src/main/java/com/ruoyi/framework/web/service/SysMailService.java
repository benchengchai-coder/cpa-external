package com.ruoyi.framework.web.service;

import java.nio.charset.StandardCharsets;
import java.util.Properties;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.SysMailConfig;
import com.ruoyi.system.service.ISysMailConfigService;

/**
 * 动态SMTP邮件发送服务。
 */
@Component
public class SysMailService
{
    @Autowired
    private ISysMailConfigService mailConfigService;

    /**
     * 发送注册邮箱验证码
     */
    public void sendRegisterCode(String email, String code)
    {
        String subject = "注册邮箱验证码";
        String content = "您的注册邮箱验证码为：" + code + "，5分钟内有效。若非本人操作，请忽略此邮件。";
        sendTextMail(email, subject, content);
    }

    /**
     * 发送密码重置邮箱验证码
     */
    public void sendPasswordResetCode(String email, String code)
    {
        String subject = "密码重置验证码";
        String content = "您的密码重置验证码为：" + code + "，5分钟内有效。若非本人操作，请忽略此邮件。";
        sendTextMail(email, subject, content);
    }

    /**
     * 发送测试邮件
     */
    public void sendTestMail(String email)
    {
        sendTextMail(email, "SMTP测试邮件", "这是一封SMTP配置测试邮件。");
    }

    /**
     * 发送纯文本邮件。
     */
    public void sendTextMail(String to, String subject, String content)
    {
        try
        {
            SysMailConfig config = mailConfigService.selectEnabledMailConfig();
            JavaMailSenderImpl sender = buildSender(config);
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            if (StringUtils.isNotEmpty(config.getFromName()))
            {
                helper.setFrom(new InternetAddress(config.getFromEmail(), config.getFromName(), StandardCharsets.UTF_8.name()));
            }
            else
            {
                helper.setFrom(config.getFromEmail());
            }
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, false);
            sender.send(message);
        }
        catch (ServiceException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new ServiceException("邮件发送失败：" + e.getMessage());
        }
    }

    private JavaMailSenderImpl buildSender(SysMailConfig config)
    {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(config.getHost());
        sender.setPort(config.getPort());
        sender.setDefaultEncoding(StandardCharsets.UTF_8.name());
        if (UserConstants.YES.equals(config.getAuthEnable()))
        {
            sender.setUsername(config.getUsername());
            sender.setPassword(mailConfigService.decryptPassword(config));
        }
        Properties properties = sender.getJavaMailProperties();
        properties.put("mail.smtp.auth", Boolean.toString(UserConstants.YES.equals(config.getAuthEnable())));
        properties.put("mail.smtp.ssl.enable", Boolean.toString(UserConstants.YES.equals(config.getSslEnable())));
        properties.put("mail.smtp.starttls.enable", Boolean.toString(UserConstants.YES.equals(config.getStarttlsEnable())));
        properties.put("mail.smtp.connectiontimeout", String.valueOf(config.getTimeout()));
        properties.put("mail.smtp.timeout", String.valueOf(config.getTimeout()));
        properties.put("mail.smtp.writetimeout", String.valueOf(config.getTimeout()));
        return sender;
    }
}
