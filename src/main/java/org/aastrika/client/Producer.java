package org.aastrika.client;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@Service
public class Producer {

    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;

    public void push(String topic, Object value) {
        ObjectMapper mapper = new ObjectMapper();
        String message = null;
        try {
            message = mapper.writeValueAsString(value);
            kafkaTemplate.send(topic, message);
        } catch (JsonProcessingException e) {
            log.error(String.valueOf(e));
        }
    }
}