package com.ruoyi.file.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传/删除接口
 *
 * @author ruoyi
 */
public interface ISysFileService
{
    /**
     * 文件上传接口
     *
     * @param file 上传的文件
     * @return 文件地址；本地存储为相对路径，远程存储可为完整地址
     * @throws Exception
     */
    default String uploadFile(MultipartFile file) throws Exception
    {
        return uploadFile(file, null);
    }

    /**
     * 上传文件到指定业务子目录。
     *
     * @param file 上传的文件
     * @param directory 相对于统一上传目录的业务子目录
     * @return 文件地址；本地存储为相对路径，远程存储可为完整地址
     * @throws Exception 上传异常
     */
    String uploadFile(MultipartFile file, String directory) throws Exception;

    /**
     * 上传内存中的文件内容到指定业务子目录。
     *
     * @param content 文件内容
     * @param originalFilename 原始文件名
     * @param contentType 文件类型
     * @param directory 相对于统一上传目录的业务子目录
     * @return 文件地址；本地存储为相对路径，远程存储可为完整地址
     * @throws Exception 上传异常
     */
    default String uploadFile(byte[] content, String originalFilename, String contentType,
                              String directory) throws Exception
    {
        return uploadFile(new ByteArrayMultipartFile(content, originalFilename, contentType), directory);
    }

    /**
     * 文件删除接口
     *
     * @param fileUrl 文件访问URL
     * @throws Exception
     */
    void deleteFile(String fileUrl) throws Exception;
}
