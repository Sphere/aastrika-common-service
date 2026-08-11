package org.aastrika.dto;


import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class LastLoginInfo {

    String userId;
    Long orgId;
    Date loginTime;
    String orgName;

    @Override
    public String toString() {
        return "LastLoginInfo [userId=" + userId + ", orgId=" + orgId + ", loginTime=" + loginTime + ", orgName="
                + orgName + "]";
    }

    public LastLoginInfo(String userId, Long orgId, Date loginTime, String orgName) {
        super();
        this.userId = userId;
        this.orgId = orgId;
        this.loginTime = loginTime;
        this.orgName = orgName;
    }

}
