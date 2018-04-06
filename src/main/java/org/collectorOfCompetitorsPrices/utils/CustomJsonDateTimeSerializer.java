package org.collectorOfCompetitorsPrices.utils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CustomJsonDateTimeSerializer extends JsonSerializer<Date>
{
	@Override
    public void serialize(Date value, JsonGenerator jgen,
        SerializerProvider provider) throws IOException,JsonProcessingException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        jgen.writeString(format.format(value));
    }

    @Override
    public Class<Date> handledType() {
        return Date.class;
    }

}