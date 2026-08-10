package org.aastrika.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SunbirdApiUserCourseResult {
    private List<SunbirdApiUserCourse> courses;

    public List<SunbirdApiUserCourse> getCourses() {
        return courses;
    }
}

