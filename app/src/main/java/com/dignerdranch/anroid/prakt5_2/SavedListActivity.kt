package com.dignerdranch.anroid.prakt5_2

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class SavedListActivity : AppCompatActivity() {

    private lateinit var dbHelper: MyDatabaseHelper
    private lateinit var listView: ListView
    private lateinit var adapter: ArrayAdapter<String>
    private var pokemons: List<SavedPokemon> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_saved_list)

        dbHelper = MyDatabaseHelper(this)
        listView = findViewById(R.id.listView)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1)
        listView.adapter = adapter

        // Добавление новой записи
        findViewById<Button>(R.id.btnAdd).setOnClickListener {
            showEditDialog(null)
        }

        // По клику на элемент — выбор действия: редактировать или удалить
        listView.setOnItemClickListener { _, _, position, _ ->
            showActionsDialog(pokemons[position])
        }
    }

    override fun onResume() {
        super.onResume()
        loadList()
    }

    private fun loadList() {
        pokemons = dbHelper.getAllPokemons()
        adapter.clear()
        adapter.addAll(pokemons.map { "${it.name} — ${it.fact}" })
        adapter.notifyDataSetChanged()

        if (pokemons.isEmpty()) {
            Toast.makeText(this, "Сохранённых записей пока нет", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showActionsDialog(pokemon: SavedPokemon) {
        val options = arrayOf("Редактировать", "Удалить")
        AlertDialog.Builder(this)
            .setTitle(pokemon.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditDialog(pokemon)
                    1 -> {
                        dbHelper.deletePokemon(pokemon.id)
                        Toast.makeText(this, "Запись удалена", Toast.LENGTH_SHORT).show()
                        loadList()
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showEditDialog(pokemon: SavedPokemon?) {
        val view = layoutInflater.inflate(R.layout.dialog_pokemon, null)
        val etName = view.findViewById<EditText>(R.id.etName)
        val etFact = view.findViewById<EditText>(R.id.etFact)

        pokemon?.let {
            etName.setText(it.name)
            etFact.setText(it.fact)
        }

        AlertDialog.Builder(this)
            .setTitle(if (pokemon == null) "Добавить персонажа" else "Редактировать запись")
            .setView(view)
            .setPositiveButton("Сохранить") { _, _ ->
                val name = etName.text.toString().trim()
                val fact = etFact.text.toString().trim()

                if (name.isEmpty() || fact.isEmpty()) {
                    Toast.makeText(this, "Заполните все поля!", Toast.LENGTH_SHORT).show()
                } else {
                    val newPokemon = SavedPokemon(
                        id = pokemon?.id ?: System.currentTimeMillis().toInt(),
                        name = name,
                        imageUrl = pokemon?.imageUrl,
                        fact = fact
                    )
                    if (pokemon == null) {
                        dbHelper.savePokemon(newPokemon)
                        Toast.makeText(this, "Запись добавлена", Toast.LENGTH_SHORT).show()
                    } else {
                        dbHelper.updatePokemon(newPokemon)
                        Toast.makeText(this, "Запись изменена", Toast.LENGTH_SHORT).show()
                    }
                    loadList()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
}
