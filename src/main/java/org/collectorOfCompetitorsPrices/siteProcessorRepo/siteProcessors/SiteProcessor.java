package org.collectorOfCompetitorsPrices.siteProcessorRepo.siteProcessors;

import org.collectorOfCompetitorsPrices.models.brand.Brand;
import org.collectorOfCompetitorsPrices.models.competitorPriceCheck.CompetitorPriceCheck;

import java.util.List;

public interface SiteProcessor {

    List<String> getBrandInSite(List<String> brandsFromRest);

    List<CompetitorPriceCheck> getItemByBrands(Brand brand, List<String> itemByBrandFromRest);

    String getHttpResponse(String address, Integer timeOutForGetResponse, Integer timeout, String[] proxyList);

    Integer getCompetitorId();

    String getSiteAddress();

    int getIncompleteMatchingBrand();
}
