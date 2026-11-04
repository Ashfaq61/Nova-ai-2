package com.nova.ai.memory

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject

class MemoryDb(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "nova_memory.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_FTS = "memory_fts"

        @Volatile
        private var instance: MemoryDb? = null

        fun getInstance(context: Context): MemoryDb {
            return instance ?: synchronized(this) {
                instance ?: MemoryDb(context.applicationContext).also { instance = it }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Create FTS4 virtual table for full-text semantic and keyword memory search
        db.execSQL(
            "CREATE VIRTUAL TABLE $TABLE_FTS USING fts4(content, category, timestamp, tags)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_FTS")
        onCreate(db)
    }

    fun remember(content: String, category: String = "general", tags: String = ""): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("content", content.trim())
            put("category", category)
            put("timestamp", System.currentTimeMillis().toString())
            put("tags", tags)
        }
        return db.insert(TABLE_FTS, null, values)
    }

    fun recall(query: String, limit: Int = 10): List<String> {
        val results = mutableListOf<String>()
        val db = readableDatabase
        val sanitizedQuery = query.replace("'", "''").trim()
        if (sanitizedQuery.isBlank()) return results

        val cursor = db.rawQuery(
            "SELECT content, category, timestamp FROM $TABLE_FTS WHERE $TABLE_FTS MATCH ? ORDER BY docid DESC LIMIT ?",
            arrayOf("$sanitizedQuery*", limit.toString())
        )

        cursor.use {
            val contentIdx = it.getColumnIndex("content")
            while (it.moveToNext()) {
                if (contentIdx != -1) {
                    results.add(it.getString(contentIdx))
                }
            }
        }
        return results
    }

    fun recallJson(query: String, limit: Int = 10): String {
        val list = recall(query, limit)
        val arr = JSONArray()
        list.forEach { arr.put(it) }
        return arr.toString()
    }

    fun getAllMemories(limit: Int = 50): List<String> {
        val results = mutableListOf<String>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT content FROM $TABLE_FTS ORDER BY docid DESC LIMIT ?", arrayOf(limit.toString()))
        cursor.use {
            val contentIdx = it.getColumnIndex("content")
            while (it.moveToNext()) {
                if (contentIdx != -1) {
                    results.add(it.getString(contentIdx))
                }
            }
        }
        return results
    }

    fun clearAll() {
        writableDatabase.execSQL("DELETE FROM $TABLE_FTS")
    }
}
