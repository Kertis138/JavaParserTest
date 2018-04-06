package org.collectorOfCompetitorsPrices.siteProcessorRepo.siteProcessors.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
//import javafx.util.Entry;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.collectorOfCompetitorsPrices.models.brand.Brand;
import org.collectorOfCompetitorsPrices.models.competitorPriceCheck.CompetitorPriceCheck;
import org.collectorOfCompetitorsPrices.utils.ComparativeUtil;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

public class PartsAvatarProcessor extends AbstractSiteProcessorImpl {
    private static Logger logger = Logger.getLogger(PartsAvatarProcessor.class);
    private Integer competitorId;
    private String siteAddress;
    private String patternToBrands;
    private String pagePatternToBrandsFirstPart;
    private String pagePatternToBrandsSecondPart;
    private String pagePatternToBrandsThirdPart;
    private String pagePatternToBrandsPricePart;
    private Integer sleepTime;
    private Integer errorCountConst;
    private List<CompetitorPriceCheck> brandCompetitorPriceList = null;
    private Integer startPrice;
    private Integer endPrice;
    private Integer timeout;
    private int incompleteMatchingBrand;
    private String[] proxyList;


    @Override
    public List<String> getBrandInSite(List<String> brandsFromRest) {
        List<String> brandList = new ArrayList<>();
        try {

            Random rand = new Random();
            int n = rand.nextInt(proxyList.length);

            String proxyString = proxyList[n];
            String[] proxyArray = proxyString.split(":");

            for (String brand : brandsFromRest) {
                String response = getHttpResponse(siteAddress + pagePatternToBrandsFirstPart + brand + pagePatternToBrandsSecondPart + 1 + pagePatternToBrandsThirdPart, sleepTime, timeout, proxyArray);
                if (response != null && response.contains("\"type\":\"manufacturer\",")) {

                    brandList.add(brand);

                    logger.debug("Find a new brand in site - " + brand);
                }
                else {
                	break;
                }
            }
            return brandList;
        } catch (NullPointerException e) {
            logger.error("Cant find some jsoup element in dom structure \n" + ExceptionUtils.getStackTrace(e)
                    + "process \"get brand in site\" is interrupted");

        }
        return null;
    }

    @Override
    public List<CompetitorPriceCheck> getItemByBrands(Brand brandName, List<String> itemByBrandFromRest) {
        String brand = brandName.getBrandNameInRest();
        brandCompetitorPriceList = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String brandForSearch = brand.replace(" ", "+");
            int count = 0;


            Map<Integer, Entry<Integer, Integer>> differentiationByPrice = getDifferentiationByPrice(brandForSearch);
            if (differentiationByPrice == null) {
                logger.error("Cant get differentiationByPrice");
                return null;
            }
            for (Map.Entry<Integer, Entry<Integer, Integer>> entry : differentiationByPrice.entrySet()) {
                Integer key = entry.getKey();
                Entry<Integer, Integer> value = entry.getValue();
                String stingUrl = siteAddress + pagePatternToBrandsFirstPart + brandForSearch + pagePatternToBrandsSecondPart + 1
                        + pagePatternToBrandsThirdPart + pagePatternToBrandsPricePart + value.getKey() + "-" + value.getValue();
                parsePages(brandForSearch, 1, key, stingUrl);
            }
            return ComparativeUtil.getExistItemInOurBase(brandCompetitorPriceList, itemByBrandFromRest);
        } catch (
                NullPointerException e)

        {
            logger.error("Cant find some jsoup element in dom structure" + ExceptionUtils.getStackTrace(e)
                    + "process \"get item by brands\" for brand " + brand + " is interrupted");
            return brandCompetitorPriceList;

        }

    }

    private Map<Integer, Entry<Integer, Integer>> getDifferentiationByPrice(String brandForSearch) {

        Random rand = new Random();
        int n = rand.nextInt(proxyList.length);

        String proxyString = proxyList[n];
        String[] proxyArray = proxyString.split(":");

        String response = getHttpResponse(siteAddress + pagePatternToBrandsFirstPart + brandForSearch + pagePatternToBrandsSecondPart + 1 + pagePatternToBrandsThirdPart, sleepTime, timeout, proxyArray);
        if (response == null) {
            return null;
        }
        ObjectMapper objectMapper = new ObjectMapper();
        Map<Integer, Entry<Integer, Integer>> integerEntryMap = new HashMap<>();
        try {
            JsonNode jsonNode = objectMapper.readTree(response);
            JsonNode prices = jsonNode.path("prices");
            Iterator<JsonNode> iterator = prices.iterator();
            while (iterator.hasNext()) {
                JsonNode next = iterator.next();
                Integer count = next.path("count").intValue();
                Integer from = next.path("from").intValue();
                Integer to = next.path("to").intValue();
                integerEntryMap.put((int) Math.ceil((count) / 9), new java.util.AbstractMap.SimpleEntry<>(from, to));
            }
        } catch (IOException e) {
            logger.error("objectMapper cant transformation json, cause: " + ExceptionUtils.getStackTrace(e) + "\n"
                    + "process \"get item by brands\" to brand is interrupted");
            return null;
        }
        return integerEntryMap;
    }

    private void parsePages(String brandForSearch, Integer from, Integer to, String withPriceFilter) {
        int count = from;
        for (int brandProductPageNumber = from; brandProductPageNumber < to; brandProductPageNumber++) {
            ObjectMapper objectMapper = new ObjectMapper();
            logger.info("Now parse page " + brandProductPageNumber);
            Iterator<JsonNode> elements;
            String pageResponse = null;
            try {
                String url = siteAddress + pagePatternToBrandsFirstPart + brandForSearch + pagePatternToBrandsSecondPart + brandProductPageNumber + pagePatternToBrandsThirdPart;
                if (withPriceFilter != null) {
                    url = setCurrentPageCountInUrl(withPriceFilter, brandProductPageNumber);
                }
                Random rand = new Random();
                int n = rand.nextInt(proxyList.length);

                String proxyString = proxyList[n];
                String[] proxyArray = proxyString.split(":");

                pageResponse = getHttpResponse(url, sleepTime, timeout, proxyArray);
                JsonNode jsonNode = objectMapper.readTree(pageResponse);
                JsonNode tableNodes = jsonNode.path("products").path("products");
                elements = tableNodes.elements();
            } catch (NullPointerException e) {
                logger.error("Cant find some jsoup element in dom structure" + ExceptionUtils.getStackTrace(e) + "process \"get item by brands\" to brand" + brandForSearch + " page " + brandProductPageNumber + " is interrupted");
                continue;
            } catch (IOException e) {
                logger.error("objectMapper cant transformation json, cause: " + ExceptionUtils.getStackTrace(e) + "\n" + "page response is " + pageResponse + "process \"get item by brands\" to brand is interrupted");
                return;
            }
            int i = 1;
            while (elements.hasNext()) {
                try {
                    JsonNode productNode = elements.next();
                    String itemName = productNode.path("partNumber").asText();
                    Double priceNode = productNode.path("price").asDouble();
                    String outOfStock = "in stock";
                    if (productNode.path("outOfStock").asBoolean()) {
                        outOfStock = "out of stock";
                    }
                    String price = String.valueOf(priceNode / 100);
                    synchronized (brandCompetitorPriceList) {
                        brandCompetitorPriceList = putNewCpcInMap(brandCompetitorPriceList, competitorId, brandForSearch, itemName, price, outOfStock);
//                        logger.info("Add new element " +  brandForSearch + " "+itemName + " "+price);
                    }
                    count++;
                    if (count == to) {
                        count = 0;
//                        logger.info("brandCompetitorPriceList size for brand: " + brandForSearch + " is" + brandCompetitorPriceList.size());
                    }
                    i++;
                } catch (NullPointerException e) {
                    logger.error("Cant find some jsoup element in dom structure" + ExceptionUtils.getStackTrace(e)
                            + "process \"get item by brands\" to brand" + brandForSearch + " page " + brandProductPageNumber + " element " + i + " is interrupted");
                    i++;
                }
            }
        }
    }

    private String setCurrentPageCountInUrl(String withPriceFilter, int brandProductPageNumber) {
        StringBuilder stringBuilder = new StringBuilder();
        String[] split = withPriceFilter.split("&");
        for (String part : split) {
            if (part.contains("pg=")) {
                stringBuilder.append("pg=" + brandProductPageNumber + "&");
            } else {
                stringBuilder.append(part).append("&");
            }
        }
        return stringBuilder.replace(stringBuilder.length() - 2, stringBuilder.length() - 1, "").toString();
    }

    @Override
    public Integer getCompetitorId() {
        return competitorId;
    }

    @Override
    public String getSiteAddress() {
        return siteAddress;
    }

    @Override
    public int getIncompleteMatchingBrand() {
        return incompleteMatchingBrand;
    }

    public void setSleepTime(Integer sleepTime) {
        this.sleepTime = sleepTime;
    }

    public void setPatternToBrand(String patternToBrands) {
        this.patternToBrands = patternToBrands;
    }

    public void setSiteAddress(String siteAddress) {
        this.siteAddress = siteAddress;
    }


    public void setCompetitorId(Integer competitorId) {
        this.competitorId = competitorId;
    }

    public void setPagePatternToBrandsThirdPart(String pagePatternToBrandsThirdPart) {
        this.pagePatternToBrandsThirdPart = pagePatternToBrandsThirdPart;
    }

    public void setPagePatternToBrandsSecondPart(String pagePatternToBrandsSecondPart) {
        this.pagePatternToBrandsSecondPart = pagePatternToBrandsSecondPart;
    }

    public void setPagePatternToBrandsFirstPart(String pagePatternToBrandsFirstPart) {
        this.pagePatternToBrandsFirstPart = pagePatternToBrandsFirstPart;
    }

    @Deprecated
    public Integer getItemPageCount(String brandForSearch) {
        return null;
    }

    public void setErrorCountConst(Integer errorCountConst) {
        this.errorCountConst = errorCountConst;
    }

    public Integer getErrorCountConst() {
        return errorCountConst;
    }

    public String getPagePatternToBrandsPricePart() {
        return pagePatternToBrandsPricePart;
    }

    public void setPagePatternToBrandsPricePart(String pagePatternToBrandsPricePart) {
        this.pagePatternToBrandsPricePart = pagePatternToBrandsPricePart;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public void setIncompleteMatchingBrand(int incompleteMatchingBrand) {
        this.incompleteMatchingBrand = incompleteMatchingBrand;
    }

    public String[] getProxyList() {
        return proxyList;
    }

    public void setProxyList(String[] proxyList) {
        this.proxyList = proxyList;
    }
}

