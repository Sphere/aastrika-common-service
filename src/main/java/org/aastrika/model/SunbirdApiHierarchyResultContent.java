package org.aastrika.model;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
//import com.google.gson.Gson;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SunbirdApiHierarchyResultContent {
    private String parent;
    private String identifier;
    private String downloadUrl;
    private String channel;
    private String source;
    private String mimeType;
    private String objectType;
    private String primaryCategory;
    private String artifactUrl;
    private String contentType;
    private String status;
    private String name;
    private String code;
    private String streamingUrl;
    private List<SunbirdApiHierarchyResultContent> children;
    private List<SunbirdApiBatchResp> batches;
    private int leafNodesCount;

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getObjectType() {
        return objectType;
    }

    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }

    public String getPrimaryCategory() {
        return primaryCategory;
    }

    public void setPrimaryCategory(String primaryCategory) {
        this.primaryCategory = primaryCategory;
    }

    public String getArtifactUrl() {
        return artifactUrl;
    }

    public void setArtifactUrl(String artifactUrl) {
        this.artifactUrl = artifactUrl;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getStreamingUrl() {
        return streamingUrl;
    }

    public void setStreamingUrl(String streamingUrl) {
        this.streamingUrl = streamingUrl;
    }

    public List<SunbirdApiHierarchyResultContent> getChildren() {
        return children;
    }

    public void setChildren(List<SunbirdApiHierarchyResultContent> children) {
        this.children = children;
    }

    public List<SunbirdApiBatchResp> getBatches() {
        return batches;
    }

    // public void setBatches(List<SunbirdApiBatchResp> batches) {
    // 	this.batches = batches;
    // }
    @SuppressWarnings("unchecked")
/*    public void setBatches(Object batches) throws JsonParseException, JsonMappingException, IOException {
        List<SunbirdApiBatchResp> lst = null;
         ObjectMapper mapper = new ObjectMapper();
        if (batches instanceof String) {
            lst = Arrays.asList(mapper.readValue(batches.toString(),SunbirdApiBatchResp[].class));
        }else {
            lst = (List<SunbirdApiBatchResp>) batches;
        }
        this.batches = lst;
    } */

    public void setBatches(Object batches) throws JsonParseException, JsonMappingException, IOException {

        if (batches instanceof String) {
            ObjectMapper mapper = new ObjectMapper();
            List<SunbirdApiBatchResp> lst = null;
            // . convert JSON array to List of objects
            lst = Arrays.asList(mapper.readValue(batches.toString(),SunbirdApiBatchResp[].class));
            this.batches = lst;
        }else {
//            String jsonInString = new Gson().toJson(batches);
            ObjectMapper mapper = new ObjectMapper();
//            List<SunbirdApiBatchResp> lst = Arrays.asList(mapper.readValue(jsonInString.toString(),SunbirdApiBatchResp[].class));
//            this.batches = lst;
        }
    }

    public int getLeafNodesCount() {
        return leafNodesCount;
    }

    public void setLeafNodesCount(int leafNodesCount) {
        this.leafNodesCount = leafNodesCount;
    }
}

