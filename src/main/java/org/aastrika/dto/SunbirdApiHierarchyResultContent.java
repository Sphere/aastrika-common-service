package org.aastrika.dto;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;
import org.aastrika.dto.response.SunbirdApiBatchResp;

@Getter
@Setter
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


    public void setBatches(Object batches) throws JsonParseException, JsonMappingException, IOException {

        if (batches instanceof String) {
            ObjectMapper mapper = new ObjectMapper();
            List<SunbirdApiBatchResp> lst = null;
            // . convert JSON array to List of objects
            lst = Arrays.asList(mapper.readValue(batches.toString(),SunbirdApiBatchResp[].class));
            this.batches = lst;
        }else {
            String jsonInString = new Gson().toJson(batches);
            ObjectMapper mapper = new ObjectMapper();
            List<SunbirdApiBatchResp> lst = Arrays.asList(mapper.readValue(jsonInString.toString(),SunbirdApiBatchResp[].class));
            this.batches = lst;
        }
    }

}

