package com.motorsport.service;

import com.motorsport.model.Item;
import com.motorsport.model.MercadoLibreResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class MotoService implements MotoServiceInterface {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    @Autowired
    public MotoService(RestTemplate restTemplate, @Value("${mercadolibre.api.url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    @Override
    public Map<String, Map<String, String>> obtenerPromedioPorMarca() {
        List<Item> items = fetchAllMotorcycleData();
        return calculateAverages(processItems(items));
    }

    private List<Item> fetchAllMotorcycleData() {
        int totalItems = 900; // Total de items a paginar
        int pageSize = 50; // Cantidad de items por página

        List<CompletableFuture<List<Item>>> futurePages = IntStream.range(0, (totalItems + pageSize - 1) / pageSize)
                .mapToObj(page -> {
                    int offset = page * pageSize;
                    int limit = Math.min(pageSize, totalItems - offset);
                    return fetchMotorcycleDataAsync(offset, limit);
                })
                .collect(Collectors.toList());

        List<Item> allItems = futurePages.stream()
                .map(CompletableFuture::join)
                .flatMap(List::stream)
                .collect(Collectors.toList());

        // Registro detallado para verificar el tamaño de cada página
        int totalFetched = 0;
        for (int i = 0; i < futurePages.size(); i++) {
            int pageSizeFetched = futurePages.get(i).join().size();
            totalFetched += pageSizeFetched;
        }

        if (allItems.size() < totalItems) {
            int offset = allItems.size(); // La posición del siguiente elemento faltante
            int limit = totalItems - allItems.size(); // Elementos restantes para alcanzar 900
            List<Item> additionalItems = fetchMotorcycleData(offset, limit);
            allItems.addAll(additionalItems);
        }

        System.out.println("total items fetched: " + allItems.size());
        return allItems;
    }



    private CompletableFuture<List<Item>> fetchMotorcycleDataAsync(int offset, int limit) {
        return CompletableFuture.supplyAsync(() -> fetchMotorcycleData(offset, limit));
    }

    private List<Item> fetchMotorcycleData(int offset, int limit) {
        String url = String.format("%s&offset=%d&limit=%d", baseUrl, offset, limit);
        return Optional.ofNullable(restTemplate.getForObject(url, MercadoLibreResponse.class))
                .map(MercadoLibreResponse::getResults)
                .orElse(Collections.emptyList());
    }

    private Map<String, BrandCurrencyData> processItems(List<Item> items) {
        Map<String, BrandCurrencyData> currencyDataByBrand = new HashMap<>();

        for (Item item : items) {
            String brand = item.getBrand().toLowerCase();

            // Correcciones de nombres de marcas
            if (brand.equals("susuki")) {
                brand = "suzuki";
            } else if (brand.equals("royal enfiled")) {
                brand = "royal enfield";
            } else if (brand.equals("c&f")) {
                brand = "cfmoto";
            }

            double price = item.getPrice();
            String currencyId = item.getCurrencyId();

            currencyDataByBrand.computeIfAbsent(brand, k -> new BrandCurrencyData())
                    .addData(price, currencyId);
        }
        return currencyDataByBrand;
    }


    private Map<String, Map<String, String>> calculateAverages(Map<String, BrandCurrencyData> dataByBrand) {
        Map<String, Map<String, String>> averages = new HashMap<>();

        dataByBrand.forEach((brand, data) -> {
            Map<String, String> details = new HashMap<>();
            double average = data.getAveragePrice();
            String currency = data.getPredominantCurrency();

            details.put("promedio", String.format("%.2f", average));
            details.put("currency", currency);
            averages.put(brand, details);

            // Imprime la marca y el promedio con su moneda en la consola
            System.out.println("Marca: " + brand + ", Promedio: " + details.get("promedio") + " " + details.get("currency"));
        });

        return averages;
    }


    static class BrandCurrencyData {
        private double totalARS = 0;
        private double totalUSD = 0;
        private int countARS = 0;
        private int countUSD = 0;
        private static final double USD_TO_ARS_RATE = 1200; // Tasa de cambio fija para conversión

        void addData(double price, String currencyId) {
            if ("ARS".equals(currencyId)) {
                totalARS += price;
                countARS++;
            } else if ("USD".equals(currencyId)) {
                totalUSD += price;
                countUSD++;
            }
        }

        double getAveragePrice() {
            // Determina la moneda predominante y convierte la minoría de precios
            if (countARS > countUSD) {
                // Convertir el total de USD a ARS y sumar al total en ARS
                double totalUSDinARS = totalUSD * USD_TO_ARS_RATE;
                return (totalARS + totalUSDinARS) / (countARS + countUSD);
            } else {
                // Convertir el total de ARS a USD y sumar al total en USD
                double totalARSinUSD = totalARS / USD_TO_ARS_RATE;
                return (totalUSD + totalARSinUSD) / (countARS + countUSD);
            }
        }

        String getPredominantCurrency() {
            // Determina la moneda predominante para la salida
            return countARS >= countUSD ? "ARS" : "USD";
        }
    }

}
