package com.example.geongang.entity

import com.fasterxml.jackson.annotation.JsonProperty

class Transaction(
    @field:JsonProperty("id") var id: String,
    @field:JsonProperty("operation") var operation: String,
    @field:JsonProperty("metadata") var metadata: Metadata
)