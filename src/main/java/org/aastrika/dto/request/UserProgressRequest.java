package org.aastrika.dto.request;

import lombok.Getter;
import lombok.Setter;
import org.aastrika.dto.BatchEnrolment;

import java.util.List;

@Getter
@Setter
public class UserProgressRequest {

    private List<BatchEnrolment> batchList;
    private int limit;
    private int offset;

}
