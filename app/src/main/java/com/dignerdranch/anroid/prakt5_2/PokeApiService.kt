package com.dignerdranch.anroid.prakt5_2

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

// Вложенный объект со спрайтами (картинками) покемона
data class Sprites(
    val front_default: String?
)

// Тип покемона: {"slot":1,"type":{"name":"electric"}}
data class TypeEntry(
    val slot: Int,
    val type: TypeDetail
)

data class TypeDetail(
    val name: String
)

data class PokemonResponse(
    val id: Int,
    val name: String,
    val height: Int,
    val weight: Int,
    val base_experience: Int,
    val sprites: Sprites?,
    val types: List<TypeEntry>?
)

interface PokeApiService {
    @GET("pokemon/{query}")
    suspend fun getPokemon(@Path("query") query: String): PokemonResponse

    companion object {
        // Важно: базовый URL должен заканчиваться на /api/v2/,
        // иначе запрос уходит на https://pokeapi.co/pokemon/... и сервер отдаёт 404
        private const val BASE_URL = "https://pokeapi.co/api/v2/"

        fun create(): PokeApiService {
            // Увеличенные таймауты: pokeapi.co отвечает очень медленно
            // (15-20 секунд на запрос), стандартных 10 секунд не хватает
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(PokeApiService::class.java)
        }
    }
}