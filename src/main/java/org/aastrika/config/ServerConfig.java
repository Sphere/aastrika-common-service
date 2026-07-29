package org.aastrika.config;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
public class ServerConfig {

    @Value("${lms.user.read.path}")
    private String lmsUserReadPath;

    @Value("${sb.service.url}")
    private String sbUrl;
    @Value("${es.host.list}")
    private String esHostList;

    @Value("${es.username}")
    private String esUser;

    @Value("${es.password}")
    private String esPassword;

    @Value("${es.profile.index.type}")
    private String esProfileIndexType;

    @Value("${sb.service.assign.role.path}")
    private String sbAssignRolePath;

    @Value("${es.user.auto.complete.search.fields}")
    private String esAutoCompleteSearchFields;

    @Value("${es.user.auto.complete.include.fields}")
    private String esAutoCompleteIncludeFields;

    @Value("${sb.service.user.migrate.path}")
    private String lmsUserMigratePath;

    @Value("${sb.data.sync.path}")
    private String lmsDataSyncPath;

    @Value("${sb.es.host.list}")
    private String sbEsHostList;

    @Value("${sb.es.username}")
    private String sbEsUser;

    @Value("${sb.es.password}")
    private String sbEsPassword;

    @Value("${sb.es.user.profile.index}")
    private String sbEsUserProfileIndex;

    public String getLmsUserReadPath() {
        return lmsUserReadPath;
    }

    public String getSbUrl() {
        return sbUrl;
    }


    public String[] getEsHostList() {
        return esHostList.split(",", -1);
    }

    public String getEsUser() {
        return esUser;
    }

    public String getEsPassword() {
        return esPassword;
    }

    public String getEsProfileIndexType() {
        return esProfileIndexType;
    }


    public String getSbAssignRolePath() {
        return sbAssignRolePath;
    }

    public String getLmsUserMigratePath() {
        return lmsUserMigratePath;
    }


    public String getLmsDataSyncPath() {
        return lmsDataSyncPath;
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

    public String getSbEsUser() {
        return sbEsUser;
    }

    public String getSbEsPassword() {
        return sbEsPassword;
    }


    public String getSbEsUserProfileIndex() {
        return sbEsUserProfileIndex;
    }


}