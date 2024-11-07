package com.motorsport.controller;

import com.motorsport.service.MotoService;
import com.motorsport.service.MotoServiceInterface;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class MotoController {

    private final MotoServiceInterface motoService;

    public MotoController(MotoServiceInterface motoService) {
        this.motoService = motoService;
    }

    @GetMapping("/promedios")
    public Map<String, Map<String, String>> calculateAverages() {
        return motoService.obtenerPromedioPorMarca();
    }
}