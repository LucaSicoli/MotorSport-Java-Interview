package com.motorsport.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

@Getter
public class MercadoLibreResponse {

    @JsonProperty("results")
    private List<Item> results;

}