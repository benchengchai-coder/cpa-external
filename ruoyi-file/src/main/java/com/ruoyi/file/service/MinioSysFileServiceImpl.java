package com.ruoyi.file.service;

import java.io.InputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.file.FileUploadUtils;
import com.ruoyi.file.config.MinioConfig;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;

/**
 * Minio 文件存储
 *
 * <p>仅当 file.storage.type=minio 时生效，依赖 {@link MinioConfig} 暴露的 {@link MinioClient}。</p>
 *
 * @author ruoyi
 */
@Service
@ConditionalOnProperty(prefix = "file.storage", name = "type", havingValue = "minio")
public class MinioSysFileServiceImpl implements ISysFileService
{
    @Autowired
    private MinioConfig minioConfig;

    @Autowired
    private MinioClient client;

    /**
     * Minio文件上传接口
     *
     * @param file 上传的文件
     * @return 访问地址
     * @throws Exception
     */
    @Override
    public String uploadFile(MultipartFile file, String directory) throws Exception
    {
        // 按日期目录 + 原文件名 + 序列号生成 object key，与本地存储命名风格一致
        String fileName = FileUploadUtils.extractFilename(file);
        String normalizedDirectory = normalizeDirectory(directory);
        if (StringUtils.isNotEmpty(normalizedDirectory))
        {
            fileName = normalizedDirectory + "/" + fileName;
        }
        try (InputStream inputStream = file.getInputStream())
        {
            PutObjectArgs args = PutObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(fileName)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build();
            client.putObject(args);
            return minioConfig.getUrl() + "/" + minioConfig.getBucketName() + "/" + fileName;
        }
        catch (Exception e)
        {
            throw new RuntimeException("Minio Failed to upload file", e);
        }
    }

    /**
     * Minio文件删除接口
     *
     * @param fileUrl 文件访问URL
     * @throws Exception
     */
    @Override
    public void deleteFile(String fileUrl) throws Exception
    {
        try
        {
            // 取存储桶名称之后的相对路径作为 object key
            String minioFile = StringUtils.substringAfter(fileUrl, minioConfig.getBucketName());
            minioFile = minioFile.replaceFirst("^/+", "");
            client.removeObject(RemoveObjectArgs.builder().bucket(minioConfig.getBucketName()).object(minioFile).build());
        }
        catch (Exception e)
        {
            throw new RuntimeException("Minio Failed to delete file", e);
        }
    }

    private String normalizeDirectory(String directory)
    {
        if (StringUtils.isEmpty(directory))
        {
            return "";
        }
        String normalized = directory.replace('\\', '/').replaceAll("^/+|/+$", "");
        if (normalized.contains(".."))
        {
            throw new IllegalArgumentException("文件存储目录不合法");
        }
        return normalized;
    }
}
