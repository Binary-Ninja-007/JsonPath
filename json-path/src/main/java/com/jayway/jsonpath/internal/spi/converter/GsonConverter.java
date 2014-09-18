package com.jayway.jsonpath.internal.spi.converter;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.internal.spi.json.GsonJsonProvider;
import com.jayway.jsonpath.spi.converter.ConversionException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GsonConverter extends ConverterBase {

    public GsonConverter() {
        register(JsonPrimitive.class, Integer.class);
        register(JsonPrimitive.class, Long.class);
        register(JsonPrimitive.class, Float.class);
        register(JsonPrimitive.class, Double.class);
        register(JsonPrimitive.class, BigDecimal.class);
        register(JsonPrimitive.class, BigInteger.class);
        register(JsonPrimitive.class, Date.class);

        register(JsonPrimitive.class, String.class);

        register(JsonPrimitive.class, Boolean.class);

        register(JsonArray.class, List.class);
        register(JsonObject.class, Map.class);
    }

    @Override
    public Object convert(Object src, Class<?> srcType, Class<?> targetType, Configuration conf) {

        assertValidConversion(src, srcType, targetType);

        if (src == null || src.getClass().equals(JsonNull.class)) {
            return null;
        }

        if (JsonPrimitive.class.isAssignableFrom(srcType)) {

            JsonPrimitive primitive = (JsonPrimitive) src;
            if (targetType.equals(Long.class)) {
                return primitive.getAsLong();
            } else if (targetType.equals(Integer.class)) {
                return primitive.getAsInt();
            } else if (targetType.equals(BigInteger.class)) {
                return primitive.getAsBigInteger();
            } else if (targetType.equals(Byte.class)) {
                return primitive.getAsByte();
            } else if (targetType.equals(BigDecimal.class)) {
                return primitive.getAsBigDecimal();
            } else if (targetType.equals(Double.class)) {
                return primitive.getAsDouble();
            } else if (targetType.equals(Float.class)) {
                return primitive.getAsFloat();
            } else if (targetType.equals(String.class)) {
                return primitive.getAsString();
            } else if (targetType.equals(Boolean.class)) {
                return primitive.getAsBoolean();
            } else if (targetType.equals(Date.class)) {

                if(primitive.isNumber()){
                    return new Date(primitive.getAsLong());
                } else if(primitive.isString()){
                    try {
                        return DateFormat.getInstance().parse(primitive.getAsString());
                    } catch (ParseException e) {
                        throw new ConversionException(e);
                    }
                }
            }


        } else if (JsonObject.class.isAssignableFrom(srcType)) {
            JsonObject srcObject = (JsonObject) src;
            if(targetType.equals(Map.class)){
                Map<String, Object> targetMap = new LinkedHashMap<String, Object>();
                for (Map.Entry<String,JsonElement> entry : srcObject.entrySet()) {
                    Object val = null;
                    JsonElement element = entry.getValue();
                    if(element.isJsonPrimitive()) {
                        val = GsonJsonProvider.unwrap(element);
                    } else if(element.isJsonArray()){
                        val = convert(element, element.getClass(), List.class, conf);
                    } else if(element.isJsonObject()){
                        val = convert(element, element.getClass(), Map.class, conf);
                    } else if(element.isJsonNull()){
                        val = null;
                    }
                    targetMap.put(entry.getKey(), val);
                }
                return targetMap;
            }

        } else if (JsonArray.class.isAssignableFrom(srcType)) {
            JsonArray srcArray = (JsonArray) src;
            if(targetType.equals(List.class)){
                List<Object> targetList = new ArrayList<Object>();
                for (JsonElement element : srcArray) {
                    if(element.isJsonPrimitive()) {
                        targetList.add(GsonJsonProvider.unwrap(element));
                    } else if(element.isJsonArray()){
                        targetList.add(convert(element, element.getClass(), List.class, conf));
                    } else if(element.isJsonObject()){
                        targetList.add(convert(element, element.getClass(), Map.class, conf));
                    } else if(element.isJsonNull()){
                        targetList.add(null);
                    }
                }
                return targetList;
            }
        }

        return null;
    }
}
