package com.example.recipeproject.model

data class Variant(
    var steps: String = "",
    var toppings: String = ""
)

data class RecipeDraft(
    var name: String = "",
    val ice: Variant = Variant(),
    val hot: Variant = Variant()
)