package org.aastrika.service.impl;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.aastrika.common.Constants;
import org.aastrika.config.ServerConfig;
import org.aastrika.dto.response.SBApiResponse;
import org.aastrika.service.StorageService;
import org.aastrika.util.ProjectUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.sunbird.cloud.storage.BaseStorageService;
import org.sunbird.cloud.storage.factory.StorageConfig;
import org.sunbird.cloud.storage.factory.StorageServiceFactory;
import scala.Option;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class StorageServiceImpl implements StorageService {

    private BaseStorageService storageService = null;

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    private ServerConfig serverProperties;

    @PostConstruct
    public void init() {
        if (storageService == null) {
            storageService = StorageServiceFactory.getStorageService(new StorageConfig(
                    serverProperties.getCloudStorageTypeName(), serverProperties.getCloudStorageKey(),
                    serverProperties.getCloudStorageSecret(), Option.apply(serverProperties.getCloudStorageCephs3Endpoint())));
        }
    }

    @Override
    public SBApiResponse uploadFile(MultipartFile mFile, String containerName) {
        SBApiResponse response = ProjectUtil.createDefaultResponse(Constants.API_FILE_UPLOAD);
        File file = null;
        try {
            file = new File(System.currentTimeMillis() + "_" + mFile.getOriginalFilename());
            file.createNewFile();
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(mFile.getBytes());
            fos.close();
            return uploadFile(file, containerName);
        } catch (Exception e) {
            log.error("Failed to upload file. Exception: ", e);
            response.getParams().setStatus(Constants.FAILED);
            response.getParams().setErrmsg("Failed to upload file. Exception: " + e.getMessage());
            response.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
            return response;
        } finally {
            if (file != null) {
                file.delete();
            }
        }
    }

    @Override
    public SBApiResponse uploadFile(File file, String containerName) {
        SBApiResponse response = ProjectUtil.createDefaultResponse(Constants.API_FILE_UPLOAD);
        try {
            String objectKey = containerName + "/" + file.getName();
            String url = storageService.upload(serverProperties.getCloudContainerName(), file.getAbsolutePath(),
                    objectKey, Option.apply(false), Option.apply(1), Option.apply(5), Option.empty());
            Map<String, String> uploadedFile = new HashMap<>();
            uploadedFile.put(Constants.NAME, file.getName());
            uploadedFile.put(Constants.URL, url);
            response.getResult().putAll(uploadedFile);
            return response;
        } catch (Exception e) {
            log.error("Failed to upload file. Exception: ", e);
            response.getParams().setStatus(Constants.FAILED);
            response.getParams().setErrmsg("Failed to upload file. Exception: " + e.getMessage());
            response.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
            return response;
        } finally {
            if (file != null) {
                file.delete();
            }
        }
    }

    @Override
    public SBApiResponse deleteFile(String fileName, String containerName) {
        SBApiResponse response = new SBApiResponse();
        response.setId(Constants.API_FILE_DELETE);
        try {
            String objectKey = serverProperties.getCloudContainerName() + "/" + fileName;
            storageService.deleteObject(serverProperties.getCloudContainerName(), objectKey,
                    Option.apply(Boolean.FALSE));
            response.getParams().setStatus(Constants.SUCCESSFUL);
            response.setResponseCode(HttpStatus.OK);
            return response;
        } catch (Exception e) {
            log.error("Failed to delete file: " + fileName + ", Exception: ", e);
            response.getParams().setStatus(Constants.FAILED);
            response.getParams().setErrmsg("Failed to delete file. Exception: " + e.getMessage());
            response.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
            return response;
        }
    }

    @Override
    public SBApiResponse downloadFile(String fileName) {
        log.info("About to call createDefaultResponse");
        SBApiResponse response = ProjectUtil.createDefaultResponse(Constants.API_FILE_DOWNLOAD);
        log.info("createDefaultResponse returned: " + response);
        try {

            String objectKey = serverProperties.getBulkUploadContainerName() + "/" + fileName;
            log.info("Attempting download - container: {}, key: {}",
                    serverProperties.getCloudContainerName(), objectKey);
            storageService.download(serverProperties.getCloudContainerName(), objectKey, Constants.LOCAL_BASE_PATH,
                    Option.apply(Boolean.FALSE));
            return response;
        } catch (Exception e) {
            log.error("Failed to download the file: " + fileName + ", Exception: ", e);
            response.getParams().setStatus(Constants.FAILED);
            response.getParams().setErrmsg("Failed to download the file. Exception: " + e.getMessage());
            response.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
            return response;
        }
    }


}
