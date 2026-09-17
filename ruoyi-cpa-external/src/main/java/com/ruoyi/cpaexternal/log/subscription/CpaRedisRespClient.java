package com.ruoyi.cpaexternal.log.subscription;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.net.SocketFactory;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/** CLIProxyAPI 最小 Redis RESP 接口客户端。 */
final class CpaRedisRespClient implements Closeable
{
    private static final int MAX_LINE_BYTES = 8192;
    private static final int MAX_ARRAY_ITEMS = 1024;

    private final Socket socket;
    private final InputStream input;
    private final OutputStream output;
    private final int maxPayloadBytes;

    private CpaRedisRespClient(Socket socket, int maxPayloadBytes) throws IOException
    {
        this.socket = socket;
        this.input = new BufferedInputStream(socket.getInputStream());
        this.output = new BufferedOutputStream(socket.getOutputStream());
        this.maxPayloadBytes = maxPayloadBytes;
    }

    static CpaRedisRespClient connect(CpaUsageSubscriptionProperties properties) throws IOException
    {
        Socket socket = createSocket(properties);
        boolean success = false;
        try
        {
            socket.connect(new InetSocketAddress(properties.getHost(), properties.getPort()),
                    properties.connectTimeoutMillis());
            socket.setKeepAlive(true);
            socket.setTcpNoDelay(true);
            socket.setSoTimeout(properties.readTimeoutMillis());
            if (socket instanceof SSLSocket sslSocket)
            {
                sslSocket.startHandshake();
            }
            CpaRedisRespClient client = new CpaRedisRespClient(socket, properties.getMaxPayloadBytes());
            success = true;
            return client;
        }
        finally
        {
            if (!success)
            {
                closeQuietly(socket);
            }
        }
    }

    void authenticate(String managementKey) throws IOException
    {
        writeCommand("AUTH", managementKey);
        Object response;
        try
        {
            response = readFrame();
        }
        catch (CpaRedisCommandException exception)
        {
            throw new CpaRedisAuthenticationException(exception.getMessage());
        }
        if (!(response instanceof String value) || !"OK".equalsIgnoreCase(value))
        {
            throw new CpaRedisAuthenticationException("CLIProxyAPI 返回了无效的认证响应");
        }
    }

    void subscribeUsage() throws IOException
    {
        writeCommand("SUBSCRIBE", "usage");
        Object response = readFrame();
        if (!(response instanceof List<?> values) || values.size() != 3
                || !"subscribe".equalsIgnoreCase(stringValue(values.get(0)))
                || !"usage".equalsIgnoreCase(stringValue(values.get(1))))
        {
            throw new IOException("CLIProxyAPI 返回了无效的 usage 订阅响应");
        }
    }

    /** 订阅 CLIProxyAPI 的 errors 实时事件通道；该通道无积压暂存，断线期间的事件不会补发。 */
    void subscribeErrors() throws IOException
    {
        writeCommand("SUBSCRIBE", "errors");
        Object response = readFrame();
        if (!(response instanceof List<?> values) || values.size() != 3
                || !"subscribe".equalsIgnoreCase(stringValue(values.get(0)))
                || !"errors".equalsIgnoreCase(stringValue(values.get(1))))
        {
            throw new IOException("CLIProxyAPI 返回了无效的 errors 订阅响应");
        }
    }

    List<String> popUsage(int count) throws IOException
    {
        writeCommand("LPOP", "usage", Integer.toString(count));
        Object response = readFrame();
        if (!(response instanceof List<?> values))
        {
            throw new IOException("CLIProxyAPI 返回了无效的 usage 队列响应");
        }
        List<String> payloads = new ArrayList<>(values.size());
        for (Object value : values)
        {
            if (!(value instanceof String payload))
            {
                throw new IOException("CLIProxyAPI usage 队列包含非字符串元素");
            }
            payloads.add(payload);
        }
        return payloads;
    }

    void ping() throws IOException
    {
        writeCommand("PING", "cpa-external");
    }

    Object readFrame() throws IOException
    {
        return readRespValue(input, maxPayloadBytes);
    }

    @Override
    public void close() throws IOException
    {
        socket.close();
    }

    static void writeCommand(OutputStream output, String... arguments) throws IOException
    {
        output.write(("*" + arguments.length + "\r\n").getBytes(StandardCharsets.US_ASCII));
        for (String argument : arguments)
        {
            byte[] bytes = argument.getBytes(StandardCharsets.UTF_8);
            output.write(("$" + bytes.length + "\r\n").getBytes(StandardCharsets.US_ASCII));
            output.write(bytes);
            output.write('\r');
            output.write('\n');
        }
        output.flush();
    }

    static Object readRespValue(InputStream input, int maxPayloadBytes) throws IOException
    {
        int prefix = input.read();
        if (prefix < 0)
        {
            throw new EOFException("CLIProxyAPI RESP 连接已关闭");
        }
        return switch (prefix)
        {
            case '+' -> readLine(input);
            case '-' -> throw new CpaRedisCommandException(readLine(input));
            case ':' -> parseLong(readLine(input));
            case '$' -> readBulkString(input, maxPayloadBytes);
            case '*' -> readArray(input, maxPayloadBytes);
            default -> throw new IOException("无法识别的 CLIProxyAPI RESP 类型: " + (char) prefix);
        };
    }

    private void writeCommand(String... arguments) throws IOException
    {
        writeCommand(output, arguments);
    }

    private static Socket createSocket(CpaUsageSubscriptionProperties properties) throws IOException
    {
        if (!properties.isTls())
        {
            return SocketFactory.getDefault().createSocket();
        }
        SSLSocket socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket();
        SSLParameters sslParameters = socket.getSSLParameters();
        sslParameters.setEndpointIdentificationAlgorithm("HTTPS");
        socket.setSSLParameters(sslParameters);
        return socket;
    }

    private static Object readBulkString(InputStream input, int maxPayloadBytes) throws IOException
    {
        long length = parseLong(readLine(input));
        if (length == -1)
        {
            return null;
        }
        if (length < -1 || length > maxPayloadBytes || length > Integer.MAX_VALUE)
        {
            throw new IOException("CLIProxyAPI RESP Bulk String 长度非法: " + length);
        }
        byte[] payload = input.readNBytes((int) length);
        if (payload.length != (int) length)
        {
            throw new EOFException("CLIProxyAPI RESP Bulk String 不完整");
        }
        requireCrlf(input);
        return new String(payload, StandardCharsets.UTF_8);
    }

    private static Object readArray(InputStream input, int maxPayloadBytes) throws IOException
    {
        long length = parseLong(readLine(input));
        if (length == -1)
        {
            return null;
        }
        if (length < -1 || length > MAX_ARRAY_ITEMS)
        {
            throw new IOException("CLIProxyAPI RESP Array 长度非法: " + length);
        }
        List<Object> values = new ArrayList<>((int) length);
        for (int index = 0; index < length; index++)
        {
            values.add(readRespValue(input, maxPayloadBytes));
        }
        return values;
    }

    private static String readLine(InputStream input) throws IOException
    {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int previous = -1;
        while (buffer.size() <= MAX_LINE_BYTES)
        {
            int current = input.read();
            if (current < 0)
            {
                throw new EOFException("CLIProxyAPI RESP 行不完整");
            }
            if (previous == '\r' && current == '\n')
            {
                byte[] bytes = buffer.toByteArray();
                return new String(bytes, 0, bytes.length - 1, StandardCharsets.UTF_8);
            }
            buffer.write(current);
            previous = current;
        }
        throw new IOException("CLIProxyAPI RESP 行超过长度限制");
    }

    private static long parseLong(String value) throws IOException
    {
        try
        {
            return Long.parseLong(value);
        }
        catch (NumberFormatException exception)
        {
            throw new IOException("CLIProxyAPI RESP 整数格式错误", exception);
        }
    }

    private static void requireCrlf(InputStream input) throws IOException
    {
        if (input.read() != '\r' || input.read() != '\n')
        {
            throw new IOException("CLIProxyAPI RESP 缺少 CRLF 结束符");
        }
    }

    private static String stringValue(Object value)
    {
        return value instanceof String text ? text : "";
    }

    private static void closeQuietly(Socket socket)
    {
        try
        {
            socket.close();
        }
        catch (IOException ignored)
        {
            // 连接创建失败时不覆盖原始异常。
        }
    }

    static class CpaRedisCommandException extends IOException
    {
        private static final long serialVersionUID = 1L;

        CpaRedisCommandException(String message)
        {
            super(message);
        }
    }

    static final class CpaRedisAuthenticationException extends CpaRedisCommandException
    {
        private static final long serialVersionUID = 1L;

        CpaRedisAuthenticationException(String message)
        {
            super(message);
        }
    }
}
