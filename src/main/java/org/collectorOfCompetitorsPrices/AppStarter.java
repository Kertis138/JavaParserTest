package org.collectorOfCompetitorsPrices;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.collectorOfCompetitorsPrices.dispatcher.Dispatcher;
import org.collectorOfCompetitorsPrices.restTheWrenchMonkeyAdapter.impl.HttpRequestToRestAdapterImpl;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class AppStarter {
    private static Logger logger = Logger.getLogger(AppStarter.class);
    private static boolean hasErrors = false;

    public static void main(String[] args) throws Exception {
        ApplicationContext applicationContext = new ClassPathXmlApplicationContext("spring-container.xml");
        Dispatcher dispatcher = (Dispatcher) applicationContext.getBean("dispatcher");
        List<String> argsList = Arrays.asList(args);
        ExecutorService executorService = Executors.newFixedThreadPool(argsList.size());
        for (String arg : argsList) {
            executorService.submit( new Runnable() {
                @Override
                public void run() {
                	Map<String, Boolean> stringBooleanMap = parseSite(arg, dispatcher);
                	if (stringBooleanMap == null) {
                		hasErrors = true;
                	}
                	else hasErrors = false;
                }
            });
        }
        executorService.shutdown();
        try {
            executorService.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.error("Thread pool is Interrupted! Probably not all elements of brand was send in rest, cause " + ExceptionUtils.getStackTrace(e));
        }
        if (hasErrors) throw new Exception("error on parsing supplier");
    }

    private static Map<String, Boolean> parseSite(String arg, Dispatcher dispatcher) {
        try {
            Integer competitorId = Integer.parseInt(arg);
            Map<String, Boolean> stringBooleanMap = dispatcher.parseSite(competitorId);
            logger.info("brand parse status map: \n" + stringBooleanMap);
            logger.info("site parsing for competitionId " + competitorId + " is finish");
            return stringBooleanMap;
        } catch (NumberFormatException e) {
            logger.error("For input argument " + arg + "not found site processor!");
        }
		return null;
    }
}
