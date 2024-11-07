package com.motorsport.service;

import com.motorsport.model.Item;
import com.motorsport.model.MercadoLibreResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MotoService {

    @Value("${mercadolibre.api.url}")
    private String baseUrl;

    public Map<String, Map<String, String>> obtenerPromedioPorMarca() {
        RestTemplate restTemplate = new RestTemplate();
        Map<String, Double> totalPorMarca = new HashMap<>();
        Map<String, Integer> conteoPorMarca = new HashMap<>();
        Map<String, Double> totalPesosPorMarca = new HashMap<>();
        Map<String, Integer> conteoPesosPorMarca = new HashMap<>();
        Map<String, Integer> conteoMonedas = new HashMap<>(); // New map to count currencies

        int offset = 0;

        while (true) {
            MercadoLibreResponse response = fetchMotorcycleData(restTemplate, offset);
            if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
                System.out.println("No se encontraron resultados o la respuesta es nula.");
                break; // Exit if no more results
            }

            processItems(response.getResults(), totalPorMarca, conteoPorMarca, totalPesosPorMarca, conteoPesosPorMarca, conteoMonedas);

            offset += response.getResults().size();
            if (offset >= 900) break; // Limit to 900 records
        }

        return calculateAverages(totalPesosPorMarca, conteoPorMarca, conteoMonedas);
    }

    private MercadoLibreResponse fetchMotorcycleData(RestTemplate restTemplate, int offset) {
        String url = baseUrl + "&offset=" + offset;
        return restTemplate.getForObject(url, MercadoLibreResponse.class);
    }

    private void processItems(List<Item> items,
                              Map<String, Double> totalPorMarca,
                              Map<String, Integer> conteoPorMarca,
                              Map<String, Double> totalPesosPorMarca,
                              Map<String, Integer> conteoPesosPorMarca,
                              Map<String, Integer> conteoMonedas) {
        for (Item item : items) {
            String marca = item.getBrand().toLowerCase(); // Convert to lowercase to avoid duplicates
            if (marca == null) continue; // Ignore if brand is null

            double price = item.getPrice();
            String currencyId = item.getCurrencyId(); // Get currency_id

            // Update totals and counts
            updateTotalsAndCounts(marca, price, currencyId,
                    totalPorMarca, conteoPorMarca,
                    totalPesosPorMarca, conteoPesosPorMarca,
                    conteoMonedas);
        }
    }

    private void updateTotalsAndCounts(String marca,
                                       double price,
                                       String currencyId,
                                       Map<String, Double> totalPorMarca,
                                       Map<String, Integer> conteoPorMarca,
                                       Map<String, Double> totalPesosPorMarca,
                                       Map<String, Integer> conteoPesosPorMarca,
                                       Map<String, Integer> conteoMonedas) {
        // Update totals and counts for brands
        totalPorMarca.put(marca, totalPorMarca.getOrDefault(marca, 0.0) + price);
        conteoPorMarca.put(marca, conteoPorMarca.getOrDefault(marca, 0) + 1);

        // Accumulate in pesos
        if (currencyId.equals("ARS")) {
            totalPesosPorMarca.put(marca, totalPesosPorMarca.getOrDefault(marca, 0.0) + price);
            conteoPesosPorMarca.put(marca, conteoPesosPorMarca.getOrDefault(marca, 0) + 1);
            conteoMonedas.put(marca, conteoMonedas.getOrDefault(marca, 0) + 1); // Count ARS
        } else if (currencyId.equals("USD")) {
            totalPesosPorMarca.put(marca, totalPesosPorMarca.getOrDefault(marca, 0.0) + price * 1130);
            conteoPesosPorMarca.put(marca, conteoPesosPorMarca.getOrDefault(marca, 0) + 1);
            conteoMonedas.put(marca + "_USD", conteoMonedas.getOrDefault(marca + "_USD", 0) + 1); // Count USD
        }
    }

    private Map<String, Map<String, String>> calculateAverages(Map<String, Double> totalPesosPorMarca,
                                                               Map<String, Integer> conteoPorMarca,
                                                               Map<String, Integer> conteoMonedas) {
        Map<String, Map<String, String>> promedioConMoneda = new HashMap<>();

        for (String marca : totalPesosPorMarca.keySet()) {
            double total = totalPesosPorMarca.getOrDefault(marca, 0.0); // Use the total in pesos
            int count = conteoPorMarca.get(marca);
            double promedio = count > 0 ? total / count : 0;

            // Determine predominant currency
            String monedaPredominante;
            int arsCount = conteoMonedas.getOrDefault(marca, 0);
            int usdCount = conteoMonedas.getOrDefault(marca + "_USD", 0);

            if (arsCount > usdCount) {
                monedaPredominante = "ARS"; // Predominance in ARS
            } else {
                monedaPredominante = "USD"; // Predominance in USD
                promedio /= 1130; // Convert average back to USD if needed
            }

            // Format average to two decimals
            String promedioFormateado = String.format("%.2f", promedio).replace('.', ','); // Change point to comma

            Map<String, String> detalles = new HashMap<>();
            detalles.put("promedio", promedioFormateado);
            detalles.put("currency", monedaPredominante); // Use the predominant currency

            // Print the average and currency to console
            System.out.println("Promedio de " + marca.substring(0, 1).toUpperCase() + marca.substring(1) + ": " + promedioFormateado + " " + monedaPredominante);

            promedioConMoneda.put(marca.substring(0, 1).toUpperCase() + marca.substring(1), detalles); // Capitalize the first letter of the brand
        }

        return promedioConMoneda;
    }
}