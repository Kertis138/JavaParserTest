package org.collectorOfCompetitorsPrices.dispatcher.impl;


import org.apache.log4j.Logger;
import org.collectorOfCompetitorsPrices.dispatcher.Dispatcher;
import org.collectorOfCompetitorsPrices.models.brand.Brand;
import org.collectorOfCompetitorsPrices.models.competitor.Competitor;
import org.collectorOfCompetitorsPrices.models.competitorPriceCheck.CompetitorPriceCheck;
import org.collectorOfCompetitorsPrices.restTheWrenchMonkeyAdapter.HttpRequestToRestAdapter;
import org.collectorOfCompetitorsPrices.siteProcessorRepo.RepoSiteProcessors;
import org.collectorOfCompetitorsPrices.utils.ComparativeUtil;

import java.util.*;

public class DispatcherImpl implements Dispatcher {
    private static Logger logger = Logger.getLogger(DispatcherImpl.class);
    private HttpRequestToRestAdapter httpRequestToRestAdapter;
    private RepoSiteProcessors repoSiteProcessors;

    private Map<Integer, Competitor> listCompetitorFromRest;
    private List<String> brandsFromRest;

    @Override
    public void parseAllSite()  {
        listCompetitorFromRest.keySet().forEach(this::parseSite);
    }

    public Map<String, Boolean> parseSite(int competitorId) {

        logger.info("start parse...");
        if (listCompetitorFromRest == null || listCompetitorFromRest.isEmpty()) {
            logger.error("competitor list is empty");
            return null;
        }
        Competitor competitor = listCompetitorFromRest.get(competitorId);
        if (competitor == null) {
            logger.error("competitor is empty\n" +
                    "Parsing competitor " + competitorId + " is interrupted");
            return null;
        }

        logger.info("get list of competitor from rest");
        Integer competitorIdFromRest = competitor.getId();
        String domainAddressInRepoSite = repoSiteProcessors.getDomainAddress(competitorIdFromRest);
        if (domainAddressInRepoSite == null || domainAddressInRepoSite.isEmpty()) {
            logger.error("domainAddressInRepoSite is empty\n" +
                    "Parsing competitor " + competitorId + " is interrupted");
            return null;
        }
        String domain = domainAddressInRepoSite.replace("https://", "").replace("http://", "").replace("www.", "");
        if(!(competitor.getDomain().contains(domain))){
            logger.error("domain address in repoSite NOT matches the address in properties!\n"+
                    "Parsing competitor " + competitorId + " is interrupted");
            return null;
        }
        logger.info("domain address in repoSite matches the address in properties");


        logger.info("Receive brands from site is started...");
        List<String> brandsFromSite = repoSiteProcessors.getBrandsInSite(competitorId, brandsFromRest);
        if (brandsFromSite == null || brandsFromSite.isEmpty()) {
            logger.error("brandsFromSite is empty\n" +
                    "Parsing competitor " + competitorId + " is interrupted");
            return null;
        }
        logger.info("Receive brands from site is successful complete");

        logger.info("Compare brands is started...");
        int incompleteMatchingBrand = repoSiteProcessors.getIncompleteMatchingBrand(competitorId);
        logger.info(brandsFromRest.toString());
        Set<Brand> existOurBrandOnSiteSet = ComparativeUtil.compareBrands(brandsFromSite, brandsFromRest, incompleteMatchingBrand);
        if (existOurBrandOnSiteSet == null || existOurBrandOnSiteSet.isEmpty()) {
            logger.error("existOurBrandOnSite is empty\n" +
                    "Parsing competitor " + competitorId + " is interrupted");
            return null;
        }
        logger.info("Compare brands is successful complete ");



        logger.info("Receive map ItemListFromRest is started...");
        Map<Brand, List<String>> mapBrandOnItemListFromRest = httpRequestToRestAdapter.getItemNumberByBrand(existOurBrandOnSiteSet);
        if (mapBrandOnItemListFromRest == null || mapBrandOnItemListFromRest.isEmpty()) {
            logger.error("mapBrandOnItemListFromRest is empty\n" +
                    "Parsing competitor " + competitorId + " is interrupted");
            return null;
        }

        logger.info("Receive map ItemListFromRest is successful complete");
        Map<String, Boolean> brandParseStatusMap = new HashMap<>();



        for (Brand brand : mapBrandOnItemListFromRest.keySet()) {

            String brandNameInRest = brand.getBrandNameInRest();
            logger.info("Receive ItemListFromSite for brand: " + brandNameInRest + " is started... ");
            List<CompetitorPriceCheck> competitorPriceCheckList = repoSiteProcessors.getItemByBrands(competitorId, brand, mapBrandOnItemListFromRest.get(brand));
            if (competitorPriceCheckList == null || competitorPriceCheckList.isEmpty()) {
                logger.error("itemByBrands list from repoSiteProcessors is empty\n" +
                        "Parsing competitor " + competitorId + " is interrupted");
                continue;
            }
            logger.info("Receive ItemListFromSite for brand: " + brandNameInRest + " is successful complete");

            logger.info("Update competitor Price Check List is started...");
            brandParseStatusMap.put(brandNameInRest, httpRequestToRestAdapter.putOrUpdateNewCPC(competitorPriceCheckList));
            logger.info("Update competitor Price Check List is successful complete");
        }
        return brandParseStatusMap;
    }

    private boolean initListCompetitorFromRest() {
        listCompetitorFromRest = httpRequestToRestAdapter.getListCompetitorFromRest();
        brandsFromRest = httpRequestToRestAdapter.getBrands();
        return true;
    }


    public HttpRequestToRestAdapter getHttpRequestToRestAdapter() {
        return httpRequestToRestAdapter;
    }

    public void setHttpRequestToRestAdapter(HttpRequestToRestAdapter httpRequestToRestAdapter) {
        this.httpRequestToRestAdapter = httpRequestToRestAdapter;
    }

    public RepoSiteProcessors getRepoSiteProcessors() {
        return repoSiteProcessors;
    }

    public void setRepoSiteProcessors(RepoSiteProcessors repoSiteProcessors) {
        this.repoSiteProcessors = repoSiteProcessors;
    }


}


