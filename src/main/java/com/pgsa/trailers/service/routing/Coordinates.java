package com.pgsa.trailers.service.routing;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Coordinates {
    private double lat;
    private double lng;

    public String toQuery() {
        return lng + "," + lat;
    }
}
