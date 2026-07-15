package org.aastrika.service.impl;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.aastrika.core.logger.CbExtLogger;
import org.aastrika.model.DeptPublicInfo;
import org.aastrika.model.SunbirdApiResp;
import org.aastrika.model.SunbirdApiRespContent;
import org.aastrika.model.SunbirdApiResultResponse;
import org.aastrika.service.OutboundRequestHandlerServiceImpl;
import org.aastrika.service.PortalService;
import org.aastrika.util.CassandraOperation;
import org.aastrika.util.CbExtServerProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.aastrika.common.Constants;

@Service
public class PortalServiceImpl implements PortalService {

    private CbExtLogger logger = new CbExtLogger(getClass().getName());
    private ObjectMapper mapper = new ObjectMapper();

    @Autowired
    CbExtServerProperties serverConfig;

    @Autowired
    CassandraOperation cassandraOperation;

    @Autowired
    OutboundRequestHandlerServiceImpl outboundRequestHandlerService;

    @Override
    public List<String> getDeptNameList() {
        try {
            List<String> orgNames = new ArrayList<>();
            int count = 0;
            int iterateCount = 0;
            do {
                // request body
                Map<String, Object> requestMap = new HashMap<>();
                requestMap.put(Constants.OFFSET, iterateCount);
                requestMap.put(Constants.LIMIT, 100);
                requestMap.put(Constants.FIELDS,
                        new ArrayList<>(Arrays.asList(Constants.CHANNEL, Constants.IS_MDO, Constants.IS_CBP)));
                requestMap.put(Constants.FILTERS, new HashMap<String, Object>() {
                    {
                        put(Constants.IS_TENANT, Boolean.TRUE);
                    }
                });

                String serviceURL = serverConfig.getSbUrl() + serverConfig.getSbOrgSearchPath();
                SunbirdApiResp orgResponse = mapper.convertValue(
                        outboundRequestHandlerService.fetchResultUsingPost(serviceURL, new HashMap<String, Object>() {
                            {
                                put(Constants.REQUEST, requestMap);
                            }
                        }), SunbirdApiResp.class);

                SunbirdApiResultResponse resultResp = orgResponse.getResult().getResponse();
                count = resultResp.getCount();
                iterateCount = iterateCount + resultResp.getContent().size();
                for (SunbirdApiRespContent content : resultResp.getContent()) {
                    // return orgname only if cbp or mdo
                    if ((!ObjectUtils.isEmpty(content.getIsMdo()) && content.getIsMdo())
                            || (!ObjectUtils.isEmpty(content.getIsCbp()) && content.getIsCbp())) {
                        orgNames.add(content.getChannel());
                    }
                }
            } while (count != iterateCount);
            return orgNames;
        } catch (Exception e) {
            logger.info("Exception occurred in getDeptNameList");
            logger.error(e);
        }
        return Collections.emptyList();
    }

    @Override
    public List<DeptPublicInfo> getAllDept() throws Exception {
        UnsupportedOperationException ex = new UnsupportedOperationException(
                "/portal/getAllDept API is not implemented.");
        logger.error(ex);
        throw ex;
    }

    @Override
    public DeptPublicInfo  searchDept(String deptName) throws Exception {
        UnsupportedOperationException ex = new UnsupportedOperationException(
                "/portal/getAllDept API is not implemented.");
        logger.error(ex);
        throw ex;
    }
}

