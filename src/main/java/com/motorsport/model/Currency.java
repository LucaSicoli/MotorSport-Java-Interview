package com.motorsport.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class Currency {

    @JsonProperty("currency_id")
    private String currencyId;
}