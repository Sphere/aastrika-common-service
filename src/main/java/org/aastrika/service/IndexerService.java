package org.aastrika.service;


import org.opensearch.action.bulk.BulkRequest;
import org.opensearch.action.bulk.BulkResponse;
import org.opensearch.action.get.GetRequest;
import org.opensearch.action.get.GetResponse;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.index.IndexResponse;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.action.update.UpdateRequest;
import org.opensearch.action.update.UpdateResponse;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.client.core.CountRequest;
import org.opensearch.client.core.CountResponse;
import org.opensearch.core.rest.RestStatus;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class IndexerService {

    private Logger logger = LoggerFactory.getLogger(IndexerService.class);

    @Autowired
    @Qualifier("osClient")
    private RestHighLevelClient esClient;

    @Autowired
    @Qualifier("sbOsClient")
    private RestHighLevelClient sbEsClient;

    /**
     * @param index         name of index
     * @param indexType     index type
     * @param entityId      entity Id
     * @param indexDocument index Document
     * @return status
     */
    public RestStatus addEntity(String index, String indexType, String entityId, Map<String, Object> indexDocument) {
        logger.info("addEntity starts with index {} and entityId {}", index, entityId);
        IndexResponse response = null;
        try {
            if (!StringUtils.isEmpty(entityId)) {
                response = esClient.index(new IndexRequest(index).id(entityId).source(indexDocument),
                        RequestOptions.DEFAULT);
            } else {
                response = esClient.index(new IndexRequest(index).source(indexDocument),
                        RequestOptions.DEFAULT);
            }
        } catch (IOException e) {
            logger.error("Exception in adding record to OpenSearch", e);
        }
        if (null == response)
            return null;
        return response.status();
    }

    /**
     * @param index         name of index
     * @param indexType     index type
     * @param entityId      entity Id
     * @param indexDocument index Document
     * @return status
     */
    public RestStatus updateEntity(String index, String indexType, String entityId, Map<String, ?> indexDocument) {
        logger.info("updateEntity starts with index {} and entityId {}", index, entityId);
        UpdateResponse response = null;
        try {
            response = esClient.update(new UpdateRequest(index.toLowerCase(), entityId).doc(indexDocument),
                    RequestOptions.DEFAULT);
        } catch (IOException e) {
            logger.error("Exception in updating a record to OpenSearch", e);
        }
        if (null == response)
            return null;
        return response.status();
    }

    /**
     * @param index     name of index
     * @param indexType index type
     * @param entityId  entity Id
     * @return status
     */
    public Map<String, Object> readEntity(String index, String indexType, String entityId) {
        logger.info("readEntity starts with index {} and entityId {}", index, entityId);
        GetResponse response = null;
        try {
            response = esClient.get(new GetRequest(index, entityId), RequestOptions.DEFAULT);
        } catch (IOException e) {
            logger.error("Exception in getting the record from OpenSearch", e);
        }
        if (null == response)
            return null;
        return response.getSourceAsMap();
    }

    /**
     * Search the document in OpenSearch based on provided information
     *
     * @param indexName           index name
     * @param type                index type (unused - types are removed in OpenSearch)
     * @param searchSourceBuilder source builder
     * @return search response
     * @throws IOException
     */
    public SearchResponse getEsResult(String indexName, String type, SearchSourceBuilder searchSourceBuilder,
                                      boolean isSunbirdES) throws IOException {
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.indices(indexName);
        searchRequest.source(searchSourceBuilder);
        return getEsResult(searchRequest, isSunbirdES);
    }

    public RestStatus BulkInsert(List<IndexRequest> indexRequestList) {
        BulkResponse restStatus = null;
        if (!CollectionUtils.isEmpty(indexRequestList)) {
            BulkRequest bulkRequest = new BulkRequest();
            indexRequestList.forEach(bulkRequest::add);
            try {
                restStatus = esClient.bulk(bulkRequest, RequestOptions.DEFAULT);
            } catch (IOException e) {
                logger.error("Exception while doing the bulk operation in OpenSearch", e);
            }
        }
        if (null == restStatus)
            return null;
        return restStatus.status();
    }

    public long getDocumentCount(String index, SearchSourceBuilder searchSourceBuilder) {
        try {
            CountRequest countRequest = new CountRequest().indices(index);
            countRequest.source(searchSourceBuilder);
            CountResponse countResponse = esClient.count(countRequest, RequestOptions.DEFAULT);
            return countResponse.getCount();
        } catch (Exception e) {
            logger.error(String.format("Exception in getDocumentCount: %s", e.getMessage()));
            return 0l;
        }
    }

    public long getDocumentCount(String index, boolean isSunbirdES) {
        try {
            CountRequest countRequest = new CountRequest().indices(index);
            if (isSunbirdES) {
                return sbEsClient.count(countRequest, RequestOptions.DEFAULT).getCount();
            } else {
                return esClient.count(countRequest, RequestOptions.DEFAULT).getCount();
            }

        } catch (Exception e) {
            logger.error(String.format("Exception in getDocumentCount: %s", e.getMessage()));
        }
        return 0l;
    }

    private SearchResponse getEsResult(SearchRequest searchRequest, boolean isSbES) throws IOException {
        if (isSbES) {
            return sbEsClient.search(searchRequest, RequestOptions.DEFAULT);
        } else {
            return esClient.search(searchRequest, RequestOptions.DEFAULT);
        }
    }
}
