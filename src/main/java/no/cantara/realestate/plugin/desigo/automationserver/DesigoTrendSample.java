package no.cantara.realestate.plugin.desigo.automationserver;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.cantara.realestate.observations.TrendSample;

import java.time.Instant;

/*
    {
     "Value": "455",
     "Quality": "0",
     "QualityGood": true,
     "Timestamp": "2022-09-05T02:45:42.11Z"
    }
 */
public class DesigoTrendSample extends TrendSample {
    public static final String NUMMERIC_VALUE = "Value";
    private String trendId = null;

    @JsonProperty("Quality")
    private String quality = null;
    @JsonProperty("QualityGood")
    private Boolean isReliable = null;
    @JsonProperty("Timestamp")
    private Instant observedAt;

    @JsonProperty("Value")
    private Number value;

    private String objectId;

    private String propertyId;

    public DesigoTrendSample() {
    }

    public String getTrendId() {
        return trendId;
    }

    public void setTrendId(String trendId) {
        this.trendId = trendId;
    }


//    public void setSampleDate(Instant sampleDate) {
//        this.sampleDate = sampleDate;
//    }

    public void setTimestamp(String timestamp) {
//        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
//        LocalDate parsedDate = LocalDate.parse(timestamp, formatter);
       super.setObservedAt(Instant.parse(timestamp));
    }

    @Override
    public void setObservedAt(Instant observedAt) {
        super.setObservedAt(observedAt);
    }

    public String getQuality() {
        return quality;
    }

    public void setQuality(String quality) {
        this.quality = quality;
    }

    public Boolean isReliable() {
        return super.getReliable();
    }

    public void setReliable(Boolean reliable) {
        isReliable = reliable;
    }


    public String getPropertyId() {
        return propertyId;
    }

    public void setPropertyId(String propertyId) {
        this.propertyId = propertyId;
    }

    public boolean isValid() {
        return true; //FIXME validate DesigoTrendSample
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public String getObjectId() {
        return objectId;
    }




}
