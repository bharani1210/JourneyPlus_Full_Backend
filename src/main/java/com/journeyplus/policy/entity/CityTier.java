package com.journeyplus.policy.entity;
 
import com.journeyplus.common.EncryptedBigDecimalConverter;
import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;
 
@Entity
@Table(name = "city_tiers")
@Getter
@Setter
public class CityTier {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @Column(name = "city_name", nullable = false, unique = true, length = 100)
    private String cityName;
 
    @Column(nullable = false, length = 20)
    private String tier; // TIER_1, TIER_2, TIER_3
 
    @Convert(converter = EncryptedBigDecimalConverter.class)
    @Column(name = "daily_allowance_limit", nullable = false, length = 255)
    private BigDecimal dailyAllowanceLimit;
 
    public CityTier() {}
 
    public CityTier(String cityName, String tier, BigDecimal dailyAllowanceLimit) {
        this.cityName = cityName;
        this.tier = tier;
        this.dailyAllowanceLimit = dailyAllowanceLimit;
    }
}
