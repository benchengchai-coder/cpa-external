package com.ruoyi.cpaexternal.apikey.client;

/** CLIProxyAPI 管理接口调用异常。 */
public class CpaManagementException extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    public CpaManagementException(String message)
    {
        super(message);
    }

    public CpaManagementException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
