package io.renren.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        SimpleModule dateModule = new SimpleModule();
        dateModule.addDeserializer(Date.class, new CustomDateDeserializer());
        dateModule.addSerializer(Date.class, new CustomDateSerializer());
        mapper.registerModule(dateModule);

        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        mapper.registerModule(javaTimeModule);

        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        return mapper;
    }

    @Bean
    public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter() {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(objectMapper());
        return converter;
    }

    public static class CustomDateDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<Date> {
        private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        private final SimpleDateFormat dateOnlyFormat = new SimpleDateFormat("yyyy-MM-dd");
        private final SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        private final SimpleDateFormat isoFormatNoMillis = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

        @Override
        public Date deserialize(JsonParser p, DeserializationContext ctxt) throws java.io.IOException {
            String dateStr = p.getValueAsString();
            if (dateStr == null || dateStr.isEmpty()) {
                return null;
            }

            try {
                if (dateStr.contains("T") && dateStr.endsWith("Z")) {
                    if (dateStr.contains(".")) {
                        return isoFormat.parse(dateStr);
                    } else {
                        return isoFormatNoMillis.parse(dateStr);
                    }
                }
                if (dateStr.contains("T")) {
                    return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(dateStr);
                }
                if (dateStr.length() == 10) {
                    return dateOnlyFormat.parse(dateStr);
                }
                return dateTimeFormat.parse(dateStr);
            } catch (ParseException e) {
                throw new java.io.IOException("Failed to parse date: " + dateStr, e);
            }
        }
    }

    public static class CustomDateSerializer extends com.fasterxml.jackson.databind.JsonSerializer<Date> {
        private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        @Override
        public void serialize(Date date, JsonGenerator gen, com.fasterxml.jackson.databind.SerializerProvider serializers) throws java.io.IOException {
            if (date == null) {
                gen.writeNull();
            } else {
                gen.writeString(dateTimeFormat.format(date));
            }
        }
    }
}