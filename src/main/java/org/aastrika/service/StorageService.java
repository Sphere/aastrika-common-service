package org.aastrika.service;


import org.aastrika.dto.response.SBApiResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

public interface StorageService {
    public SBApiResponse uploadFile(MultipartFile file, String containerName) throws IOException;

    SBApiResponse uploadFile(File file, String containerName);

    public SBApiResponse deleteFile(String fileName, String containerName);

    SBApiResponse downloadFile(String fileName);
}