package org.collectorOfCompetitorsPrices.models.competitorPriceCheck;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.collectorOfCompetitorsPrices.utils.CustomJsonDateTimeDeserializer;
import org.collectorOfCompetitorsPrices.utils.CustomJsonDateTimeSerializer;

import java.math.BigDecimal;
import java.util.Date;

@JacksonXmlRootElement(localName = "competitorPriceCheck")
public class CompetitorPriceCheck{

        private Integer id;
        private String brand;
        private String itemNumber;
        private Integer competitorId;
        private BigDecimal price;
        private String stockCondition;

        @JsonSerialize(using = CustomJsonDateTimeSerializer.class)
        @JsonDeserialize(using = CustomJsonDateTimeDeserializer.class)
        private Date datetime;

        private Integer quantityInStock;


        public Integer getId() {
                return id;
        }

        public void setId(Integer id) {
                this.id = id;
        }

        public String getBrand() {
                return brand;
        }

        public void setBrand(String brand) {
                this.brand = brand;
        }

        public String getItemNumber() {
                return itemNumber;
        }

        public void setItemNumber(String itemNumber) {
                this.itemNumber = itemNumber;
        }

        public Integer getCompetitorId() {
                return competitorId;
        }

        public void setCompetitorId(Integer competitorId) {
                this.competitorId = competitorId;
        }

        public BigDecimal getPrice() {
                return price;
        }

        public void setPrice(BigDecimal price) {
                this.price = price;
        }

        public String getStockCondition() {
                return stockCondition;
        }

        public void setStockCondition(String stockCondition) {
                this.stockCondition = stockCondition;
        }

        public Date getDatetime() {
                return datetime;
        }

        public void setDatetime(Date datetime) {
                this.datetime = datetime;
        }

        public Integer getQuantityInStock() {
                return quantityInStock;
        }

        public void setQuantityInStock(Integer quantityInStock) {
                this.quantityInStock = quantityInStock;
        }

        @Override
        public String toString() {
                return "CompetitorPriceCheck{" +
                        "id=" + id +
                        ", brand='" + brand + '\'' +
                        ", itemNumber='" + itemNumber + '\'' +
                        ", competitorId=" + competitorId +
                        ", price=" + price +
                        ", stockCondition='" + stockCondition + '\'' +
                        ", datetime=" + datetime +
                        ", quantityInStock=" + quantityInStock +
                        '}';
        }
}
