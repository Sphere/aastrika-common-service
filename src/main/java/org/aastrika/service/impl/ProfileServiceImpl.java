//package org.aastrika.service.impl;
//
//import org.aastrika.common.Constants;
//import org.aastrika.config.ServerConfig;
//import org.aastrika.dto.response.SBApiResponse;
//import org.aastrika.service.ProfileService;
//import org.apache.commons.lang3.StringUtils;
//import org.opensearch.action.search.SearchResponse;
//import org.opensearch.index.query.BoolQueryBuilder;
//import org.opensearch.index.query.QueryBuilders;
//import org.opensearch.search.SearchHit;
//import org.opensearch.search.builder.SearchSourceBuilder;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.HttpStatus;
//import org.springframework.stereotype.Service;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//@Service
//public class ProfileServiceImpl implements ProfileService {
//
//    @Autowired
//    ServerConfig serverConfig;
//
//    @Autowired
//    IndexerService indexerService;
//
//    @Override
//    public SBApiResponse userAutoComplete(String searchTerm) {
//        SBApiResponse response = new SBApiResponse();
//        response.setResponseCode(HttpStatus.BAD_REQUEST);
//        response.getParams().setStatus(Constants.FAILED);
//        if (StringUtils.isEmpty(searchTerm)) {
//            response.getParams().setErrmsg("Invalid Search Term");
//            return response;
//        }
//
//        response.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
//        Map<String, Object> resultResp = new HashMap<>();
//        System.out.println(response);
//        try {
//            List<Map<String, Object>> userData = getUserSearchData(searchTerm);
//            System.out.println("userData" + userData);
//            resultResp.put(Constants.CONTENT, userData);
//            resultResp.put(Constants.COUNT, userData.size());
//            response.setResponseCode(HttpStatus.OK);
//            response.getParams().setStatus(Constants.SUCCESS);
//            response.put(Constants.RESPONSE, resultResp);
//        } catch (Exception e) {
//            response.getParams().setErrmsg("Failed to get user details from ES. Exception: " + e.getStackTrace());
//        }
//        System.out.println("Came at last");
//        return response;
//    }
//
//    public List<Map<String, Object>> getUserSearchData(String searchTerm) throws Exception {
//        List<Map<String, Object>> resultArray = new ArrayList<>();
//        Map<String, Object> result;
//        final BoolQueryBuilder query = QueryBuilders.boolQuery();
//        for (String field : serverConfig.getEsAutoCompleteSearchFields()) {
//            query.should(QueryBuilders.matchPhrasePrefixQuery(field, searchTerm));
//        }
//        System.out.println(query);
//        final BoolQueryBuilder finalQuery = QueryBuilders.boolQuery();
//        finalQuery.must(QueryBuilders.termQuery(Constants.STATUS, 1)).must(query);
//        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder().query(finalQuery);
//        sourceBuilder.fetchSource(serverConfig.getEsAutoCompleteIncludeFields(), new String[] {});
//        System.out.println("indexname" + serverConfig.getSbEsUserProfileIndex());
//        System.out.println("final query" + finalQuery);
//        SearchResponse searchResponse = indexerService.getEsResult(serverConfig.getSbEsUserProfileIndex(),
//                serverConfig.getEsProfileIndexType(), sourceBuilder, true);
//        System.out.println("source builder" + sourceBuilder);
//        System.out.println("search response" + searchResponse);
//
//        for (SearchHit hit : searchResponse.getHits()) {
//            result = hit.getSourceAsMap();
//            resultArray.add(result);
//        }
//        return resultArray;
//    }
//
//
//    @Override
//    public SBApiResponse migrateUser(Map<String, Object> request, String userToken, String authToken) {
//        return null;
//    }
//
//    @Override
//    public SBApiResponse bulkUpload(MultipartFile mFile, String orgId, String orgName, String userId) {
//        return null;
//    }
//
//    @Override
//    public SBApiResponse getBulkUploadDetails(String orgId) {
//        return null;
//    }
//}
