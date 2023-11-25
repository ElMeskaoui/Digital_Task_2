package com.example.digitaltask2.services;

import com.example.digitaltask2.models.GeoArea;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class GeoAreaService {

    private final RestTemplate restTemplate;

    public GeoAreaService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }



    public GeoArea generateGeoJSON(String areaName) throws IOException {

        GeoArea geoArea = new GeoArea();

        ObjectMapper objectMapper = new ObjectMapper();


        String nominatimBaseUrl = "https://nominatim.openstreetmap.org/";
        JsonNode JsonNode = restTemplate.getForObject(nominatimBaseUrl + "search.php?q=" + areaName +
                "&polygon_geojson=1&format=jsonv2", com.fasterxml.jackson.databind.JsonNode.class);
        JsonNode placeNode = JsonNode.get(0);

        long osmId = placeNode.get("osm_id").asLong();
        String addresstype = placeNode.get("addresstype").asText();
        String geojson = String.valueOf(placeNode.findValue("geojson"));
        Double lat = placeNode.get("lat").asDouble();
        Double lon = placeNode.get("lon").asDouble();
        GeoArea.Center center = new GeoArea.Center();
        center.setLatitude(lat);
        center.setLongitude(lon);
        String postalCodes = "";

        System.out.println("osm_id: " + osmId);

//        2.Get the name of the area

        String queryArea = "[out:json]; rel("+osmId+"); out;";

        String overpassEndpoint = "https://overpass-api.de/api/interpreter";
        ResponseEntity<String> response = restTemplate.getForEntity(overpassEndpoint + "?data=" + queryArea, String.class);

        String AreaName = extractAreaName(response.getBody());
        System.out.println("Amt name: " + AreaName);

        geoArea.setName(AreaName);
        geoArea.setGeoAreaType(addresstype);
        geoArea.setCenter(center);
        geoArea.setGeoJson(geojson);


//        3.Get all children of the area

        String queryChildren = "[out:json]; area[name=\""+AreaName+"\"]; rel(area)[\"admin_level\"~\"8\"]; (._;<<;); out;";
        JsonNode responseChildren = restTemplate.getForEntity(overpassEndpoint + "?data=" + queryChildren, JsonNode.class).getBody();
        JsonNode root = objectMapper.readTree(responseChildren.traverse());
        JsonNode elements = root.get("elements");

        if (elements != null && elements.isArray()) {
            List<GeoArea.ChildGeoArea> childGeoAreas = new ArrayList<>();

            for (JsonNode element : elements) {
                Long idNode = element.get("id").asLong();
                String nameNode = element.get("tags").get("name").asText();
                System.out.println(nameNode);
                String subpartUrl = nominatimBaseUrl + "search.php?q=" + nameNode + "&polygon_geojson=1&format=jsonv2";
                JsonNode nameNodeDet = restTemplate.getForObject(subpartUrl, JsonNode.class);
                long nameNodeDet_OsmID = nameNodeDet.findValue("osm_id").asLong();

                if (idNode == nameNodeDet_OsmID) {
                    System.out.println("good ID");
                    String categoryType = nameNodeDet.findValue("category").asText();
                    String osmType = (nameNodeDet.findValue("osm_type").asText().equals("relation")) ? "R" : "N";
                    JsonNode TryPlaces = restTemplate.getForObject(nominatimBaseUrl + "details.php?osmtype=" + osmType +
                                    "&osmid=" + idNode + "&class=" + categoryType + "&addressdetails=1&hierarchy=0&group_hierarchy=1&polygon_geojson=1&format=json",
                            JsonNode.class);
                    GeoArea.ChildGeoArea childGeoArea = new GeoArea.ChildGeoArea();
                    childGeoArea.setName(nameNode);
                    System.out.println(idNode+nameNode);

                    JsonNode node = TryPlaces.get("calculated_postcode");
                    String postalCode;
                    if(node != null && !node.isNull()){
                        postalCode = node.asText();
                        postalCodes += postalCode + "," ;
                    }else postalCode = "";

                    childGeoArea.setPostalCodes(postalCode);
                    childGeoArea.setGeoJson(String.valueOf(TryPlaces.get("geometry")));
                    childGeoArea.setGeoAreaType(nameNodeDet.findValue("addresstype").asText());

                    Double chiLat = nameNodeDet.findValue("lat").asDouble();
                    Double chiLon = nameNodeDet.findValue("lon").asDouble();

                    GeoArea.Center chiCenter = new GeoArea.Center();
                    chiCenter.setLatitude(chiLat);
                    chiCenter.setLongitude(chiLon);

                    childGeoArea.setCenter(chiCenter);

//                    add the child to the List
                    childGeoAreas.add(childGeoArea);
                }

            }
//            Set the list of ChildGeoAreas in GeoArea
            geoArea.setChildGeoAreas(childGeoAreas);
            geoArea.setPostalCodes(postalCodes);
        }



        return geoArea;

    }

    private static String extractAreaName(String json) throws JsonProcessingException {
        // Parse the JSON data
        Map<String, Object> data = new ObjectMapper().readValue(json, Map.class);

        // Get the name of the Amt
        List<Map<String, Object>> elements = (List<Map<String, Object>>) data.get("elements");
        Map<String, Object> relation = elements.get(0);
        Map<String, Object> tags = (Map<String, Object>) relation.get("tags");
        return (String) tags.get("name");
    }
}
