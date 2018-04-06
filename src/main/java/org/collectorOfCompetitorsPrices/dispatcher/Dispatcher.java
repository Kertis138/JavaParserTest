package org.collectorOfCompetitorsPrices.dispatcher;

import java.util.Map;

public interface Dispatcher {
    void parseAllSite() throws Exception;
    Map<String, Boolean> parseSite(int competitorId);
}
