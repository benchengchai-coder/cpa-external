package com.ruoyi.cpaexternal.billing.service.impl;

import java.sql.SQLException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Component;
import com.ruoyi.cpaexternal.billing.config.CpaBillingProperties;

/** CLIProxyAPI 计费数据库死锁即时重试执行器。 */
@Component
public class CpaBillingTransactionRetryExecutor
{
    private static final Logger log = LoggerFactory.getLogger(CpaBillingTransactionRetryExecutor.class);

    private static final int MYSQL_DEADLOCK_ERROR_CODE = 1213;

    private static final String TRANSACTION_ROLLBACK_SQL_STATE = "40001";

    @Autowired
    private CpaBillingProperties billingProperties;

    public void execute(String operation, Runnable action)
    {
        execute(operation, () ->
        {
            action.run();
            return null;
        });
    }

    public <T> T execute(String operation, Supplier<T> action)
    {
        int maxAttempts = Math.max(1, billingProperties.getTransactionDeadlockMaxAttempts());
        RuntimeException lastException = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++)
        {
            try
            {
                return action.get();
            }
            catch (RuntimeException e)
            {
                if (!isDeadlock(e) || attempt >= maxAttempts)
                {
                    throw e;
                }
                lastException = e;
                long delayMs = calculateDelayMs(attempt);
                log.warn("CLIProxyAPI计费事务发生死锁，准备重试: operation={}, attempt={}/{}, delayMs={}",
                    operation, attempt, maxAttempts, delayMs);
                sleep(delayMs);
            }
        }
        throw lastException;
    }

    private boolean isDeadlock(Throwable throwable)
    {
        Throwable current = throwable;
        while (current != null)
        {
            if (current instanceof CannotAcquireLockException
                || current instanceof PessimisticLockingFailureException)
            {
                return true;
            }
            if (current instanceof SQLException)
            {
                SQLException sqlException = (SQLException) current;
                if (sqlException.getErrorCode() == MYSQL_DEADLOCK_ERROR_CODE
                    || TRANSACTION_ROLLBACK_SQL_STATE.equals(sqlException.getSQLState()))
                {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private long calculateDelayMs(int attempt)
    {
        long baseDelayMs = billingProperties.getTransactionDeadlockBaseDelayMs();
        long maxDelayMs = billingProperties.getTransactionDeadlockMaxDelayMs();
        long delayMs = baseDelayMs;
        for (int i = 1; i < attempt && delayMs < maxDelayMs; i++)
        {
            delayMs = Math.min(maxDelayMs, delayMs * 3L);
        }
        long jitterBound = Math.max(2L, delayMs / 5L + 1L);
        long jitterMs = ThreadLocalRandom.current().nextLong(jitterBound);
        return Math.min(maxDelayMs, delayMs + jitterMs);
    }

    private void sleep(long delayMs)
    {
        try
        {
            Thread.sleep(delayMs);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("CLIProxyAPI计费死锁重试等待被中断", e);
        }
    }
}
