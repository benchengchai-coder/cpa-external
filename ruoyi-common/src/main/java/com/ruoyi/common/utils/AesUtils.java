package com.ruoyi.common.utils;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import com.ruoyi.common.exception.ServiceException;

/**
 * AES-GCM 加解密工具。
 */
public class AesUtils
{
    private static final String AES = "AES";

    private static final String AES_GCM = "AES/GCM/NoPadding";

    private static final int IV_LENGTH = 12;

    private static final int TAG_LENGTH_BIT = 128;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public static String encrypt(String plainText, String secret)
    {
        if (StringUtils.isEmpty(plainText))
        {
            return plainText;
        }
        try
        {
            byte[] iv = new byte[IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(AES_GCM);
            cipher.init(Cipher.ENCRYPT_MODE, buildKey(secret), new GCMParameterSpec(TAG_LENGTH_BIT, iv));
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);
            return Base64.getEncoder().encodeToString(byteBuffer.array());
        }
        catch (Exception e)
        {
            throw new ServiceException("敏感信息加密失败");
        }
    }

    public static String decrypt(String cipherText, String secret)
    {
        if (StringUtils.isEmpty(cipherText))
        {
            return cipherText;
        }
        try
        {
            byte[] encrypted = Base64.getDecoder().decode(cipherText);
            ByteBuffer byteBuffer = ByteBuffer.wrap(encrypted);
            byte[] iv = new byte[IV_LENGTH];
            byteBuffer.get(iv);
            byte[] content = new byte[byteBuffer.remaining()];
            byteBuffer.get(content);
            Cipher cipher = Cipher.getInstance(AES_GCM);
            cipher.init(Cipher.DECRYPT_MODE, buildKey(secret), new GCMParameterSpec(TAG_LENGTH_BIT, iv));
            return new String(cipher.doFinal(content), StandardCharsets.UTF_8);
        }
        catch (Exception e)
        {
            throw new ServiceException("敏感信息解密失败，请检查邮件加密密钥");
        }
    }

    private static SecretKeySpec buildKey(String secret) throws Exception
    {
        if (StringUtils.isEmpty(secret))
        {
            throw new ServiceException("邮件加密密钥未配置");
        }
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] key = digest.digest(secret.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(key, AES);
    }
}
