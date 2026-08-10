package org.aastrika.dto;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SunbirdUserProfileDetail {
    private List<SunbirdUserProfessionalDetail> professionalDetails;
    private Map<String, Object> personalDetails;
}

