package com.dignerdranch.anroid.prakt5_2

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

// Класс модели данных (вместо Entity из Room)
data class SavedPokemon(
    val id: Int,
    val name: String,
    val imageUrl: String?,
    val fact: String
)

// Отдельный файл базы, чтобы не конфликтовать с DatabaseHelper ("pokemon_db"),
// иначе onCreate() не будет вызван и таблица saved_pokemons не создастся
class MyDatabaseHelper(context: Context) : SQLiteOpenHelper(context, "saved_pokemons_db", null, 1) {

    override fun onCreate(db: SQLiteDatabase?) {
        // Создаем таблицу при первом запуске
        val createTable = """
            CREATE TABLE saved_pokemons (
                id INTEGER PRIMARY KEY,
                name TEXT,
                imageUrl TEXT,
                fact TEXT
            )
        """.trimIndent()
        db?.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS saved_pokemons")
        onCreate(db)
    }

    // Метод для сохранения (Добавления/Редактирования)
    fun savePokemon(pokemon: SavedPokemon) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put("id", pokemon.id)
            put("name", pokemon.name)
            put("imageUrl", pokemon.imageUrl)
            put("fact", pokemon.fact)
        }
        // REPLACE обновит запись, если такой id уже есть
        db.insertWithOnConflict("saved_pokemons", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        db.close()
    }

    // Метод для редактирования записи
    fun updatePokemon(pokemon: SavedPokemon) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put("name", pokemon.name)
            put("imageUrl", pokemon.imageUrl)
            put("fact", pokemon.fact)
        }
        db.update("saved_pokemons", values, "id = ?", arrayOf(pokemon.id.toString()))
        db.close()
    }

    // Метод для получения всех покемонов
    fun getAllPokemons(): List<SavedPokemon> {
        val pokemonList = mutableListOf<SavedPokemon>()
        val db = this.readableDatabase
        val cursor = db.query("saved_pokemons", null, null, null, null, null, null)

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                val imageUrl = cursor.getString(cursor.getColumnIndexOrThrow("imageUrl"))
                val fact = cursor.getString(cursor.getColumnIndexOrThrow("fact"))
                pokemonList.add(SavedPokemon(id, name, imageUrl, fact))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return pokemonList
    }

    // Метод для удаления
    fun deletePokemon(id: Int) {
        val db = this.writableDatabase
        db.delete("saved_pokemons", "id = ?", arrayOf(id.toString()))
        db.close()
    }
}