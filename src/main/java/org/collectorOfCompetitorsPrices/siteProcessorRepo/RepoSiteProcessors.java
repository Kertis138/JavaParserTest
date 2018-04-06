package org.collectorOfCompetitorsPrices.siteProcessorRepo;

import org.collectorOfCompetitorsPrices.models.brand.Brand;
import org.collectorOfCompetitorsPrices.models.competitorPriceCheck.CompetitorPriceCheck;
import org.collectorOfCompetitorsPrices.siteProcessorRepo.siteProcessors.SiteProcessor;

import java.util.List;

public interface RepoSiteProcessors {

    String getDomainAddress(Integer competitorId);

    List<String> getBrandsInSite(int competitorId, List<String> brandsFromRest);

     List<CompetitorPriceCheck> getItemByBrands(Integer competitorId, Brand brand, List<String> itemByBrandFromRest);

    void setSiteProcessors(List<SiteProcessor> siteProcessorList);

    SiteProcessor getSiteProcessor(Integer siteProcessor);

    int getIncompleteMatchingBrand(int competitorId);
}

