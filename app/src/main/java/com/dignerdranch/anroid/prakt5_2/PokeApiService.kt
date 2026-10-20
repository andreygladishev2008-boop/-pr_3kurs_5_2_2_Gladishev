package com.dignerdranch.anroid.prakt5_2

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

data class PokemonResponse(
    val id: Int,
    val name: String,
    val height: Int,
    val weight: Int,
    val base_experience: Int
)

interface PokeApiService {
    @GET("pokemon/{query}")
    suspend fun getPokemon(@Path("query") query: String): PokemonResponse

    companion object {
        private const val BASE_URL = "https://pokeapi.co"

        fun create(): PokeApiService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(PokeApiService::class.java)
        }
    }
}