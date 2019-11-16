package com.example.geongang.entity

import com.fasterxml.jackson.annotation.JsonProperty

class Emg(
    @field:JsonProperty("transaction") var transaction: Transaction,
    @field:JsonProperty("exerciseTime") var exerciseTime: Float,
    @field:JsonProperty("monthlyPremium") var monthlyPremium: Float
)