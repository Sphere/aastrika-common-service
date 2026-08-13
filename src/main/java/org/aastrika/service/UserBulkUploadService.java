package org.aastrika.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aastrika.common.Constants;
import org.aastrika.config.ServerConfig;
import org.aastrika.dao.CassandraDao;
import org.aastrika.dto.UserRegistration;
import org.aastrika.dto.response.SBApiResponse;
import org.aastrika.util.ProjectUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.NumberToTextConverter;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.*;

@Service
public class UserBulkUploadService {
    private Logger logger = LoggerFactory.getLogger(UserBulkUploadService.class);
    ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    ServerConfig serverProperties;
    @Autowired
    UserUtilityService userUtilityService;
    @Autowired
    CassandraDao cassandraDao;

    @Autowired
    StorageService storageService;

    public void initiateUserBulkUploadProcess(String inputData) {
        logger.info("UserBulkUploadService:: initiateUserBulkUploadProcess: Started");
        long duration = 0;
        long startTime = System.currentTimeMillis();
        try {
            HashMap<String, String> inputDataMap = objectMapper.readValue(inputData,
                    new TypeReference<HashMap<String, String>>() {
                    });
            List<String> errList = validateReceivedKafkaMessage(inputDataMap);
            if (errList.isEmpty()) {
                updateUserBulkUploadStatus(inputDataMap.get(Constants.ROOT_ORG_ID),
                        inputDataMap.get(Constants.IDENTIFIER), Constants.STATUS_IN_PROGRESS_UPPERCASE, 0, 0, 0);
                storageService.downloadFile(inputDataMap.get(Constants.FILE_NAME));
                processBulkUpload(inputDataMap);
            } else {
                logger.error(String.format("Error in the Kafka Message Received : %s", errList));
            }
        } catch (Exception e) {
            logger.error(String.format("Error in the scheduler to upload bulk users %s", e.getMessage()),
                    e);
        }
        duration = System.currentTimeMillis() - startTime;
        logger.info("UserBulkUploadService:: initiateUserBulkUploadProcess: Completed. Time taken: "
                + duration + " milli-seconds");
    }

    public void updateUserBulkUploadStatus(String rootOrgId, String identifier, String status, int totalRecordsCount,
                                           int successfulRecordsCount, int failedRecordsCount) {
        try {
            // CassandraDaoImpl doesn't distinguish INSERT vs UPDATE - in Cassandra both are
            // upserts that only touch the columns supplied, so a single map containing the
            // partition/clustering keys plus the fields to update is sufficient.
            Map<String, Object> row = new HashMap<>();
            row.put(Constants.ROOT_ORG_ID_LOWER, rootOrgId);
            row.put(Constants.IDENTIFIER, identifier);
            if (!status.isEmpty()) {
                row.put(Constants.STATUS, status);
            }
            if (totalRecordsCount >= 0) {
                row.put(Constants.TOTAL_RECORDS, totalRecordsCount);
            }
            if (successfulRecordsCount >= 0) {
                row.put(Constants.SUCCESSFUL_RECORDS_COUNT, successfulRecordsCount);
            }
            if (failedRecordsCount >= 0) {
                row.put(Constants.FAILED_RECORDS_COUNT, failedRecordsCount);
            }
            row.put(Constants.DATE_UPDATE_ON, Instant.now());
            cassandraDao.insert(Constants.KEYSPACE_SUNBIRD, Constants.TABLE_USER_BULK_UPLOAD, row);
        } catch (Exception e) {
            logger.error(String.format("Error in Updating User Bulk Upload Status in Cassandra %s", e.getMessage()), e);
        }
    }

    private void processBulkUpload(HashMap<String, String> inputDataMap) throws IOException {
        File file = null;
        FileInputStream fis = null;
        XSSFWorkbook wb = null;
        int totalRecordsCount = 0;
        int noOfSuccessfulRecords = 0;
        int failedRecordsCount = 0;
        String status = "";
        String phone = "";
        try {
            file = new File(Constants.LOCAL_BASE_PATH + inputDataMap.get(Constants.FILE_NAME));
            if (file.exists() && file.length() > 0) {
                fis = new FileInputStream(file);
                wb = new XSSFWorkbook(fis);
                XSSFSheet sheet = wb.getSheetAt(0);
                Iterator<Row> rowIterator = sheet.iterator();
                // incrementing the iterator inorder to skip the headers in the first row
                if (rowIterator.hasNext()) {
                    rowIterator.next();
                }
                while (rowIterator.hasNext()) {
                    Row nextRow = rowIterator.next();
                    if (nextRow.getCell(0) == null) {
                        break;
                    }
                    UserRegistration userRegistration = new UserRegistration();
                    userRegistration.setFirstName(nextRow.getCell(0).getStringCellValue());
                    userRegistration.setLastName(nextRow.getCell(1).getStringCellValue());
                    userRegistration.setEmail(nextRow.getCell(2).getStringCellValue());
                    if (nextRow.getCell(3).getCellType() == CellType.NUMERIC) {
                        phone = NumberToTextConverter.toText(nextRow.getCell(3).getNumericCellValue());
                    }
                    userRegistration.setPhone(phone);
                    userRegistration.setOrgName(inputDataMap.get(Constants.ORG_NAME));
                    System.out.println(" User registration data");
                    System.out.println(userRegistration);
                    List<String> errList = validateEmailContactAndDomain(userRegistration);
                    Cell statusCell = nextRow.getCell(4);
                    Cell errorDetails = nextRow.getCell(5);
                    if (statusCell == null) {
                        statusCell = nextRow.createCell(4);
                    }
                    if (errorDetails == null) {
                        errorDetails = nextRow.createCell(5);
                    }
                    totalRecordsCount++;
                    System.out.println(errList);
                    if (errList.isEmpty()) {
                        System.out.println(" Inside error list");
                        boolean isUserCreated = userUtilityService.createUser(userRegistration);
                        System.out.println(" isusercreated");
                        System.out.println(isUserCreated);

                        if (isUserCreated) {
                            noOfSuccessfulRecords++;
                            statusCell.setCellValue(Constants.SUCCESS.toUpperCase());
                            errorDetails.setCellValue("");
                        } else {
                            failedRecordsCount++;
                            statusCell.setCellValue(Constants.FAILED.toUpperCase());
                            errorDetails.setCellValue(Constants.USER_CREATION_FAILED);
                        }
                    } else {
                        failedRecordsCount++;
                        statusCell.setCellValue(Constants.FAILED.toUpperCase());
                        errorDetails.setCellValue(errList.toString());
                    }
                }
                status = uploadTheUpdatedFile(inputDataMap.get(Constants.ROOT_ORG_ID),
                        inputDataMap.get(Constants.IDENTIFIER), file, wb);
                if (!(Constants.SUCCESSFUL.equalsIgnoreCase(status) && failedRecordsCount == 0
                        && totalRecordsCount == noOfSuccessfulRecords && totalRecordsCount >= 1)) {
                    status = Constants.FAILED_UPPERCASE;
                }
            } else {
                logger.info("Error in Process Bulk Upload : The File is not downloaded/present");
                status = Constants.FAILED_UPPERCASE;
            }
            updateUserBulkUploadStatus(inputDataMap.get(Constants.ROOT_ORG_ID), inputDataMap.get(Constants.IDENTIFIER),
                    status, totalRecordsCount, noOfSuccessfulRecords, failedRecordsCount);
        } catch (Exception e) {
            logger.error(String.format("Error in Process Bulk Upload %s", e.getMessage()), e);
            updateUserBulkUploadStatus(inputDataMap.get(Constants.ROOT_ORG_ID), inputDataMap.get(Constants.IDENTIFIER),
                    Constants.FAILED.toUpperCase(), 0, 0, 0);
        } finally {
            if (wb != null)
                wb.close();
            if (fis != null)
                fis.close();
            if (file != null)
                file.delete();
        }
    }

    private String uploadTheUpdatedFile(String rootOrgId, String identifier, File file, XSSFWorkbook wb)
            throws IOException {
        FileOutputStream fileOut = new FileOutputStream(file);
        wb.write(fileOut);
        fileOut.close();
        SBApiResponse uploadResponse = storageService.uploadFile(file, serverProperties.getBulkUploadContainerName());
        if (!HttpStatus.OK.equals(uploadResponse.getResponseCode())) {
            logger.info(String.format("Failed to upload file. Error: %s",
                    uploadResponse.getParams().getErrmsg()));
            return Constants.FAILED_UPPERCASE;
        }
        return Constants.SUCCESSFUL_UPPERCASE;
    }

    private List<String> validateEmailContactAndDomain(UserRegistration userRegistration) throws Exception {
        StringBuffer str = new StringBuffer();
        List<String> errList = new ArrayList<>();
        if (!userUtilityService.isDomainAccepted(userRegistration.getEmail())) {
            errList.add("Domain not accepted");
        }
        if (!ProjectUtil.validateFirstName(userRegistration.getFirstName())) {
            errList.add("Invalid First Name");
        }
        if (!ProjectUtil.validateLastName(userRegistration.getLastName())) {
            errList.add("Invalid Last Name");
        }
        if (!ProjectUtil.validateEmailPattern(userRegistration.getEmail())) {
            errList.add("Invalid Email Id");
        }
        if (!ProjectUtil.validateContactPattern(userRegistration.getPhone())) {
            errList.add("Invalid Mobile Number");
        }
        if (userUtilityService.isUserExist(Constants.EMAIL, userRegistration.getEmail())) {
            errList.add(Constants.EMAIL_EXIST_ERROR);
        }
        if (userUtilityService.isUserExist(Constants.PHONE, String.valueOf(userRegistration.getPhone()))) {
            errList.add(Constants.MOBILE_NUMBER_EXIST_ERROR);
        }

        if (!errList.isEmpty()) {
            str.append("Failed to Validate User Details. Error Details - [").append(errList).append("]");
        }
        return errList;
    }

    private List<String> validateReceivedKafkaMessage(HashMap<String, String> inputDataMap) {
        StringBuffer str = new StringBuffer();
        List<String> errList = new ArrayList<>();
        if (StringUtils.isEmpty(inputDataMap.get(Constants.ROOT_ORG_ID))) {
            errList.add("RootOrgId is not present");
        }
        if (StringUtils.isEmpty(inputDataMap.get(Constants.IDENTIFIER))) {
            errList.add("Identifier is not present");
        }
        if (StringUtils.isEmpty(inputDataMap.get(Constants.FILE_NAME))) {
            errList.add("Filename is not present");
        }
        if (StringUtils.isEmpty(inputDataMap.get(Constants.ORG_NAME))) {
            errList.add("Orgname is not present");
        }
        if (!errList.isEmpty()) {
            str.append("Failed to Validate User Details. Error Details - [").append(errList.toString()).append("]");
        }
        return errList;
    }

}