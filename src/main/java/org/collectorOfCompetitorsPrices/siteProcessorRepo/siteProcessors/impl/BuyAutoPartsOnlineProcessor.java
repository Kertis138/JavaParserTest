package org.collectorOfCompetitorsPrices.siteProcessorRepo.siteProcessors.impl;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.collectorOfCompetitorsPrices.models.brand.Brand;
import org.collectorOfCompetitorsPrices.models.competitorPriceCheck.CompetitorPriceCheck;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class BuyAutoPartsOnlineProcessor extends AbstractSiteProcessorImpl {
    private static Logger logger = Logger.getLogger(BuyAutoPartsOnlineProcessor.class);

    private Map<String, String> brandAndAddress;
    private Integer competitorId;
    private String siteAddress;
    private String patternToBrand;
    private String pagePatternToItem;
    private Integer sleepTime;
    private Integer timeout;
    private AtomicInteger count;
    private List<CompetitorPriceCheck> brandCompetitorPriceList;
    private Integer parallelFixedThreadPoolForParseItem;
    private int incompleteMatchingBrand;
    private String[] proxyList;


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

            String response = getHttpResponse(siteAddress + patternToBrand, sleepTime, timeout, proxyArray);
            if (response == null) {
                return null;
            }
            Document domHttpSite = Jsoup.parse(response);
            Elements manufacturerColumn = domHttpSite.getElementById("manufacturercolumn").children();
            Elements manufactureList = manufacturerColumn.get(0).children();
//            Elements manufactureList = manufacturerColumn.get(0).children().get(0).children();
            for (Element element : manufactureList) {
                try {
                    Element a = element.getElementsByTag("a").first();
                    if (a != null) {
                        String href = a.attr("href");
                        String brandName = "";
                        for (Element brandNameElement : a.children()) {
                            if (brandNameElement.text() != null && !brandNameElement.text().equals("")) {
                                brandName = brandNameElement.text();
                                break;
                            }
                        }

                        if (brandName.contains("(")) {
                            brandName = brandName.split("([(])")[0].trim();
                        }
                        brandAndAddress.put(brandName, href);
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

    public List<CompetitorPriceCheck> getItemByBrands(Brand brandsName, List<String> itemByBrandFromRest) {

        brandCompetitorPriceList = new ArrayList<>();
        for (String brandInSite :brandsName.getBrandNameInSite()) {


            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                logger.error("Thread.sleep(" + sleepTime + ") is failed, cause" + ExceptionUtils.getStackTrace(e) + "\n"
                        + "Attempts well be continuance");
            }
            try {
                String brandAddress = brandAndAddress.get(brandInSite).trim().replaceAll(String.valueOf((char) 160), "").replaceAll(String.valueOf((char) 32), "");
                count = new AtomicInteger(0);
                ExecutorService executorService = Executors.newFixedThreadPool(parallelFixedThreadPoolForParseItem);
                for (String item : itemByBrandFromRest) {
                    Future<Boolean> submit = executorService.submit(() -> parsePage(brandInSite, brandsName.getBrandNameInRest(), brandAddress, item));
                    try {
                        if (!submit.get()) {
                            if (!executorService.isShutdown()) {
                                executorService.shutdown();
                            }
                        }
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }
                }
                executorService.shutdown();
                executorService.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);
            } catch (NullPointerException e) {
                logger.error("Cant find some jsoup element in dom structure \n" + ExceptionUtils.getStackTrace(e)
                        + "process \"get item by brands\" is interrupted");
                return brandCompetitorPriceList;
            } catch (InterruptedException e) {
                logger.error("Thread pool is Interrupted! Probably not all elements of brand was parsing, cause " + ExceptionUtils.getStackTrace(e));
            }
        }
        return brandCompetitorPriceList;
    }
//String brandNameOur,
    private boolean parsePage(String brandsName, String brandNameOur, String brandAddress, String item) {
        if (getFailedRequestCounter().get() > 10) {
            return false;
        }
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

            String pageResponse = getHttpResponse(brandAddress + pagePatternToItem + item, sleepTime, timeout, proxyArray);
            if (pageResponse != null && !pageResponse.isEmpty()) {
                Document domHttpBrandProductPage = Jsoup.parse(pageResponse);
                Elements elemensList = domHttpBrandProductPage.getElementsByClass("list-item");
                if (elemensList.size() > 1) {
                    for (Element element : elemensList) {
                        String itemName = element.getElementsByClass("item-part-number text-danger").first().text().replace("Item : ", "").toUpperCase();
                        itemName = itemName.replace("-", "").replace(".", "").replace(",", "")
                                .replace(" ", "").replace("\\", "").replace("/", "").replaceAll(String.valueOf((char) 160), "").trim();
                        if (item.equals(itemName)) {
                            synchronized (brandCompetitorPriceList) {
                                brandCompetitorPriceList = addNewCPC(brandNameOur, brandCompetitorPriceList, elemensList);
                            }
                        }
                    }
                } else if (elemensList.size() == 1) {
                    logger.debug("Find element");
                    brandCompetitorPriceList = addNewCPC(brandNameOur, brandCompetitorPriceList, elemensList);

                }
            }
        } catch (NullPointerException e) {
            logger.error("Cant find some jsoup element in dom structure" + ExceptionUtils.getStackTrace(e)
                    + "process \"get item by brands\" to brand" + brandsName + " item " + item + " is interrupted");
        } catch (IllegalArgumentException e) {
            logger.error("Respone is empty!" + ExceptionUtils.getStackTrace(e)
                    + "process \"get item by brands\" to brand" + brandsName + " item " + item + " is interrupted");
        }
        return true;
    }
//String brandsNameOur,
    private List<CompetitorPriceCheck> addNewCPC(String brandNameOur, List<CompetitorPriceCheck> brandCompetitorPriceList, Elements elemensList) {
        Element element = elemensList.first();
        String itemName = element.getElementsByClass("item-part-number text-danger").first().text().replace("Item : ", "").toUpperCase();
        String price = element.getElementsByClass("buy-panel-sell-price").first().text();
        String stockCondition = element.getElementsByClass("buy-panel-stock").text().replaceAll(String.valueOf((char) 160), "").trim().toLowerCase();
        logger.debug("Find element " + brandNameOur + " price " + price + " stock condition " + stockCondition);
        brandCompetitorPriceList = putNewCpcInMap(brandCompetitorPriceList, competitorId, brandNameOur, itemName, price, stockCondition);
        count.incrementAndGet();
        if (count.get() == 100) {
            count.set(0);
            logger.info("brandCompetitorPriceList size for brand: " + brandNameOur + " is" + brandCompetitorPriceList.size());
        }
        return brandCompetitorPriceList;
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

    public void setPagePatternToItem(String pagePatterToBrands) {
        this.pagePatternToItem = pagePatterToBrands;
    }

    public void setSleepTime(Integer sleepTime) {
        this.sleepTime = sleepTime;
    }

    public Integer getItemPageCount(String brandForSearch) {
        throw new UnsupportedOperationException("This impl doesn't support add, cause this siteParser have different impl");
    }

    public Integer getParallelFixedThreadPoolForParseItem() {
        return parallelFixedThreadPoolForParseItem;
    }

    public void setParallelFixedThreadPoolForParseItem(Integer parallelFixedThreadPoolForParseItem) {
        this.parallelFixedThreadPoolForParseItem = parallelFixedThreadPoolForParseItem;
    }

    public void setIncompleteMatchingBrand(int incompleteMatchingBrand) {
        this.incompleteMatchingBrand = incompleteMatchingBrand;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public String[] getProxyList() {
        return proxyList;
    }

    public void setProxyList(String[] proxyList) {
        this.proxyList = proxyList;
    }
}
