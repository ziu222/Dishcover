package com.dishcover.matching.config;

import com.dishcover.common.ingredient.IngredientCatalog;
import com.dishcover.matching.scoring.AllergyFilterRule;
import com.dishcover.matching.scoring.CalorieProximityRule;
import com.dishcover.matching.scoring.EssentialWeightRule;
import com.dishcover.matching.scoring.ExpiryBonusRule;
import com.dishcover.matching.scoring.JaccardBaseRule;
import com.dishcover.matching.scoring.MatchingEngine;
import com.dishcover.matching.scoring.ScoringRule;
import com.dishcover.matching.scoring.TagPreferenceRule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Đăng ký tường minh thứ tự rule (KHÔNG dựa vào auto-collect bean theo type, vì thứ tự
 * JaccardBase -> EssentialWeight -> ExpiryBonus -> CalorieProximity -> TagPreference -> AllergyFilter
 * ảnh hưởng trực tiếp kết quả — xem specs/matching-service.md mục 3.2. AllergyFilter (lọc cứng,
 * có quyền phủ quyết -∞) luôn đứng cuối cùng.
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
    TagPreferenceRule tagPreferenceRule() {
        return new TagPreferenceRule();
    }

    @Bean
    AllergyFilterRule allergyFilterRule(IngredientCatalog catalog) {
        return new AllergyFilterRule(catalog);
    }

    @Bean
    List<ScoringRule> scoringRules(JaccardBaseRule jaccardBaseRule, EssentialWeightRule essentialWeightRule,
                                    ExpiryBonusRule expiryBonusRule, CalorieProximityRule calorieProximityRule,
                                    TagPreferenceRule tagPreferenceRule, AllergyFilterRule allergyFilterRule) {
        return List.of(jaccardBaseRule, essentialWeightRule, expiryBonusRule, calorieProximityRule,
                tagPreferenceRule, allergyFilterRule);
    }

    @Bean
    MatchingEngine matchingEngine(List<ScoringRule> scoringRules) {
        return new MatchingEngine(scoringRules);
    }
}
