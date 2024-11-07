package com.motorsport.controller;

import com.motorsport.service.MotoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class MotoController {

    private final MotoService motoService;

    public MotoController(MotoService motoService) {
        this.motoService = motoService;
    }

    @GetMapping("/promedios")
    public Map<String, Map<String, String>> obtenerPromedios() {
        return motoService.obtenerPromedioPorMarca();
    }
}