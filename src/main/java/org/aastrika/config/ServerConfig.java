package org.aastrika.config;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Getter
@Component
public class ServerConfig {

    @Value("${lms.user.read.path}")
    private String lmsUserReadPath;

    @Value("${sb.lern.service.url}")
    private String sbUrl;
    @Value("${es.host.list}")
    private String esHostList;

    @Value("${es.username}")
    private String esUser;

    @Value("${es.password}")
    private String esPassword;

    @Value("${es.profile.index.type}")
    private String esProfileIndexType;

    @Value("${sb.lern.service.assign.role.path}")
    private String sbAssignRolePath;

    @Value("${es.user.auto.complete.search.fields}")
    private String esAutoCompleteSearchFields;

    @Value("${es.user.auto.complete.include.fields}")
    private String esAutoCompleteIncludeFields;

    @Value("${sb.lern.service.user.migrate.path}")
    private String lmsUserMigratePath;

    @Value("${sb.lern.data.sync.path}")
    private String lmsDataSyncPath;

    @Value("${sb.lern.es.host.list}")
    private String sbEsHostList;

    @Value("${sb.lern.es.username}")
    private String sbEsUser;

    @Value("${sb.lern.es.password}")
    private String sbEsPassword;

    @Value("${sb.lern.es.user.profile.index}")
    private String sbEsUserProfileIndex;

    @Value("${user.bulk.upload.container.name}")
    private String bulkUploadContainerName;

    @Value("${cloud.container.name}")
    private String cloudContainerName;

    @Value("${user.bulk.upload.email.notification.list}")
    private String bulkUploadEmailNotificationList;

    @Value("${kafka.topics.user.bulk.upload}")
    private String userBulkUploadTopic;

    @Value("${user.bulk.upload.email.template}")
    private String bulkUploadEmailTemplate;

    @Value("${user.bulk.upload.email.notification.subject}")
    private String bulkUploadEmailNotificationSubject;

    @Value("${user.bulk.upload.status.fields}")
    private String bulkUploadStatusFields;

    @Value("${sb.service.send.notify.email.path}")
    private String sbSendNotificationEmailPath;

    @Value("${cloud.storage.type.name}")
    private String cloudStorageTypeName;

    @Value("${cloud.storage.key}")
    private String cloudStorageKey;

    @Value("${cloud.storage.secret}")
    private String cloudStorageSecret;

    @Value("${cloud.storage.cephs3.endpoint}")
    private String cloudStorageCephs3Endpoint;

    @Value("${sb.lern.api.key}")
    private String sbApiKey;

    @Value("${sb.lern.course.service.host}")
    private String courseServiceHost;

    @Value("${progress.api.endpoint}")
    private String progressReadEndPoint;

    @Value("${content-hierarchy-endpoint}")
    private String hierarchyEndPoint;

    @Value("${content-service-host}")
    private String contentHost;

    @Value("${km.base.host}")
    private String kmBaseHost;

    @Value("${km.base.content.search}")
    private String kmBaseContentSearch;

    public String[] getEsHostList() {
        return esHostList.split(",", -1);
    }
    public List<String> getEsAutoCompleteSearchFields() {
        return Arrays.asList(esAutoCompleteSearchFields.split(",", -1));
    }

    public String[] getEsAutoCompleteIncludeFields() {
        return esAutoCompleteIncludeFields.split(",", -1);
    }

    public String[] getSbEsHostList() {
        return sbEsHostList.split(",", -1);
    }

    public String getCloudContainerName() {
        return cloudContainerName;
    }

    public List<String> getBulkUploadEmailNotificationList() {
        return Arrays.asList(bulkUploadEmailNotificationList.split(",", -1));
    }

    public List<String> getBulkUploadStatusFields() {
        return Arrays.asList(bulkUploadStatusFields.split(",", -1));
    }

}
