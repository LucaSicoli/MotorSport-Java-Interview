package com.motorsport.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class Item {

    @JsonProperty("sale_price")
    private SalePrice salePrice; // clase para manejar sale_price

    @JsonProperty("attributes")
    private List<Attribute> attributes; // Clase adicional para manejar atributos


    public double getPrice() {
        return salePrice != null ? salePrice.getAmount() : 0;
    }

    public String getBrand() {
        for (Attribute attribute : attributes) {
            if ("BRAND".equals(attribute.getId())) {
                return attribute.getValueName();
            }
        }
        return null; //
    }

    public String getCurrencyId() {
        return salePrice != null ? salePrice.getCurrencyId() : null;
    }
}