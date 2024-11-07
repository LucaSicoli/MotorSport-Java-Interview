package com.motorsport.service;
import com.motorsport.model.Item;
import com.motorsport.model.MercadoLibreResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
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

        int offset = 0;

        while (true) {
            String url = baseUrl + "&offset=" + offset;
            MercadoLibreResponse response = restTemplate.getForObject(url, MercadoLibreResponse.class);

            if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
                System.out.println("No se encontraron resultados o la respuesta es nula.");
                break; // Salir del bucle si no hay más resultados
            }

            for (Item item : response.getResults()) {
                String marca = item.getBrand().toLowerCase(); // Convertir a minúsculas para evitar duplicados
                if (marca == null) continue; // Ignorar si la marca es nula

                double price = item.getPrice();
                String currencyId = item.getCurrencyId(); // Obtener el currency_id

                // Actualizar total y conteo por marca
                totalPorMarca.put(marca, totalPorMarca.getOrDefault(marca, 0.0) + price);
                conteoPorMarca.put(marca, conteoPorMarca.getOrDefault(marca, 0) + 1);

                // Acumular en pesos
                if (currencyId.equals("ARS")) {
                    totalPesosPorMarca.put(marca, totalPesosPorMarca.getOrDefault(marca, 0.0) + price);
                    conteoPesosPorMarca.put(marca, conteoPesosPorMarca.getOrDefault(marca, 0) + 1);
                } else if (currencyId.equals("USD")) {
                    // Convertir a pesos y acumular
                    totalPesosPorMarca.put(marca, totalPesosPorMarca.getOrDefault(marca, 0.0) + price * 1130);
                    conteoPesosPorMarca.put(marca, conteoPesosPorMarca.getOrDefault(marca, 0) + 1);
                }
            }

            offset += response.getResults().size();
            if (offset >= 900) break; // Limitar a 900 registros
        }

        // Calcular promedios y agregar moneda
        Map<String, Map<String, String>> promedioConMoneda = new HashMap<>();
        for (String marca : totalPorMarca.keySet()) {
            double total = totalPesosPorMarca.getOrDefault(marca, 0.0); // Usar el total en pesos
            int count = conteoPorMarca.get(marca);
            double promedio = count > 0 ? total / count : 0;

            // Determinar la moneda predominante
            String monedaPredominante;
            if (conteoPesosPorMarca.containsKey(marca)) {
                monedaPredominante = "ARS"; // Si hay precios en ARS
            } else {
                monedaPredominante = "USD"; // Predominará USD si no hay ARS
            }

            // Formatear el promedio a dos decimales
            String promedioFormateado = String.format("%.2f", promedio).replace('.', ','); // Cambiar punto por coma

            Map<String, String> detalles = new HashMap<>();
            detalles.put("promedio", promedioFormateado);
            detalles.put("currency", monedaPredominante); // Usar la moneda predominante

            // Imprimir en consola el promedio y la moneda
            System.out.println("Promedio de " + marca.substring(0, 1).toUpperCase() + marca.substring(1) + ": " + promedioFormateado + " " + monedaPredominante);

            promedioConMoneda.put(marca.substring(0, 1).toUpperCase() + marca.substring(1), detalles); // Capitalizar la primera letra de la marca
        }

        return promedioConMoneda;
    }
}