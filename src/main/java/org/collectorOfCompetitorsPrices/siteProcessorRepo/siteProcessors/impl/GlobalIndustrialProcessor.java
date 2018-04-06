package org.collectorOfCompetitorsPrices.siteProcessorRepo.siteProcessors.impl;


import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.collectorOfCompetitorsPrices.models.brand.Brand;
import org.collectorOfCompetitorsPrices.models.competitorPriceCheck.CompetitorPriceCheck;
import org.collectorOfCompetitorsPrices.utils.ComparativeUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class GlobalIndustrialProcessor extends AbstractSiteProcessorImpl {

    private static Logger logger = Logger.getLogger(GlobalIndustrialProcessor.class);

    private Integer competitorId;
    private String siteAddress;
    private String patternToBrand;
    private String brandSelectorForNumber;
    private String pagePatten;
    private String itemOnPagePattern;
    private List<CompetitorPriceCheck> brandCompetitorPriceList = new ArrayList<>();

    private Integer sleepTime;
    private Map<String, String> brandAndAddress;

    private int parallelFixedThreadPoolForParseItem;
    private Integer timeout;

    private int incompleteMatchingBrand;
    private String[] proxyList;

    @Override
    public List<String> getBrandInSite(List<String> brandsFromRest) {
        brandAndAddress = new HashMap<>();
        Random rand = new Random();
        int n = rand.nextInt(proxyList.length);

        String proxyString = proxyList[n];
        String[] proxyArray = proxyString.split(":");

        for (char i = 'a'; i <= 'z'; i++) {
            String pageResponse = getHttpResponse(siteAddress + patternToBrand + i, sleepTime, timeout, proxyArray);
            getBrand(pageResponse);
        }
        String pageResponse = getHttpResponse(siteAddress + patternToBrand + brandSelectorForNumber, sleepTime, timeout, proxyArray);
        getBrand(pageResponse);
        return new ArrayList<>(brandAndAddress.keySet());
    }

    private void getBrand(String pageResponse) {
        Document document = Jsoup.parseBodyFragment(pageResponse);
        Elements a = document.getElementsByTag("a");
        for (Element next : a) {
            String href = next.attr("href");
            String brandName = next.children().first().text().toUpperCase();
            brandAndAddress.put(brandName, href);
        }
    }


    @Override
    public Integer getItemPageCount(String brandForSearch) {
        String brandPattern = brandAndAddress.get(brandForSearch);

        Random rand = new Random();
        int n = rand.nextInt(proxyList.length);

        String proxyString = proxyList[n];
        String[] proxyArray = proxyString.split(":");

        String response = getHttpResponse(siteAddress + "/" + brandPattern + pagePatten + 1 + itemOnPagePattern, sleepTime, timeout, proxyArray);
        if (response == null) {
            return null;
        }
        Document document = Jsoup.parseBodyFragment(response);
        Elements sortpage = document.getElementsByClass("sortpage");
        if (sortpage == null || sortpage.isEmpty()) {
            return 1;
        }
        String elementCount = sortpage.first().getElementsByTag("p")
                .text().replace(" Results 24 72 per pg", "").trim();
        return (int) Math.ceil(Float.parseFloat(elementCount) / 72);
    }

    @Override
    public List<CompetitorPriceCheck> getItemByBrands(Brand brandName, List<String> itemByBrandFromRest) {
        String brand = brandName.getBrandNameInRest();
        Integer pageCount = getItemPageCount(brand);
        String brandAddress = brandAndAddress.get(brand);
        ExecutorService executorService = Executors.newFixedThreadPool(parallelFixedThreadPoolForParseItem);
        for (int i = 1; i <= pageCount; i++) {
            int finalI = i;
            executorService.submit(() -> parsePage(brand, brandAddress, finalI));
        }
        executorService.shutdown();
        try {
            executorService.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.error("Thread pool is Interrupted! Probably not all elements of brand was parsing, cause " + ExceptionUtils.getStackTrace(e));
        }
        return ComparativeUtil.getExistItemInOurBase(brandCompetitorPriceList, itemByBrandFromRest);
    }

    private void parsePage(String brand, String brandPattern, int i) {
        Random rand = new Random();
        int n = rand.nextInt(proxyList.length);

        String proxyString = proxyList[n];
        String[] proxyArray = proxyString.split(":");

        String httpResponse = getHttpResponse(siteAddress + "/" + brandPattern + pagePatten + i + itemOnPagePattern, sleepTime, timeout, proxyArray);
        Document document = Jsoup.parseBodyFragment(httpResponse);
        Elements productTable = document.getElementsByClass("grid").first().children().first().children();
        for (Element itemNode : productTable) {
            String itemNumber = itemNode.getElementsByClass("itemno").text().replace("Item #: ", "");
            String price = itemNode.getElementsByClass("price").text();
            synchronized (brandCompetitorPriceList) {
                brandCompetitorPriceList = putNewCpcInMap(brandCompetitorPriceList, competitorId, brand, itemNumber,
                        price, "in stock");
            }
        }
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

    public void setCompetitorId(Integer competitorId) {
        this.competitorId = competitorId;
    }

    public void setSiteAddress(String siteAddress) {
        this.siteAddress = siteAddress;
    }

    public void setPatternToBrand(String patternToBrand) {
        this.patternToBrand = patternToBrand;
    }

    public String getPatternToBrand() {
        return patternToBrand;
    }

    public String getBrandSelectorForNumber() {
        return brandSelectorForNumber;
    }

    public void setBrandSelectorForNumber(String brandSelectorForNumber) {
        this.brandSelectorForNumber = brandSelectorForNumber;
    }

    public Integer getSleepTime() {
        return sleepTime;
    }


    public void setSleepTime(Integer sleepTime) {
        this.sleepTime = sleepTime;
    }

    public String getItemOnPagePattern() {
        return itemOnPagePattern;
    }

    public void setItemOnPagePattern(String itemOnPagePattern) {
        this.itemOnPagePattern = itemOnPagePattern;
    }

    public String getPagePatten() {
        return pagePatten;
    }

    public void setPagePatten(String pagePatten) {
        this.pagePatten = pagePatten;
    }

    public int getParallelFixedThreadPoolForParseItem() {
        return parallelFixedThreadPoolForParseItem;
    }

    public void setParallelFixedThreadPoolForParseItem(int parallelFixedThreadPoolForParseItem) {
        this.parallelFixedThreadPoolForParseItem = parallelFixedThreadPoolForParseItem;
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
