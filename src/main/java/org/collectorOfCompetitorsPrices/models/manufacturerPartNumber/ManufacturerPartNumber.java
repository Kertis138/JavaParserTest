package org.collectorOfCompetitorsPrices.models.manufacturerPartNumber;



import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.io.Serializable;



@JacksonXmlRootElement(localName = "manufacturerPartNumbers")
public class ManufacturerPartNumber implements Serializable {

        private static final long serialVersionUID = -2518022259432779136L;
        private String brand;
        private String itemNumber;

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


        @Override
        public String toString() {
                return "ManufacturerPartNumber{" +
                        "brand='" + brand + '\'' +
                        ", itemNumber='" + itemNumber + '\'' +
                        '}';
        }
}
