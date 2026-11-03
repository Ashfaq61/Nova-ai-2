package com.example.memory

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject

class KnowledgeGraph(context: Context) : SQLiteOpenHelper(context, "nova_memory.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE facts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                entity TEXT,
                relation TEXT,
                target TEXT,
                timestamp INTEGER,
                confidence REAL
            )
        """.trimIndent())

        // Bootstrap initial memory facts
        val now = System.currentTimeMillis()
        db.execSQL("INSERT INTO facts (entity, relation, target, timestamp, confidence) VALUES ('User', 'prefers', 'Direct concise answers', $now, 0.95)")
        db.execSQL("INSERT INTO facts (entity, relation, target, timestamp, confidence) VALUES ('User', 'works_on', 'Android Architecture & AI Systems', $now, 0.98)")
        db.execSQL("INSERT INTO facts (entity, relation, target, timestamp, confidence) VALUES ('Keys', 'last_seen', 'Living Room Table', $now, 0.94)")
        db.execSQL("INSERT INTO facts (entity, relation, target, timestamp, confidence) VALUES ('Routine', 'favorite', 'Deep Work Focus Mode', $now, 0.90)")
        db.execSQL("INSERT INTO facts (entity, relation, target, timestamp, confidence) VALUES ('Assistant', 'identity', 'Nova AI Omniscient Agent', $now, 1.0)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS facts")
        onCreate(db)
    }

    fun saveFact(entity: String, relation: String, target: String, confidence: Float = 0.9f): Long {
        val cv = ContentValues().apply {
            put("entity", entity)
            put("relation", relation)
            put("target", target)
            put("timestamp", System.currentTimeMillis())
            put("confidence", confidence)
        }
        return writableDatabase.insert("facts", null, cv)
    }

    fun queryRecentFacts(limit: Int = 5): String {
        val list = JSONArray()
        val cursor = readableDatabase.rawQuery("SELECT entity, relation, target, timestamp, confidence FROM facts ORDER BY id DESC LIMIT ?", arrayOf(limit.toString()))
        cursor.use {
            while (it.moveToNext()) {
                list.put(JSONObject().apply {
                    put("entity", it.getString(0))
                    put("relation", it.getString(1))
                    put("target", it.getString(2))
                    put("timestamp", it.getLong(3))
                    put("confidence", it.getDouble(4))
                })
            }
        }
        return list.toString()
    }
}
