package org.collectorOfCompetitorsPrices.utils;

import org.apache.log4j.Logger;
import org.collectorOfCompetitorsPrices.models.brand.Brand;
import org.collectorOfCompetitorsPrices.models.competitorPriceCheck.CompetitorPriceCheck;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ComparativeUtil {

    //black magic
    private static ReentrantReadWriteLock compareBrandsRwrLock = new ReentrantReadWriteLock();
    private static ReentrantReadWriteLock getExistItemInOurBaseRwrLock = new ReentrantReadWriteLock();
    private static Lock compareBrandsLock = compareBrandsRwrLock.writeLock();
    private static Lock getExistItemInOurBaseLock = getExistItemInOurBaseRwrLock.writeLock();
    private static Logger logger = Logger.getLogger(ComparativeUtil.class);

    public static Set<Brand> compareBrands(List<String> brandListFromSite, List<String> brandListFromRest, int incompleteMatchingBrand) {
        compareBrandsLock.lock();

        Set<Brand> existOurBrandOnSite = new HashSet<>();
        Map<String, Set<String>> tempMap = new HashMap<>();
        for (int i = 0; i < brandListFromRest.size(); i++) {
            String brandFromRest = brandListFromRest.get(i);
            brandFromRest = brandFromRest.toUpperCase().trim();
            brandListFromRest.set(i, brandFromRest);
            for (String brandFromSite : brandListFromSite) {
                if (brandFromRest.equalsIgnoreCase(brandFromSite)) {
                    if (tempMap.containsKey(brandFromRest)) {
                        Set<String> strings = tempMap.get(brandFromRest);
                        strings.add(brandFromSite);
//                        existOurBrandOnSite.add(new Brand(brandFromRest, brandFromSite));
                    } else {
                        tempMap.put(brandFromRest, new HashSet<>(Collections.singletonList(brandFromSite)));
                    }
                    break;
                }
                // this is not entirely correct - just for CanadaAutoSupplyProcessor...
                else if (brandFromSite.endsWith("..")) {
                    if (brandFromRest.startsWith(brandFromSite)) {
                        if (tempMap.containsKey(brandFromRest)) {
                            Set<String> strings = tempMap.get(brandFromRest);
                            strings.add(brandFromSite);
//                        existOurBrandOnSite.add(new Brand(brandFromRest, brandFromSite));
                        } else {
                            tempMap.put(brandFromRest, new HashSet<>(Collections.singletonList(brandFromSite)));
                        }
                        break;
                    }
                } else if (incompleteMatchingBrand != 0) {
                    if (brandFromSite.contains(brandFromRest)) {
                        if (tempMap.containsKey(brandFromRest)) {
                            Set<String> strings = tempMap.get(brandFromRest);
                            strings.add(brandFromSite);
//                        existOurBrandOnSite.add(new Brand(brandFromRest, brandFromSite));
                        } else {
                            tempMap.put(brandFromRest, new HashSet<>(Collections.singletonList(brandFromSite)));
                        }
                    }
                }
            }
        }

        if (tempMap.isEmpty()) {
            logger.error("existOurBrandOnSite is empty. Process \"compare brands\" is interrupted");
            compareBrandsLock.unlock();
            return null;
        }
        for (Map.Entry<String, Set<String>> entry : tempMap.entrySet()) {
            existOurBrandOnSite.add(new Brand(entry.getKey(), entry.getValue()));
        }
        compareBrandsLock.unlock();

        return existOurBrandOnSite;
    }

    public static List<CompetitorPriceCheck> getExistItemInOurBase(List<CompetitorPriceCheck> itemListFromSite, List<String> itemNumberFromRestList) {
        getExistItemInOurBaseLock.lock();
        List<CompetitorPriceCheck> existItemInOurBase = new ArrayList<>();
        for (CompetitorPriceCheck competitorPriceCheck : itemListFromSite) {
            String itemNumberFromSite = competitorPriceCheck.getItemNumber();
            if (itemNumberFromSite.contains(itemNumberFromSite)) {
                existItemInOurBase.add(competitorPriceCheck);
            }
        }
        getExistItemInOurBaseLock.unlock();
        return existItemInOurBase;
    }
}
