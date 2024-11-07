package com.motorsport.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class SalePrice {

    @JsonProperty("amount")
    private double amount;

    @JsonProperty("currency_id")
    private String currencyId; // Almacenar currency_id como String

}