package com.motorsport.service;

import com.motorsport.model.Item;
import com.motorsport.model.MercadoLibreResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

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
        final int totalItems = 900;
        final int pageSize = 50;
        Executor executor = Executors.newFixedThreadPool(10); // Ajusta según los recursos disponibles
        List<CompletableFuture<List<Item>>> futures = new ArrayList<>();

        for (int offset = 0; offset < totalItems; offset += pageSize) {
            int limit = Math.min(pageSize, totalItems - offset);
            futures.add(fetchMotorcycleDataAsync(offset, limit, executor));
        }

        // se combinan los resultados de todas las llamadas a la api usando CompletableFuture.allOf y CompletableFuture.join
        CompletableFuture<Void> allDoneFuture = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        List<Item> allItems = allDoneFuture.thenApply(v -> {
            return futures.stream()
                    .map(CompletableFuture::join)
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
        }).join();

        // aca me fijo si aun hace falta traer mas datos de la api dependiendo de la cantidad de items que se hayan traido
        int receivedItems = allItems.size();
        if (receivedItems < totalItems) {
            int remainingItems = totalItems - receivedItems;

            while (receivedItems < totalItems) {
                int offset = receivedItems;
                int limit = Math.min(pageSize, remainingItems);
                List<Item> additionalItems = fetchMotorcycleData(offset, limit);
                allItems.addAll(additionalItems);
                receivedItems += additionalItems.size();
                remainingItems -= additionalItems.size();
                if (additionalItems.isEmpty()) {
                    break;
                }
            }
        }
        return allItems;
    }

    private CompletableFuture<List<Item>> fetchMotorcycleDataAsync(int offset, int limit, Executor executor) {
        return CompletableFuture.supplyAsync(() -> fetchMotorcycleData(offset, limit), executor);
    }

    private List<Item> fetchMotorcycleData(int offset, int limit) {
        String url = String.format("%s&offset=%d&limit=%d", baseUrl, offset, limit);
        return Optional.ofNullable(restTemplate.getForObject(url, MercadoLibreResponse.class))
                .map(MercadoLibreResponse::getResults)
                .orElse(Collections.emptyList());
    }

    private Map<String, BrandCurrencyData> processItems(List<Item> items) {
        Map<String, BrandCurrencyData> currencyDataByBrand = new HashMap<>();
        items.forEach(item -> {
            String brand = item.getBrand().toLowerCase();
            double price = item.getPrice();
            String currencyId = item.getCurrencyId();
            currencyDataByBrand.computeIfAbsent(brand, k -> new BrandCurrencyData())
                    .addData(price, currencyId);
        });
        return currencyDataByBrand;
    }

    private Map<String, Map<String, String>> calculateAverages(Map<String, BrandCurrencyData> dataByBrand) {
        Map<String, Map<String, String>> averages = new HashMap<>();
        dataByBrand.forEach((brand, data) -> {
            Map<String, String> details = new HashMap<>();
            details.put("promedio", String.format("%.2f", data.getAveragePrice()));
            details.put("currency", data.getPredominantCurrency());
            System.out.println("Marca: " + brand + ", Promedio: " + details.get("promedio") + " " + details.get("currency"));
        });
        return averages;
    }

    static class BrandCurrencyData {
        private double totalARS = 0;
        private double totalUSD = 0;
        private int countARS = 0;
        private int countUSD = 0;

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
            return (countARS >= countUSD) ? totalARS / countARS : totalUSD / countUSD;
        }

        String getPredominantCurrency() {
            return (countARS >= countUSD) ? "ARS" : "USD";
        }
    }
}
