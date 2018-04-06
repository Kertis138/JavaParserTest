package org.collectorOfCompetitorsPrices.siteProcessorRepo.siteProcessors.impl;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.collectorOfCompetitorsPrices.models.brand.Brand;
import org.collectorOfCompetitorsPrices.models.competitorPriceCheck.CompetitorPriceCheck;
import org.collectorOfCompetitorsPrices.utils.ComparativeUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AutoPartsWayProcessor extends AbstractSiteProcessorImpl {

    private static Logger logger = Logger.getLogger(AutoPartsWayProcessor.class);

    private Integer competitorId;
    private String siteAddress;
    private String patternToPageBrand;
    private String[] proxyList;
    private Integer sleepTime;
    private Integer timeout;
    private int incompleteMatchingBrand;
    private int parallelFixedThreadPoolForParseItem;

    private Map<String, String> brandAndAddress;
    private List<CompetitorPriceCheck> brandCompetitorPriceList;

    @Override
    public List<String> getBrandInSite(List<String> brandsFromRest) {
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            logger.error("Thread.sleep(" + sleepTime + ") is failed, cause" + ExceptionUtils.getStackTrace(e) + "\n"
                    + "Attempts well be continuance");
        }
        try {
            brandAndAddress = new HashMap<>();
            Random rand = new Random();
            int n = rand.nextInt(proxyList.length);
            String proxyString = proxyList[n];
            String[] proxyArray = proxyString.split(":");

            String brandsAddress = siteAddress + patternToPageBrand;
            String response = getHttpResponse(brandsAddress, sleepTime, timeout, proxyArray);
            if (response == null) {
                return null;
            }
            Document domHttpSite = Jsoup.parse(response);
            Elements elements = domHttpSite.select("#product_categories select#Brand > option");

            for(int i=1; i<elements.size(); ++i) {
                try {
                    Element element = elements.get(i);
                    String brand = element.val();
                    brand = brand.toUpperCase();
                    if(!brand.isEmpty()) {
                        brandAndAddress.put(brand, "");
                    }
                } catch (NullPointerException e) {
                    logger.error("Cant find some jsoup element in dom structure \n" + ExceptionUtils.getStackTrace(e)
                            + "process \"get brand in site\" is interrupted");
                }
            }
            return new ArrayList<>(brandAndAddress.keySet());
        } catch (NullPointerException e) {
            logger.error("Cant find some jsoup element in dom structure \n" + ExceptionUtils.getStackTrace(e)
                    + "process \"get brand in site\" is interrupted");
        }
        return null;
    }

    @Override
    public Integer getItemPageCount(String brandForSearch) {
        return null;
    }

    @Override
    public List<CompetitorPriceCheck> getItemByBrands(Brand brandName, List<String> itemByBrandFromRest) {
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            logger.error("Thread.sleep(" + sleepTime + ") is failed, cause" + ExceptionUtils.getStackTrace(e) + "\n"
                    + "Attempts well be continuance");
        }

        String template = "https://www.autopartsway.ca/partlist.cfm/";
        String brandAddress = brandName.getBrandNameInRest();



//        brandCompetitorPriceList = new ArrayList<>();
//        String brandNameInRest = brandName.getBrandNameInRest();
//        Integer pageCount = getItemPageCount(brandNameInRest);
//        String brandAddress = brandAndAddress.get(brandNameInRest);
//        ExecutorService executorService = Executors.newFixedThreadPool(parallelFixedThreadPoolForParseItem);
//        for (int i = 1; i <= pageCount; i++) {
//            int finalI = i;
//            executorService.submit(() -> parsePage(brandNameInRest, brandAddress, finalI));
//        }
//        executorService.shutdown();
//        return ComparativeUtil.getExistItemInOurBase(brandCompetitorPriceList, itemByBrandFromRest);
        return null;
    }

    private void parsePage(String brand, String brandPattern, int i) {

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
        return 0;
    }

    public void setCompetitorId(Integer competitorId) {
        this.competitorId = competitorId;
    }

    public void setSiteAddress(String siteAddress) {
        this.siteAddress = siteAddress;
    }

    public String[] getProxyList() {
        return proxyList;
    }

    public void setProxyList(String[] proxyList) {
        this.proxyList = proxyList;
    }

    public void setSleepTime(Integer sleepTime) {
        this.sleepTime = sleepTime;
    }

    public Integer getSleepTime() {
        return sleepTime;
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

    public String getPatternToPageBrand() {
        return patternToPageBrand;
    }

    public void setPatternToPageBrand(String patternToPageBrand) {
        this.patternToPageBrand = patternToPageBrand;
    }
    public int getParallelFixedThreadPoolForParseItem() {
        return parallelFixedThreadPoolForParseItem;
    }

    public void setParallelFixedThreadPoolForParseItem(int parallelFixedThreadPoolForParseItem) {
        this.parallelFixedThreadPoolForParseItem = parallelFixedThreadPoolForParseItem;
    }

}
