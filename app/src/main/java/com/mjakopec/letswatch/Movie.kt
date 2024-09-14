package com.mjakopec.letswatch

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class MovieApiResponse(
    val page: Int,
    val results: List<Movie>
)

@Serializable
data class Movie(
    val title: String,
    val overview: String,
    val backdrop_path: String?,
    val release_date: String,
    val vote_average: Double
)