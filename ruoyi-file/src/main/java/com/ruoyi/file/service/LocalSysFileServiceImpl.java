package com.ruoyi.file.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.utils.file.FileUploadUtils;
import com.ruoyi.common.utils.file.FileUtils;
import com.ruoyi.common.utils.StringUtils;

/**
 * 本地文件存储
 *
 * <p>默认实现（file.storage.type 缺省或为 local 时生效）。复用 aigate 既有的上传路径与
 * 静态资源映射（/profile/**），统一返回相对路径，由前端通过 API 前缀访问。</p>
 *
 * @author ruoyi
 */
@Service
@ConditionalOnProperty(prefix = "file.storage", name = "type", havingValue = "local", matchIfMissing = true)
public class LocalSysFileServiceImpl implements ISysFileService
{
    /**
     * 本地文件上传接口
     *
     * @param file 上传的文件
     * @return 访问地址
     * @throws Exception
     */
    @Override
    public String uploadFile(MultipartFile file, String directory) throws Exception
    {
        String uploadPath = RuoYiConfig.getUploadPath();
        String normalizedDirectory = normalizeDirectory(directory);
        if (StringUtils.isNotEmpty(normalizedDirectory))
        {
            uploadPath = uploadPath + "/" + normalizedDirectory;
        }
        // 上传文件路径，返回形如 /profile/upload/yyyy/MM/dd/xxx.png 的相对路径
        String fileName = FileUploadUtils.upload(uploadPath, file);
        return fileName;
    }

    /**
     * 本地文件删除接口
     *
     * @param fileUrl 文件访问URL
     * @throws Exception
     */
    @Override
    public void deleteFile(String fileUrl) throws Exception
    {
        // 去掉访问地址中的 /profile 前缀（及域名部分），拼回本地 profile 根目录
        String localFile = RuoYiConfig.getProfile() + FileUtils.stripPrefix(fileUrl);
        FileUtils.deleteFile(localFile);
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
