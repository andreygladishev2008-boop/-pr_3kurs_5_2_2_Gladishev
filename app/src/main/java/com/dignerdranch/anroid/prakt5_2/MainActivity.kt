package com.dignerdranch.anroid.prakt5_2

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SearchHistory(val query: String, val timestamp: Long)

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "PokeAPI"

        // Перевод типов покемонов на русский
        private val TYPE_NAMES = mapOf(
            "normal" to "Обычный", "fire" to "Огненный", "water" to "Водный",
            "electric" to "Электро", "grass" to "Травяной", "ice" to "Ледяный",
            "fighting" to "Боевой", "poison" to "Ядовитый", "ground" to "Земляной",
            "flying" to "Летающий", "psychic" to "Психический", "bug" to "Насекомое",
            "rock" to "Каменный", "ghost" to "Призрачный", "dragon" to "Дракон",
            "dark" to "Тёмный", "steel" to "Стальной", "fairy" to "Фейри"
        )
    }

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var savedDbHelper: MyDatabaseHelper
    private val apiService = PokeApiService.create()
    private val gson = Gson()

    // Последний успешно найденный покемон (для кнопки "Добавить в сохранённые")
    private var lastPokemon: Pokemon? = null
    // URL спрайта и тип последнего найденного покемона
    private var lastSpriteUrl: String? = null
    private var lastType: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Инициализируем наш встроенный SQLite Хелпер
        dbHelper = DatabaseHelper(this)
        // Хелпер экрана "Сохранённые персонажи"
        savedDbHelper = MyDatabaseHelper(this)

        val etQuery = findViewById<EditText>(R.id.etQuery)
        val btnSearch = findViewById<Button>(R.id.btnSearch)
        val tvResult = findViewById<TextView>(R.id.tvResult)
        val btnSaved = findViewById<Button>(R.id.btnSaved)
        val btnAddSaved = findViewById<Button>(R.id.btnAddSaved)
        val ivPokemon = findViewById<ImageView>(R.id.ivPokemon)

        // Приветствие с именем пользователя из SharedPreferences
        val userPrefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val username = userPrefs.getString("username", "гость") ?: "гость"
        findViewById<TextView>(R.id.tvGreeting).text = "Привет, $username!"

        // Переход на экран сохранённых записей
        btnSaved.setOnClickListener {
            startActivity(Intent(this, SavedListActivity::class.java))
        }

        // Контрастная кнопка: добавление найденного покемона
        // в сохранённые персонажи (экран списка)
        btnAddSaved.setOnClickListener {
            val pokemon = lastPokemon
            if (pokemon == null) {
                Toast.makeText(this, "Сначала найдите персонажа!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // "Факт" о персонаже — основные характеристики в человекочитаемых единицах
            val fact = "Рост: ${"%.1f".format(pokemon.height / 10.0)} м, " +
                    "вес: ${"%.1f".format(pokemon.weight / 10.0)} кг, " +
                    "опыт: ${pokemon.baseExperience}"

            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    // REPLACE: если такой покемон уже есть — запись обновится
                    savedDbHelper.savePokemon(
                        SavedPokemon(
                            id = pokemon.id,
                            name = pokemon.name,
                            imageUrl = lastSpriteUrl,
                            fact = fact,
                            type = lastType
                        )
                    )
                }
                // Блокируем кнопку до следующего поиска, чтобы не дублировать
                btnAddSaved.isEnabled = false
                btnAddSaved.alpha = 0.5f
                Toast.makeText(
                    this@MainActivity,
                    "«${pokemon.name}» добавлен в сохранённые!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

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
                // Показываем, что идёт загрузка
                tvResult.text = "Загрузка..."

                try {
                    Log.d(TAG, "Запрос к Poke API: '$query', время: ${System.currentTimeMillis()}")
                    // Запрос в сеть к Poke API выполняем на фоновом потоке IO
                    val response = withContext(Dispatchers.IO) {
                        apiService.getPokemon(query)
                    }
                    Log.d(TAG, "Успешный ответ для запроса '$query'")

                    val pokemon = Pokemon(
                        id = response.id,
                        name = response.name,
                        height = response.height,
                        weight = response.weight,
                        baseExperience = response.base_experience
                    )

                    // Выводим ответ API в Logcat в формате JSON (для отладки)
                    Log.d(TAG, "Ответ Poke API (JSON): ${gson.toJson(response)}")

                    // Показываем картинку покемона (Coil качает её в фоне и кэширует).
                    // Если спрайт отсутствует — прячем картинку
                    val spriteUrl = response.sprites?.front_default
                    if (spriteUrl != null) {
                        ivPokemon.visibility = View.VISIBLE
                        ivPokemon.load(spriteUrl)
                    } else {
                        ivPokemon.visibility = View.GONE
                    }
                    lastSpriteUrl = spriteUrl

                    // Типы покемона (обычно 1-2), сортируем по слоту и переводим
                    val typeNames = response.types
                        ?.sortedBy { it.slot }
                        ?.joinToString(", ") { TYPE_NAMES[it.type.name] ?: it.type.name }
                        ?: "неизвестен"
                    lastType = typeNames

                    // Сохранение в SQLite базу данных на фоновом потоке
                    withContext(Dispatchers.IO) {
                        dbHelper.insertPokemon(pokemon)
                    }

                    // Вывод информации на экран.
                    // PokeAPI отдаёт рост в дециметрах (1 = 10 см), вес в гектограммах (1 = 100 г),
                    // поэтому переводим в метры и килограммы
                    val heightMeters = pokemon.height / 10.0
                    val weightKg = pokemon.weight / 10.0
                    tvResult.text = "Имя: ${pokemon.name}\n" +
                            "ID: ${pokemon.id}\n" +
                            "Тип: $typeNames\n" +
                            "Рост: ${"%.1f".format(heightMeters)} м\n" +
                            "Вес: ${"%.1f".format(weightKg)} кг\n" +
                            "Базовый опыт (очки): ${pokemon.baseExperience}"
                    Toast.makeText(this@MainActivity, "Успешно сохранено в SQLite!", Toast.LENGTH_SHORT).show()

                    // Покемон найден — активируем кнопку добавления в сохранённые
                    lastPokemon = pokemon
                    btnAddSaved.isEnabled = true
                    btnAddSaved.alpha = 1f

                } catch (e: retrofit2.HttpException) {
                    // Ответ сервера пришёл, но с ошибкой (например, 404 — персонаж не найден)
                    Log.e(TAG, "Ошибка HTTP ${e.code()} для запроса '$query'", e)
                    lastPokemon = null
                    lastSpriteUrl = null
                    lastType = ""
                    ivPokemon.visibility = View.GONE
                    btnAddSaved.isEnabled = false
                    btnAddSaved.alpha = 0.5f
                    if (e.code() == 404) {
                        tvResult.text = "Персонаж '$query' не найден. Проверьте имя или ID."
                    } else {
                        tvResult.text = "Ошибка сервера: HTTP ${e.code()}"
                    }
                } catch (e: Exception) {
                    // Проблемы с сетью: нет интернета, DNS, таймаут и т.д.
                    Log.e(TAG, "Ошибка сети для запроса '$query': ${e.javaClass.simpleName}: ${e.message}", e)
                    lastPokemon = null
                    lastSpriteUrl = null
                    lastType = ""
                    ivPokemon.visibility = View.GONE
                    btnAddSaved.isEnabled = false
                    btnAddSaved.alpha = 0.5f
                    tvResult.text = "Ошибка сети: ${e.message ?: e.javaClass.simpleName}\nПроверьте подключение к интернету."
                }
            }
        }
    }
}
