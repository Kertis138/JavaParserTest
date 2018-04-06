package org.collectorOfCompetitorsPrices.restTheWrenchMonkeyAdapter;

import org.collectorOfCompetitorsPrices.models.brand.Brand;
import org.collectorOfCompetitorsPrices.models.competitor.Competitor;
import org.collectorOfCompetitorsPrices.models.competitorPriceCheck.CompetitorPriceCheck;

import java.util.List;
import java.util.Map;
import java.util.Set;


public interface HttpRequestToRestAdapter {

    Map<Integer, Competitor> getListCompetitorFromRest() ;

    List<String> getBrands();

    Map<Brand, List<String>> getItemNumberByBrand(Set<Brand> existOurBrandOnSite);

    boolean putOrUpdateNewCPC(List<CompetitorPriceCheck> CompetitorPriceCheckList);
}
