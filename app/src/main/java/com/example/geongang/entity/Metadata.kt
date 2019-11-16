package com.example.geongang.entity

import com.fasterxml.jackson.annotation.JsonProperty

class Metadata(
    @field:JsonProperty("datetime") var datetime: String
)