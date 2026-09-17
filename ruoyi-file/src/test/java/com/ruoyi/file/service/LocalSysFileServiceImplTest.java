package com.ruoyi.file.service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import com.ruoyi.common.config.RuoYiConfig;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalSysFileServiceImplTest
{
    @TempDir
    Path tempDir;

    @Test
    void shouldReturnRelativeProfilePathForLocalFile() throws Exception
    {
        String originalProfile = RuoYiConfig.getProfile();
        RuoYiConfig config = new RuoYiConfig();
        try
        {
            config.setProfile(tempDir.toString());
            LocalSysFileServiceImpl fileService = new LocalSysFileServiceImpl();
            MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png",
                    "avatar-content".getBytes(StandardCharsets.UTF_8));

            String fileUrl = fileService.uploadFile(file);

            assertTrue(fileUrl.startsWith("/profile/upload/"));
            assertTrue(fileUrl.endsWith(".png"));
            assertFalse(fileUrl.startsWith("http://"));
            assertFalse(fileUrl.startsWith("https://"));
        }
        finally
        {
            config.setProfile(originalProfile);
        }
    }
}
