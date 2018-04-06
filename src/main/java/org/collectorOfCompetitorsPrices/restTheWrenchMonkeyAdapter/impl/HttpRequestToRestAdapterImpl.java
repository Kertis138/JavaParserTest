package org.collectorOfCompetitorsPrices.restTheWrenchMonkeyAdapter.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.collectorOfCompetitorsPrices.models.RestResponsePage;
import org.collectorOfCompetitorsPrices.models.brand.Brand;
import org.collectorOfCompetitorsPrices.models.competitor.Competitor;
import org.collectorOfCompetitorsPrices.models.competitorPriceCheck.CompetitorPriceCheck;
import org.collectorOfCompetitorsPrices.restTheWrenchMonkeyAdapter.HttpRequestToRestAdapter;
import org.springframework.data.domain.Page;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class HttpRequestToRestAdapterImpl implements HttpRequestToRestAdapter {

    private final String URL_DOMAIN;
    private ObjectMapper objectMapper = new ObjectMapper();

    private static Logger logger = Logger.getLogger(HttpRequestToRestAdapterImpl.class);
    private final String URL_AUTHORIZE_USER;
    private final String URL_AUTHORIZE_PASSWORD;
    private final String URL_COMPETITOR;
    private final String URL_COMPETITOR_PRICE_CHECK_UPDATE_ALL;
    private final String URL_MANUFACTURER_PART_NUMBER_FIND_BRANDS;

    private boolean isSuccessfulSending = false;
    private final String URL_MANUFACTURER_PART_NUMBER_FIND_ITEM_BY_BRANDS;
    private String URL_AUTHORIZE;
    private final int optimalSizeForAsyncRequest;
    private int sendAttempts;

    public HttpRequestToRestAdapterImpl(String URL_AUTHORIZE_USER, String URL_AUTHORIZE_PASSWORD, String URL_DOMAIN, String URL_COMPETITOR, String URL_COMPETITOR_PRICE_CHECK_UPDATE_ALL, String URL_MANUFACTURER_PART_NUMBER_FIND_BRANDS, String URL_MANUFACTURER_PART_NUMBER_FIND_ITEM_BY_BRANDS, int optimalSizeForAsyncRequest) {
        this.URL_AUTHORIZE_USER = URL_AUTHORIZE_USER;
        this.URL_AUTHORIZE_PASSWORD = URL_AUTHORIZE_PASSWORD;
        this.URL_DOMAIN = URL_DOMAIN;
        this.URL_COMPETITOR = URL_COMPETITOR;
        this.URL_COMPETITOR_PRICE_CHECK_UPDATE_ALL = URL_COMPETITOR_PRICE_CHECK_UPDATE_ALL;
        this.URL_MANUFACTURER_PART_NUMBER_FIND_BRANDS = URL_MANUFACTURER_PART_NUMBER_FIND_BRANDS;
        this.URL_MANUFACTURER_PART_NUMBER_FIND_ITEM_BY_BRANDS = URL_MANUFACTURER_PART_NUMBER_FIND_ITEM_BY_BRANDS;
        this.optimalSizeForAsyncRequest = optimalSizeForAsyncRequest;
    }


    @Override
    public Map<Integer, Competitor> getListCompetitorFromRest() {
        //return new HashMap<Integer, Competitor>(){{put(1,new Competitor());}};
        String response = sendHttpRequest(URL_COMPETITOR, "GET", null);
        JsonNode competitorJson;
        Page<Competitor> competitorList;
        try {
            competitorJson = objectMapper.readTree(response);
            competitorList = ((RestResponsePage<Competitor>)objectMapper.readValue(String.valueOf(competitorJson), new TypeReference<RestResponsePage<Competitor>>() {})).pageImpl();
        } catch (IOException e) {
            logger.error("objectMapper cant transformation json, cause: " + ExceptionUtils.getStackTrace(e));
            return null;
        }
        Map<Integer, Competitor> competitorMap = new HashMap<>();
        for (Competitor competitor : competitorList) {
            competitorMap.put(competitor.getId(), competitor);
        }
        return competitorMap;
    }

    @Override
    public List<String> getBrands() {
        String response;
        response = sendHttpRequest(URL_MANUFACTURER_PART_NUMBER_FIND_BRANDS, "GET", null);
        if (response == null || response.isEmpty()) {
            logger.error("response is empty. Process \"get brands\" is interrupted");
            return null;
        }
        JsonNode brandsListJson;
        try {
            brandsListJson = objectMapper.readTree(response);
            return new ArrayList<>(Arrays.asList(objectMapper.treeToValue(brandsListJson, String[].class)));
        } catch (IOException e) {
            logger.error("objectMapper cant transformation object " + response + ", cause: " + ExceptionUtils.getStackTrace(e)
                    + "process \"getting item number by brandt\" is interrupted");
            return null;
        }
    }


    @Override
    public Map<Brand, List<String>> getItemNumberByBrand(Set<Brand> existOurBrandOnSiteList) {
        String response = null;
        try {
            List<String> brandList = existOurBrandOnSiteList.stream().map(Brand::getBrandNameInRest).collect(Collectors.toCollection(ArrayList::new));
            response = sendHttpRequest(URL_MANUFACTURER_PART_NUMBER_FIND_ITEM_BY_BRANDS, "POST", objectMapper.writeValueAsString(brandList));
            Map<String, List<String>> stringListMap = objectMapper.readValue(response, new TypeReference<Map<String, List<String>>>() {
            });
            Map<Brand, List<String>> brandListMap = new HashMap<>();
            for (Brand brand : existOurBrandOnSiteList) {
                brandListMap.put(brand, stringListMap.get(brand.getBrandNameInRest()));
            }
            return brandListMap;
        } catch (JsonProcessingException e) {
            logger.error("objectMapper cant transformation object " + existOurBrandOnSiteList + ", cause: " + ExceptionUtils.getStackTrace(e)
                    + "process \"getting item number by brandt\" is interrupted");
            return null;
        } catch (IOException e) {
            logger.error("objectMapper cant transformation object " + response + ", cause: " + ExceptionUtils.getStackTrace(e)
                    + "process \"getting item number by brandt\" is interrupted");
            return null;
        }
    }

    @Override
    public boolean putOrUpdateNewCPC(List<CompetitorPriceCheck> competitorPriceCheckList) {
        double countOfPart = Math.ceil(competitorPriceCheckList.size() / optimalSizeForAsyncRequest);
        List<List<CompetitorPriceCheck>> listOfSubList;
        if (countOfPart > 0) {
            int from = 0;
            int to = optimalSizeForAsyncRequest;
            listOfSubList = new ArrayList<>();
            for (int i = 0; i < countOfPart; i++) {
                List<CompetitorPriceCheck> competitorPriceCheckSubList = competitorPriceCheckList.subList(from, to);
                logger.info("new sublist - " + competitorPriceCheckSubList.size());
                logger.debug("new sublist - " + competitorPriceCheckSubList);
                listOfSubList.add(competitorPriceCheckSubList);
                from = i * optimalSizeForAsyncRequest;
                to = (i + 1) * optimalSizeForAsyncRequest;
                if (competitorPriceCheckList.size() < to) {
                    to = competitorPriceCheckList.size();
                }
            }
        } else {
            listOfSubList = new ArrayList<>(Collections.singletonList(competitorPriceCheckList));
        }
        logger.info("start parallel processing");
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        for (List<CompetitorPriceCheck> competitorPriceCheckSubList : listOfSubList) {
            executorService.submit(() -> sendToRest(competitorPriceCheckSubList));
        }
        executorService.shutdown();
        try {
            executorService.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.error("Thread pool is Interrupted! Probably not all elements of brand was send in rest, cause " + ExceptionUtils.getStackTrace(e));
            return isSuccessfulSending;
        }
        setSuccessfulSending(true);
        return isSuccessfulSending;
    }

    private void sendToRest(List<CompetitorPriceCheck> p) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String param = objectMapper.writeValueAsString(p);
            logger.info("Current thread " + Thread.currentThread());
            logger.debug("update entity list: \n" + param);
            sendHttpRequest(URL_COMPETITOR_PRICE_CHECK_UPDATE_ALL, "PUT", param);
        } catch (Exception e) {
            logger.error("ObjectMapper cant transformation object\n" +
                    "processing sending subList " + p + " is interrupted, cause: " + ExceptionUtils.getStackTrace(e));
            this.setSuccessfulSending(false);
        }
    }


    private String sendHttpRequest(String stingUrl, String requestMethod, Object param) {
        try {
            URL url = new URL(URL_DOMAIN + stingUrl);
            HttpURLConnection httpURLConnection;
            try {
                httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setRequestMethod(requestMethod);
            } catch (IOException e) {
                logger.error("cant open connection or set request method, cause: " + ExceptionUtils.getStackTrace(e)
                        + "process \"send http requet\" is interrupted");
                return null;
            }
            httpURLConnection.setRequestProperty("Authorization", "Basic " + getAuthorize());
            httpURLConnection.setRequestProperty("Accept", "application/json; charset=utf-8");

            httpURLConnection.addRequestProperty("Content-Type", "application/json");
            if (param != null) {
                httpURLConnection.setDoOutput(true);
                if (param instanceof String) {
                    String query = (String) param;
                    if (!query.isEmpty()) {
                        httpURLConnection.setRequestProperty("Content-Length", Integer.toString(query.length()));
                        try {
                            httpURLConnection.getOutputStream().write(query.getBytes("UTF8"));
                        } catch (IOException e) {
                            logger.error("Cant write OutputStream, cause: " + ExceptionUtils.getStackTrace(e)
                                    + "process \"send http requet\" is interrupted");
                            return null;
                        }
                    } else {
                        logger.warn("Http request " + httpURLConnection.toString() + " send empty body!");
                    }
                } else {
                    logger.warn("Http request " + httpURLConnection.toString() + " have unidentified type of request body!");
                }
            }

            int responseCode;
            try {
                responseCode = httpURLConnection.getResponseCode();
                logger.info("for request - " + URL_DOMAIN + stingUrl + " responseCode " + responseCode);
            } catch (IOException e) {
                logger.error("Cant get response code, cause: " + ExceptionUtils.getStackTrace(e)
                        + "process \"send http request\" is interrupted");
                return null;
            }
            if (responseCode != 200 & responseCode != 201) {
                logger.error("Attempt to send request № " + sendAttempts + "return empty response...\n" +
                        " try again...");
                try {
                    if (sendAttempts > 5) {
                        sendAttempts = 0;
                        logger.error("After 5 attempts responseCode still not 200 or 201");
                        return null;
                    }
                    sendAttempts++;
                    Thread.sleep(10000);
                    String request = sendHttpRequest(stingUrl, requestMethod, param);
                    if (request != null) {
                        sendAttempts = 0;
                        return request;
                    }

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

    private String getAuthorize() {
        if (URL_AUTHORIZE == null || URL_AUTHORIZE.isEmpty()) {
            URL_AUTHORIZE = URL_AUTHORIZE_USER + ":" + URL_AUTHORIZE_PASSWORD;
            URL_AUTHORIZE = new String(Base64.encodeBase64(URL_AUTHORIZE.getBytes()));
        }
        return URL_AUTHORIZE;
    }

    private void setSuccessfulSending(boolean successfulSending) {
        isSuccessfulSending = successfulSending;
    }
}
