package org.aastrika.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SunbirdApiUserCourse {
    private long dateTime;
    private Object lastReadContentStatus;
    private String enrolledDate;
    private String contentId;
    private String description;
    private String courseLogoUrl;
    private String batchId;
    private Object content;
    private Object contentStatus;
    private Object lastReadContentId;
    private Object certstatus;
    private String courseId;
    private String collectionId;
    private String addedBy;
    private SunbirdApiBatch batch;
    private boolean active;
    private String userId;
    private List<Object> issuedCertificates;
    private Object completionPercentage;
    private String courseName;
    private List<Object> certificates;
    private Object completedOn;
    private int leafNodesCount;
    private int progress;
    private int status;
}

