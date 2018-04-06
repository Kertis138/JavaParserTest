package org.collectorOfCompetitorsPrices.siteProcessorRepo.siteProcessors.impl;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.collectorOfCompetitorsPrices.models.competitorPriceCheck.CompetitorPriceCheck;
import org.collectorOfCompetitorsPrices.siteProcessorRepo.siteProcessors.SiteProcessor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.*;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


abstract class AbstractSiteProcessorImpl implements SiteProcessor {
    private static Logger logger = Logger.getLogger(AbstractSiteProcessorImpl.class);
    private int sendAttempts;
    private static AtomicInteger failedRequestCounter = new AtomicInteger(0);

    public abstract Integer getItemPageCount(String brandForSearch);

    public String getHttpResponse(String stingUrl, Integer timeOutForGetResponse, Integer timeout, String[] proxyArray) {

        Proxy proxy = null;
        if (proxyArray != null) {

            proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyArray[0], Integer.valueOf(proxyArray[1])));

            Authenticator authenticator = new Authenticator() {

                public PasswordAuthentication getPasswordAuthentication() {
                    return (new PasswordAuthentication(proxyArray[2],
                            proxyArray[3].toCharArray()));
                }
            };
            Authenticator.setDefault(authenticator);
        }
         try {
            URL url = new URL(stingUrl);
            HttpURLConnection httpURLConnection;
            try {

                httpURLConnection = (HttpURLConnection) url.openConnection(proxy);
                httpURLConnection.addRequestProperty("User-Agent", "Chrome/44.0.2403.157");
                // httpURLConnection = new ProxySetup().proxySetup(stingUrl);
                httpURLConnection.setRequestMethod("GET");
            } catch (IOException e) {
                logger.error("cant open connection or set request method, cause: " + ExceptionUtils.getStackTrace(e)
                        + "process \"send http requet\" is interrupted");
                return null;
            }
            int responseCode = 0;
            try {
                httpURLConnection.setConnectTimeout(timeout);
                logger.info("opening connection - " + stingUrl);
                httpURLConnection.connect();
                logger.info("connection opened");
                if (timeOutForGetResponse != null && timeOutForGetResponse > 0) {
                    logger.info("timeOutForGetResponse");
                    Thread.sleep(timeOutForGetResponse);
                }
                logger.info("getResponseCode");
                responseCode = httpURLConnection.getResponseCode();
                logger.info("for request - " + stingUrl + " responseCode " + responseCode);
            } catch (InterruptedException e) {
                logger.error("Thread.sleep(10000) is failed, cause" + ExceptionUtils.getStackTrace(e) + "\n"
                        + "Attempts well be continuance");
            } catch (IOException e) {
                logger.error("Cant get response code, cause: " + ExceptionUtils.getStackTrace(e)
                        + "process \"send http request\" is interrupted");
                return null;
            }
            if (responseCode != 200 & responseCode != 201 & responseCode != 302 & responseCode != 500) {
                logger.error("Attempt to send request № " + sendAttempts + " return empty response...\n" +
                        " try again...");
                try {
                    if (sendAttempts >= 5) {
                        sendAttempts = 0;
                        logger.error("After 5 attempts responseCode still not 200 or 201");
                        failedRequestCounter.set(failedRequestCounter.get()+1);
                        return null;
                    }
                    sendAttempts++;
                    if (timeOutForGetResponse != null) Thread.sleep(timeOutForGetResponse);
                    String request = getHttpResponse(stingUrl, timeOutForGetResponse, timeout, proxyArray);
                    if (failedRequestCounter.get() > 0) return null;
                    
                    if (request != null) {
                        sendAttempts = 0;
                        failedRequestCounter.set(0);
                        return request;
                    }
                    sendAttempts = 0;
                } catch (InterruptedException e) {
                    logger.error("Thread.sleep(10000) is failed, cause" + ExceptionUtils.getStackTrace(e) + "\n"
                            + "Attempts well be continuance");
                }
            }
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(httpURLConnection.getInputStream()))) {

                String inputLine;
                StringBuilder responseBuilder = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    responseBuilder.append(inputLine);
                }
                return responseBuilder.toString();
            } catch (IOException e) {
                logger.error("Cant read response InputStream, cause: " + ExceptionUtils.getStackTrace(e)
                        + "process \"send http requet\" is interrupted");
                return null;
            }
        } catch (MalformedURLException e) {
            logger.error("Cant read URL, cause: " + ExceptionUtils.getStackTrace(e)
                    + "process \"send http requet\" is interrupted");
            return null;
        }
    }

    List<CompetitorPriceCheck> putNewCpcInMap(List<CompetitorPriceCheck> brandOnCompetitorPriceList,
                                              Integer competitorId, String brandsName, String itemName,
                                              String price, String stockCondition) {
        if ((itemName != null && !itemName.isEmpty()) && (price != null && !price.isEmpty())) {
            brandOnCompetitorPriceList.add(createNewCpc(competitorId, brandsName, itemName, price, stockCondition));
            return brandOnCompetitorPriceList;
        } else {
            logger.error("Empty price or item number in " + brandsName + ", " + itemName + "! Process \"put new CompetitorPriceCheck in map\" is interrupted");
            return brandOnCompetitorPriceList;
        }
    }


    private CompetitorPriceCheck createNewCpc(Integer competitorId, String brandsName, String itemName, String price,
                                              String stockCondition) {
        itemName = itemName.toUpperCase();
        if (itemName.contains(brandsName)) {
            itemName = itemName.replace(brandsName, "");
        }
        itemName = itemName.replace("-", "").replace(".", "").replace(",", "").replace(" ", "").trim();
        price = price.replace("Add:", "").replace("... more info", "")
                .replace("$", "").replace(",", "").replace("Price:", "")
                .replaceAll(" ", "").replaceAll(String.valueOf((char) 160), "").trim();
        CompetitorPriceCheck competitorPriceCheck = new CompetitorPriceCheck();

        competitorPriceCheck.setBrand(brandsName);
        competitorPriceCheck.setItemNumber(itemName);

        competitorPriceCheck.setPrice(new BigDecimal(price));
        competitorPriceCheck.setDatetime(new Date());
        competitorPriceCheck.setCompetitorId(competitorId);
        if(stockCondition != null && !stockCondition.isEmpty()){
            stockCondition=stockCondition.toLowerCase();
            if (!stockCondition.contains("in stock")){
                competitorPriceCheck.setStockCondition("not in stock");
            }
            else {
                competitorPriceCheck.setStockCondition("in stock");
            }
            Pattern p = Pattern.compile("-?\\d+");
            Matcher m = p.matcher(stockCondition);
            while (m.find()) {
                competitorPriceCheck.setQuantityInStock(Integer.valueOf(m.group()));
            }
        }
        return competitorPriceCheck;
    }


    static AtomicInteger getFailedRequestCounter() {
        return failedRequestCounter;
    }

    public static void setFailedRequestCounter(AtomicInteger failedRequestCounter) {
        AbstractSiteProcessorImpl.failedRequestCounter = failedRequestCounter;
    }

    public int getSendAttempts() {
        return sendAttempts;
    }

    public void setSendAttempts(int sendAttempts) {
        this.sendAttempts = sendAttempts;
    }
}
