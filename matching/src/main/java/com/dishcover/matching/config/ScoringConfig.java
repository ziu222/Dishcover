package com.dishcover.matching.config;

import com.dishcover.common.ingredient.IngredientCatalog;
import com.dishcover.matching.scoring.AllergyFilterRule;
import com.dishcover.matching.scoring.CalorieProximityRule;
import com.dishcover.matching.scoring.EssentialWeightRule;
import com.dishcover.matching.scoring.ExpiryBonusRule;
import com.dishcover.matching.scoring.JaccardBaseRule;
import com.dishcover.matching.scoring.MatchingEngine;
import com.dishcover.matching.scoring.ScoringRule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Đăng ký tường minh thứ tự 4 rule (KHÔNG dựa vào auto-collect bean theo type, vì thứ tự
 * JaccardBase -> EssentialWeight -> ExpiryBonus -> AllergyFilter ảnh hưởng trực tiếp kết quả —
 * xem specs/matching-service.md mục 3.2).
 */
@Configuration
public class ScoringConfig {

    @Bean
    JaccardBaseRule jaccardBaseRule() {
        return new JaccardBaseRule();
    }

    @Bean
    EssentialWeightRule essentialWeightRule() {
        return new EssentialWeightRule();
    }

    @Bean
    ExpiryBonusRule expiryBonusRule() {
        return new ExpiryBonusRule();
    }

    @Bean
    CalorieProximityRule calorieProximityRule() {
        return new CalorieProximityRule();
    }

    @Bean
    AllergyFilterRule allergyFilterRule(IngredientCatalog catalog) {
        return new AllergyFilterRule(catalog);
    }

    @Bean
    List<ScoringRule> scoringRules(JaccardBaseRule jaccardBaseRule, EssentialWeightRule essentialWeightRule,
                                    ExpiryBonusRule expiryBonusRule, CalorieProximityRule calorieProximityRule,
                                    AllergyFilterRule allergyFilterRule) {
        return List.of(jaccardBaseRule, essentialWeightRule, expiryBonusRule, calorieProximityRule,
                allergyFilterRule);
    }

    @Bean
    MatchingEngine matchingEngine(List<ScoringRule> scoringRules) {
        return new MatchingEngine(scoringRules);
    }
}
