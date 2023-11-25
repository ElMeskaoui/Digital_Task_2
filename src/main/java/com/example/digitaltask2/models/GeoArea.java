package com.example.digitaltask2.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data @AllArgsConstructor @NoArgsConstructor
public class GeoArea {
    private String name;
    private String geoAreaType;
    private String geoJson;
    private Center center;
    private String postalCodes;
    private List<ChildGeoArea> childGeoAreas;


    @Data
    public static class Center {
        private double latitude;
        private double longitude;
    }

    @Data
    public static class ChildGeoArea {
        private String name;
        private String geoAreaType;
        private String geoJson;
        private Center center;
        private String postalCodes;
    }
}
