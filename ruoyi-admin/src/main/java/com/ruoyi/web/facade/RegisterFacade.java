package com.ruoyi.web.facade;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import com.ruoyi.common.core.domain.model.UserRegisteredEvent;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.cpaexternal.apikey.service.ICpaApiKeyService;
import com.ruoyi.cpaexternal.subscription.domain.AiSubscriptionConstants;
import com.ruoyi.cpaexternal.subscription.service.IAiSubscriptionService;
import com.ruoyi.system.service.ISysConfigService;

/**
 * 注册流程的服务编排 Facade，处理邀请绑定、新人试用订阅和默认 API Key 生成。
 */
@Component
public class RegisterFacade
{
    private static final Logger log = LoggerFactory.getLogger(RegisterFacade.class);

    @Autowired
    private InviteFacade inviteFacade;

    @Autowired
    private IAiSubscriptionService subscriptionService;

    @Autowired
    private ISysConfigService configService;

    @Autowired
    private ICpaApiKeyService apiKeyService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @EventListener
    @Transactional
    public void onUserRegistered(UserRegisteredEvent event)
    {
        Long userId = event.getUserId();
        try
        {
            inviteFacade.ensureAffiliate(userId);
            inviteFacade.bindInviter(userId, event.getInviteCode());
        }
        catch (Exception e)
        {
            log.error("为新注册用户[{}]绑定邀请返利失败", userId, e);
        }
        grantRegisterTrial(userId);
        ensureDefaultApiKey(userId);
    }

    /**
     * 注册成功即为用户落一把默认 API Key。
     *
     * <p>必须用独立提交事务（REQUIRES_NEW）包裹：向 CLIProxyAPI 推送密钥发生在
     * 事务提交后，若并入外层事务，推送失败的异常会沿事件发布链冒泡导致注册接口
     * 报错（用户实际已创建成功）。独立事务 + try/catch 把失败限制在本步骤内，
     * 差异由镜像对账定时任务自动补推。</p>
     */
    private void ensureDefaultApiKey(Long userId)
    {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        try
        {
            template.executeWithoutResult(status -> apiKeyService.ensureDefault(userId));
        }
        catch (Exception e)
        {
            log.error("为新注册用户[{}]生成默认API Key失败", userId, e);
        }
    }

    private void grantRegisterTrial(Long userId)
    {
        String planIdValue = configService.selectConfigByKey(AiSubscriptionConstants.CONFIG_REGISTER_TRIAL_PLAN_ID);
        if (StringUtils.isEmpty(planIdValue))
        {
            return;
        }
        try
        {
            Long planId = Long.valueOf(planIdValue);
            if (planId > 0)
            {
                subscriptionService.grantRegisterTrial(userId, planId);
            }
        }
        catch (Exception e)
        {
            log.error("为新注册用户[{}]发放试用订阅失败", userId, e);
        }
    }
}
