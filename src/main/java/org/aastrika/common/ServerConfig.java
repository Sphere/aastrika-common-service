package org.aastrika.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ServerConfig {

    @Value("${sb.service.url}")
    private String sbUrl;

    @Value("${lms.user.update.path}")
    private String lmsUserUpdatePath;

    @Value("${lms.user.read.path}")
    private String lmsUserReadPath;

    @Value("${wf.service.host}")
    private String wfServiceHost;

    @Value("${wf.service.transitionPath}")
    private String wfServiceTransitionPath;

    @Value("${lms.system.settings.path}")
    private String lmsSystemSettingsPath;

    @Value("${es.profile.index.type}")
    private String esProfileIndexType;

    @Value("${es.host.list}")
    private String[] esHostList;

    @Value("${es.username}")
    private String esUser;

    @Value("${es.password}")
    private String esPassword;

    @Value("${sb.es.host.list}")
    private String[] sbEsHostList;

    @Value("${sb.es.username}")
    private String sbEsUser;

    @Value("${sb.es.password}")
    private String sbEsPassword;

    @Value("${es.org.onboarding.index}")
    private String orgOnboardingIndex;

    public String getSbUrl() {
        return sbUrl;
    }

    public String getLmsUserUpdatePath() {
        return lmsUserUpdatePath;
    }

    public String getLmsUserReadPath() {
        return lmsUserReadPath;
    }

    public String getWfServiceHost() {
        return wfServiceHost;
    }

    public String getWfServiceTransitionPath() {
        return wfServiceTransitionPath;
    }

    public String getLmsSystemSettingsPath() {
        return lmsSystemSettingsPath;
    }

    public String getEsProfileIndexType() {
        return esProfileIndexType;
    }

    public String getEsUser() {
        return esUser;
    }

    public String getEsPassword() {
        return esPassword;
    }

    public String[] getEsHostList() {
        return esHostList;
    }

    public String getOrgOnboardingIndex() {
        return orgOnboardingIndex;
    }

    public String[] getSbEsHostList() {
        return sbEsHostList;
    }

    public String getSbEsUser() {
        return sbEsUser;
    }

    public String getSbEsPassword() {
        return sbEsPassword;
    }
}

