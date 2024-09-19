package com.eric_eldard.harpocrates.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.eric_eldard.harpocrates.enumeration.Action;
import com.eric_eldard.harpocrates.enumeration.DataType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class DataDefinition
{
    private static final ObjectMapper MAPPER;

    static
    {
        MAPPER = new ObjectMapper();
        MAPPER.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
    }

    @JsonProperty("typ")
    private DataType type;

    @JsonProperty("act")
    private Action action;

    @JsonProperty("pat")
    private String pattern;

    @JsonProperty("dsc")
    private String description;

    @Override
    public String toString()
    {
        return toJson();
    }

    public String toJson()
    {
        try
        {
            return MAPPER.writeValueAsString(this);
        }
        catch (JsonProcessingException ex)
        {
            throw new RuntimeException(STR."Unable to convert data definition to json: [\{this}]", ex);
        }
    }

    public static DataDefinition from(String json)
    {
        try
        {
            return MAPPER.readValue(json, DataDefinition.class);
        }
        catch (JsonProcessingException ex)
        {
            throw new RuntimeException(STR."Unable to convert json to data definition: [\{json}]", ex);
        }
    }
}