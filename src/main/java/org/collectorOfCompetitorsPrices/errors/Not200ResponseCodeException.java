package org.collectorOfCompetitorsPrices.errors;


public class Not200ResponseCodeException extends RuntimeException {
    public Not200ResponseCodeException() {
        super("HTTP Response Code is not 200");
    }
}
