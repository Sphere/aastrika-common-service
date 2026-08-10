package org.aastrika.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRegistration extends UserRegistrationInfo {
    private String wfId;
    private String priviousStatus;
    private String status;
    private long createdOn;
    private long updatedOn;
    private String createdBy;
    private String updatedBy;
    private String userId;
    private String userName;
    private String proposedDeptName;

    public String toMininumString() {
        StringBuilder strBuilder = new StringBuilder("[ UserRegistrationCode : ");
        strBuilder.append(this.getRegistrationCode()).append(", UserId : ").append(this.getUserId()).append("]");
        return strBuilder.toString();
    }
}
