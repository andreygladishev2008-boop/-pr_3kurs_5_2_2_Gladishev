package com.dignerdranch.anroid.prakt5_2

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SearchHistory(val query: String, val timestamp: Long)

class MainActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private val apiService = PokeApiService.create()
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Инициализируем наш встроенный SQLite Хелпер
        dbHelper = DatabaseHelper(this)

        val etQuery = findViewById<EditText>(R.id.etQuery)
        val btnSearch = findViewById<Button>(R.id.btnSearch)
        val tvResult = findViewById<TextView>(R.id.tvResult)

        // Восстановление истории из JSON
        val sharedPrefs = getSharedPreferences("PokePrefs", Context.MODE_PRIVATE)
        val lastQueryJson = sharedPrefs.getString("last_search_json", null)
        if (lastQueryJson != null) {
            val lastQueryData = gson.fromJson(lastQueryJson, SearchHistory::class.java)
            etQuery.setText(lastQueryData.query)
        }

        btnSearch.setOnClickListener {
            val query = etQuery.text.toString().trim().lowercase()
            if (query.isEmpty()) {
                Toast.makeText(this, "Поле пустое!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Сохранение JSON в SharedPreferences
            val searchHistory = SearchHistory(query = query, timestamp = System.currentTimeMillis())
            sharedPrefs.edit().putString("last_search_json", gson.toJson(searchHistory)).apply()

            lifecycleScope.launch {
                try {
                    // Запрос в сеть к Poke API выполняем на фоновом потоке IO
                    val response = withContext(Dispatchers.IO) {
                        apiService.getPokemon(query)
                    }

                    val pokemon = Pokemon(
                        id = response.id,
                        name = response.name,
                        height = response.height,
                        weight = response.weight,
                        baseExperience = response.base_experience
                    )

                    // Сохранение в SQLite базу данных на фоновом потоке
                    withContext(Dispatchers.IO) {
                        dbHelper.insertPokemon(pokemon)
                    }

                    // Вывод информации на экран
                    tvResult.text = "Имя: ${pokemon.name}\nID: ${pokemon.id}\nРост: ${pokemon.height}\nВес: ${pokemon.weight}\nОпыт: ${pokemon.baseExperience}"
                    Toast.makeText(this@MainActivity, "Успешно сохранено в SQLite!", Toast.LENGTH_SHORT).show()

                } catch (e: Exception) {
                    e.printStackTrace()
                    tvResult.text = "Ошибка: Персонаж не найден или нет интернета"
                }
            }
        }
    }
}
