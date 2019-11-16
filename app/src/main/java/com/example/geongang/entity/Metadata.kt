package com.example.geongang

import com.fasterxml.jackson.annotation.JsonProperty

class Metadata(
    @field:JsonProperty("datetime") var datetime: String
)