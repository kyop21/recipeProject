package com.example.recipeproject.util

import com.example.recipeproject.db.RecipeEntity

enum class DrinkCategory(val cardColorHex: String, val badgeColorHex: String) {
    COFFEE("#F4ECE1", "#D7BFA6"),   // 연브라운 (카드), 조금 더 짙은 브라운 (배지)
    SMOOTHIE("#E3F2FD", "#BBDEFB"), // 소프트 블루 (카드), 진한 파랑 (배지)
    ADE("#FFEAD2", "#FFD1A9"),      // 피치 오렌지 (카드), 진한 피치 (배지)
    TEA("#EDF6EC", "#C8E6C9"),      // 연초록 (카드), 진한 초록 (배지)
    WHITE("#FFFFFF", "#E0E0E0")     // 화이트 (카드), 연회색 (배지)
}

object CategoryThemeResolver {
    fun resolve(recipe: RecipeEntity): DrinkCategory {
        val name = recipe.name
        
        // 1. 레시피 내용 중 '샷'이 포함되어 있는지 전체 검사 (커피 판별)
        val hasShot = recipe.iceSteps.contains("샷") || 
                      recipe.hotSteps.contains("샷") ||
                      recipe.iceToppings.contains("샷") || 
                      recipe.hotToppings.contains("샷") ||
                      recipe.iceMemo.contains("샷") || 
                      recipe.hotMemo.contains("샷")
                      
        if (hasShot) {
            return DrinkCategory.COFFEE
        }
        
        // 2. 샷이 들어가지 않는 논커피 카테고리 판별
        return when {
            // 스무디/프라페/주스 등
            name.contains("스무디") || name.contains("프라페") || 
            name.contains("주스") || name.contains("크러쉬") || 
            name.contains("쉐이크") -> DrinkCategory.SMOOTHIE
            
            // 에이드류
            name.contains("에이드") || name.contains("콕") || 
            name.contains("모히또") -> DrinkCategory.ADE
            
            // 티류
            name.contains("차") || name.contains("티") || name.contains("녹차") -> DrinkCategory.TEA
            
            // 샷이 안 들어가는 고구마라떼, 딸기라떼, 녹차라떼 등은 화이트 처리
            else -> DrinkCategory.WHITE
        }
    }
}
