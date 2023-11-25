package com.example.digitaltask2.controllers;

import com.example.digitaltask2.models.GeoArea;
import com.example.digitaltask2.services.GeoAreaService;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@CrossOrigin("*")
public class GeoAreaController {

    private final GeoAreaService geoAreaService;

    public GeoAreaController(GeoAreaService geoAreaService) {
        this.geoAreaService = geoAreaService;
    }

    @GetMapping("/generateGeoJSON/{areaName}")
    public Object generateGeoJSON(@PathVariable String areaName) throws IOException {
        GeoArea geoJSON = geoAreaService.generateGeoJSON(areaName);
        return geoJSON;
    }


}
