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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


public class CanadaAutoSupplyProcessor extends AbstractSiteProcessorImpl {

    private static Logger logger = Logger.getLogger(CanadaAutoSupplyProcessor.class);

    private Map<String, Integer> brandAndAddress;

    private Integer competitorId;
    private String siteAddress;
    private String patternToBrand;
    private String pagePatternToBrands;
    private Integer sleepTime;
    private List<CompetitorPriceCheck> brandCompetitorPriceList;
    private AtomicInteger count;
    private Integer parallelFixedThreadPoolForParseItem;
    private Integer timeout;
    private int incompleteMatchingBrand;
    private String[] proxyList;

    public List<String> getBrandInSite(List<String> brandsFromRest) {

        Random rand = new Random();
        int n = rand.nextInt(proxyList.length);

        String proxyString = proxyList[n];
        String[] proxyArray = proxyString.split(":");

        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            logger.error("Thread.sleep(" + sleepTime + ") is failed, cause" + ExceptionUtils.getStackTrace(e) + "\n"
                    + "Attempts well be continuance");
        }
        try {
            brandAndAddress = new HashMap<>();

            String response = getHttpResponse(siteAddress, sleepTime, timeout, proxyArray);
            if (response == null) {
                return null;
            }
            Document domHttpSite = Jsoup.parse(response);
            Elements table = domHttpSite.getElementById("manufacturersContent").getElementsByTag("select");
            Element selected = table.get(0);
            Elements allElements = selected.getAllElements();
            for (int i = 2; i < allElements.size(); i++) {
                try {
                    Element element = allElements.get(i);
                    String brand = ((TextNode) element.childNodes().get(0)).text();
                    brand = brand.toUpperCase();
                    brand = brand.trim();
                    if (!(brand.isEmpty())) {
                        Integer id = Integer.parseInt(element.val());
                        brandAndAddress.put(brand, id);
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
        try {

            Random rand = new Random();
            int n = rand.nextInt(proxyList.length);

            String proxyString = proxyList[n];
            String[] proxyArray = proxyString.split(":");

            String response = getHttpResponse(brandForSearch, sleepTime, timeout, proxyArray);
            Document domHttpSite = Jsoup.parse(response);
            Element productsListingTopNumber = domHttpSite.getElementById("productsListingTopNumber");
            Elements countOfPageDiv = productsListingTopNumber.children();
            Element element = countOfPageDiv.get(2);
            return (int) Math.ceil(Float.parseFloat(element.getElementsByTag("strong").text()) / 10);
        } catch (NullPointerException e) {
            logger.error("Cant find some jsoup element in dom structure \n" + ExceptionUtils.getStackTrace(e)
                    + "process \"get item by brands\" is interrupted");
            return null;
        }
    }

    public List<CompetitorPriceCheck> getItemByBrands(Brand brandsName, List<String> itemByBrandFromRest) {
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            logger.error("Thread.sleep(" + sleepTime + ") is failed, cause" + ExceptionUtils.getStackTrace(e) + "\n"
                    + "Attempts well be continuance");
        }
        brandCompetitorPriceList = new ArrayList<>();
        String brandNameInRest = brandsName.getBrandNameInRest();
        try {
            Set<String> brands = brandAndAddress.keySet();
            for (String brand : brands) {
                if (brandNameInRest.startsWith(brand)) {
                    Integer pageNumber = brandAndAddress.get(brand);
                    brandAndAddress.remove(brand);
                    brandAndAddress.put(brandNameInRest, pageNumber);
                    break;
                }
            }
            int brandPage = brandAndAddress.get(brandNameInRest);
            int productPageCount = getItemPageCount(patternToBrand + brandPage);
            ExecutorService executorService = Executors.newFixedThreadPool(parallelFixedThreadPoolForParseItem);
            count = new AtomicInteger(0);
            for (int brandProductPageNumber = 1; brandProductPageNumber <= productPageCount; brandProductPageNumber++) {
                int finalBrandProductPageNumber = brandProductPageNumber;
                executorService.submit(() -> parseItemPage(brandNameInRest, brandPage, finalBrandProductPageNumber));
            }
            executorService.shutdown();
            executorService.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);
            return ComparativeUtil.getExistItemInOurBase(brandCompetitorPriceList, itemByBrandFromRest);
        } catch (NullPointerException e) {
            logger.error("Cant find some jsoup element in dom structure" + ExceptionUtils.getStackTrace(e)
                    + "process \"get item by brands\" for brand " + brandsName + " is interrupted");
        } catch (InterruptedException e) {
            logger.error("Thread pool is Interrupted! Probably not all elements of brand was parsing, cause " + ExceptionUtils.getStackTrace(e));
        }
        return ComparativeUtil.getExistItemInOurBase(brandCompetitorPriceList, itemByBrandFromRest);
    }

    private void parseItemPage(String brandsName, Integer brandPage, Integer brandProductPageNumber) {
        Elements tableNodes;
        try {
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                logger.error("Thread.sleep(" + sleepTime + ") is failed, cause" + ExceptionUtils.getStackTrace(e) + "\n"
                        + "Attempts well be continuance");
            }
            Random rand = new Random();
            int n = rand.nextInt(proxyList.length);

            String proxyString = proxyList[n];
            String[] proxyArray = proxyString.split(":");

            String pageResponse = getHttpResponse(patternToBrand + brandPage + pagePatternToBrands + brandProductPageNumber, sleepTime, timeout, proxyArray);
            Document domHttpBrandProductPage = Jsoup.parse(pageResponse);
            Element catTable = domHttpBrandProductPage.getElementById("catTable").children().first();
            tableNodes = catTable.children();
        } catch (NullPointerException e) {
            logger.error("Cant find some jsoup element in dom structure" + ExceptionUtils.getStackTrace(e)
                    + "process \"get item by brands\" to brand" + brandsName + " page " + brandProductPageNumber + " is interrupted");
            return;
        }
        for (int i = 1; i < tableNodes.size(); i++) {
            try {
                Element productNode = tableNodes.get(i);
                Element nameNode = productNode.child(1);
                String itemName = nameNode.getElementsByTag("h3").get(0).getElementsByTag("a").text();
                Element child = productNode.child(2);
                Elements img = child.getElementsByTag("img");
                String stockCondition = "in stock";
                if (img != null && img.size() != 0) {
                    stockCondition = img.get(0).attr("title").trim().toLowerCase();
                }
                String price = child.text();
                synchronized (brandCompetitorPriceList) {
                    brandCompetitorPriceList = putNewCpcInMap(brandCompetitorPriceList, competitorId, brandsName, itemName, price, stockCondition);

                }

                count.incrementAndGet();
                if (count.get() == 100) {
                    count.set(0);
                    logger.info("brandCompetitorPriceList size for brand: " + brandsName + " is" + brandCompetitorPriceList.size());
                }

            } catch (NullPointerException e) {
                logger.error("Cant find some jsoup element in dom structure" + ExceptionUtils.getStackTrace(e)
                        + "process \"get item by brands\" to brand" + brandsName + " page " + brandProductPageNumber + " element  " + i + " is interrupted");
            }
        }
    }

    @Override
    public Integer getCompetitorId() {
        return competitorId;
    }

    public String getSiteAddress() {
        return siteAddress;
    }

    @Override
    public int getIncompleteMatchingBrand() {
       return incompleteMatchingBrand;
    }

    public void setSiteAddress(String siteAddress) {
        this.siteAddress = siteAddress;
    }

    public void setPatternToBrand(String patternToBrand) {
        this.patternToBrand = patternToBrand;
    }

    public void setPagePatternToBrands(String pagePatternToBrands) {
        this.pagePatternToBrands = pagePatternToBrands;
    }

    public void setSleepTime(Integer sleepTime) {
        this.sleepTime = sleepTime;
    }

    public void setCompetitorId(Integer competitorId) {
        this.competitorId = competitorId;
    }

    public Map<String, Integer> getBrandAndAddress() {
        return brandAndAddress;
    }

    public void setBrandAndAddress(Map<String, Integer> brandAndAddress) {
        this.brandAndAddress = brandAndAddress;
    }

    public Integer getParallelFixedThreadPoolForParseItem() {
        return parallelFixedThreadPoolForParseItem;
    }

    public void setParallelFixedThreadPoolForParseItem(Integer parallelFixedThreadPoolForParseItem) {
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



