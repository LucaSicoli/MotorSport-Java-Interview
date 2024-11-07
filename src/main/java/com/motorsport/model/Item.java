package com.motorsport.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class Item {

    @JsonProperty("id")
    private String id;

    @JsonProperty("title")
    private String title;

    @JsonProperty("condition")
    private String condition;

    @JsonProperty("sale_price")
    private SalePrice salePrice; // Nueva clase para manejar sale_price

    @JsonProperty("permalink")
    private String permalink;

    @JsonProperty("attributes")
    private List<Attribute> attributes; // Clase adicional para manejar atributos

    @JsonProperty("thumbnail")
    private String thumbnail;

    public double getPrice() {
        return salePrice != null ? salePrice.getAmount() : 0; // Obtener el precio del sale_price
    }

    public String getBrand() {
        for (Attribute attribute : attributes) {
            if ("BRAND".equals(attribute.getId())) {
                return attribute.getValueName(); // Obtener el nombre de la marca
            }
        }
        return null; // Retornar null si no se encuentra la marca
    }

    public String getCurrencyId() {
        return salePrice != null ? salePrice.getCurrencyId() : null; // Obtener el currency_id
    }
}