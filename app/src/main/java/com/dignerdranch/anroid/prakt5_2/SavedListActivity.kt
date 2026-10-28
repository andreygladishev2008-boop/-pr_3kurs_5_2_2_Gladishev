package com.dignerdranch.anroid.prakt5_2

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import coil.load

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

        // По клику на элемент — открывается карточка персонажа
        listView.setOnItemClickListener { _, _, position, _ ->
            showCardDialog(pokemons[position])
        }
    }

    override fun onResume() {
        super.onResume()
        loadList()
    }

    private fun loadList() {
        pokemons = dbHelper.getAllPokemons()
        adapter.clear()
        adapter.addAll(pokemons.map {
            if (it.type.isNotEmpty()) "${it.name} (${it.type}) — ${it.fact}"
            else "${it.name} — ${it.fact}"
        })
        adapter.notifyDataSetChanged()

        if (pokemons.isEmpty()) {
            Toast.makeText(this, "Сохранённых записей пока нет", Toast.LENGTH_SHORT).show()
        }
    }

    // Увеличенная карточка персонажа: картинка, имя, тип, факт
    private fun showCardDialog(pokemon: SavedPokemon) {
        val view = layoutInflater.inflate(R.layout.dialog_card, null)
        val ivCard = view.findViewById<ImageView>(R.id.ivCard)
        val tvName = view.findViewById<TextView>(R.id.tvCardName)
        val tvType = view.findViewById<TextView>(R.id.tvCardType)
        val tvFact = view.findViewById<TextView>(R.id.tvCardFact)
        val tvId = view.findViewById<TextView>(R.id.tvCardId)

        tvName.text = pokemon.name
        tvFact.text = pokemon.fact
        tvId.text = "ID: ${pokemon.id}"

        if (pokemon.type.isNotEmpty()) {
            tvType.text = pokemon.type
            tvType.visibility = android.view.View.VISIBLE
        } else {
            tvType.visibility = android.view.View.GONE
        }

        if (pokemon.imageUrl != null) {
            ivCard.visibility = android.view.View.VISIBLE
            ivCard.load(pokemon.imageUrl)
        } else {
            ivCard.visibility = android.view.View.GONE
        }

        AlertDialog.Builder(this)
            .setView(view)
            .setPositiveButton("Редактировать") { _, _ ->
                showEditDialog(pokemon)
            }
            .setNegativeButton("Удалить") { _, _ ->
                dbHelper.deletePokemon(pokemon.id)
                Toast.makeText(this, "Запись удалена", Toast.LENGTH_SHORT).show()
                loadList()
            }
            .setNeutralButton("Закрыть", null)
            .show()
    }

    private fun showEditDialog(pokemon: SavedPokemon?) {
        val view = layoutInflater.inflate(R.layout.dialog_pokemon, null)
        val etName = view.findViewById<EditText>(R.id.etName)
        val etFact = view.findViewById<EditText>(R.id.etFact)
        val etType = view.findViewById<EditText>(R.id.etType)

        pokemon?.let {
            etName.setText(it.name)
            etFact.setText(it.fact)
            etType.setText(it.type)
        }

        AlertDialog.Builder(this)
            .setTitle(if (pokemon == null) "Добавить персонажа" else "Редактировать запись")
            .setView(view)
            .setPositiveButton("Сохранить") { _, _ ->
                val name = etName.text.toString().trim()
                val fact = etFact.text.toString().trim()
                val type = etType.text.toString().trim()

                if (name.isEmpty() || fact.isEmpty()) {
                    Toast.makeText(this, "Заполните все поля!", Toast.LENGTH_SHORT).show()
                } else {
                    val newPokemon = SavedPokemon(
                        id = pokemon?.id ?: System.currentTimeMillis().toInt(),
                        name = name,
                        imageUrl = pokemon?.imageUrl,
                        fact = fact,
                        type = type
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
