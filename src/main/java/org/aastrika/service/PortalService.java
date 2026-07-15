package org.aastrika.service;

import org.aastrika.model.DeptPublicInfo;

import java.util.List;

public interface PortalService {
    List<String> getDeptNameList();

    List<DeptPublicInfo> getAllDept() throws Exception;

    DeptPublicInfo searchDept(String deptName) throws Exception;
}
