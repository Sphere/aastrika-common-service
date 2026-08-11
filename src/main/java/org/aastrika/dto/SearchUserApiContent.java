package org.aastrika.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchUserApiContent {

    private String id;
    private String userId;
    private String email;
    private String firstName;
    private String lastName;
    private String desc;
    private String channel;
    private String phone;

    private String courseId;
    private String rootOrgId;
    private SunbirdUserProfileDetail profileDetails;

    private List<Map<String, Object>> organisations = null;

    @Override
    public String toString() {
        return "SearchUserApiContent{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", email='" + email + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", desc='" + desc + '\'' +
                ", channel='" + channel + '\'' +
                ", phone='" + phone + '\'' +
                ", rootOrgId='" + rootOrgId + '\'' +
                ", profileDetails=" + profileDetails +
                ", organisations=" + organisations +
                '}';
    }
}

