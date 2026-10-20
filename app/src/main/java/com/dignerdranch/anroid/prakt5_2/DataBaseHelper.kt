package com.dignerdranch.anroid.prakt5_2

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class Pokemon(
    val id: Int,
    val name: String,
    val height: Int,
    val weight: Int,
    val baseExperience: Int
)

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "pokemon_db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_NAME = "pokemons"

        private const val KEY_ID = "id"
        private const val KEY_NAME = "name"
        private const val KEY_HEIGHT = "height"
        private const val KEY_WEIGHT = "weight"
        private const val KEY_EXP = "base_experience"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createTableQuery = ("CREATE TABLE $TABLE_NAME (" +
                "$KEY_ID INTEGER PRIMARY KEY," +
                "$KEY_NAME TEXT," +
                "$KEY_HEIGHT INTEGER," +
                "$KEY_WEIGHT INTEGER," +
                "$KEY_EXP INTEGER)")
        db?.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun insertPokemon(pokemon: Pokemon) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_ID, pokemon.id)
            put(KEY_NAME, pokemon.name)
            put(KEY_HEIGHT, pokemon.height)
            put(KEY_WEIGHT, pokemon.weight)
            put(KEY_EXP, pokemon.baseExperience)
        }
        db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        db.close()
    }
}