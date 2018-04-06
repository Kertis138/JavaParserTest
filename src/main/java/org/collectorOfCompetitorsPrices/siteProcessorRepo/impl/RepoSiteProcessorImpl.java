package org.collectorOfCompetitorsPrices.siteProcessorRepo.impl;

import org.collectorOfCompetitorsPrices.models.brand.Brand;
import org.collectorOfCompetitorsPrices.models.competitorPriceCheck.CompetitorPriceCheck;
import org.collectorOfCompetitorsPrices.siteProcessorRepo.RepoSiteProcessors;
import org.collectorOfCompetitorsPrices.siteProcessorRepo.siteProcessors.SiteProcessor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class RepoSiteProcessorImpl implements RepoSiteProcessors {
    private Map<Integer, SiteProcessor> siteProcessorMap = new HashMap<>();
    private List<SiteProcessor> siteProcessorList;

    @Override
    public String getDomainAddress(Integer competitorId) {
        return siteProcessorMap.get(competitorId).getSiteAddress();
    }

    @Override
    public List<String> getBrandsInSite(int competitorId, List<String> brandsFromRest) {

        return siteProcessorMap.get(competitorId).getBrandInSite(brandsFromRest);
    }

    @Override
    public List<CompetitorPriceCheck> getItemByBrands(Integer competitorId, Brand brand, List<String> itemByBrandFromRest) {
        return siteProcessorMap.get(competitorId).getItemByBrands(brand, itemByBrandFromRest);
    }

    @Override
    public void setSiteProcessors(List<SiteProcessor> siteProcessorList) {
        this.siteProcessorList = siteProcessorList;
        for (SiteProcessor siteProcessor : siteProcessorList) {
            siteProcessorMap.put(siteProcessor.getCompetitorId(), siteProcessor);
        }
    }

    @Override
    public SiteProcessor getSiteProcessor(Integer siteProcessor) {
        return siteProcessorMap.get(siteProcessor);
    }

    @Override
    public int getIncompleteMatchingBrand(int competitorId) {
        return siteProcessorMap.get(competitorId).getIncompleteMatchingBrand();
    }
}
