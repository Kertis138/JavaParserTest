package org.collectorOfCompetitorsPrices.models.brand;


import java.util.Set;

public class Brand {
    public Brand(String brandNameInRest, Set<String> brandNameInSite) {
        this.brandNameInRest = brandNameInRest;
        this.brandNameInSite = brandNameInSite;
    }

    private String brandNameInRest;
    private Set<String> brandNameInSite;

    public String getBrandNameInRest() {
        return brandNameInRest;
    }

    public void setBrandNameInRest(String brandNameInRest) {
        this.brandNameInRest = brandNameInRest;
    }

    public Set<String> getBrandNameInSite() {
        return brandNameInSite;
    }

    public void setBrandNameInSite(Set<String> brandNameInSite) {
        this.brandNameInSite = brandNameInSite;
    }
}
