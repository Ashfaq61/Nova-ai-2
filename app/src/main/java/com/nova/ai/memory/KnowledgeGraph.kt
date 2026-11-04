package com.nova.ai.memory

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject

class KnowledgeGraph(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        private const val DB_NAME = "nova_knowledge_graph.db"
        private const val DB_VERSION = 1

        @Volatile
        private var instance: KnowledgeGraph? = null

        fun getInstance(context: Context): KnowledgeGraph {
            return instance ?: synchronized(this) {
                instance ?: KnowledgeGraph(context.applicationContext).also { instance = it }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Entity Table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS entities (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT UNIQUE NOT NULL,
                type TEXT,
                attributes TEXT,
                created_at INTEGER
            )
            """.trimIndent()
        )

        // Relation Table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS relations (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                source_entity TEXT NOT NULL,
                relation TEXT NOT NULL,
                target_entity TEXT NOT NULL,
                confidence REAL DEFAULT 1.0,
                created_at INTEGER
            )
            """.trimIndent()
        )

        // Event Table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS events (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                description TEXT NOT NULL,
                timestamp INTEGER,
                location TEXT,
                associated_entity TEXT
            )
            """.trimIndent()
        )

        // Seed core knowledge
        seedInitialKnowledge(db)
    }

    private fun seedInitialKnowledge(db: SQLiteDatabase) {
        val now = System.currentTimeMillis()
        db.execSQL("INSERT OR IGNORE INTO entities(name, type, attributes, created_at) VALUES ('User', 'Person', '{\"role\":\"Owner\"}', $now)")
        db.execSQL("INSERT OR IGNORE INTO entities(name, type, attributes, created_at) VALUES ('Nova', 'AI', '{\"version\":\"Phase 10 Singularity\"}', $now)")
        db.execSQL("INSERT OR IGNORE INTO relations(source_entity, relation, target_entity, confidence, created_at) VALUES ('Nova', 'assists', 'User', 1.0, $now)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS entities")
        db.execSQL("DROP TABLE IF EXISTS relations")
        db.execSQL("DROP TABLE IF EXISTS events")
        onCreate(db)
    }

    fun addEntity(name: String, type: String, attributes: String = "{}"): Long {
        val values = ContentValues().apply {
            put("name", name)
            put("type", type)
            put("attributes", attributes)
            put("created_at", System.currentTimeMillis())
        }
        return writableDatabase.insertWithOnConflict("entities", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun addRelation(source: String, relation: String, target: String, confidence: Float = 1.0f): Long {
        val values = ContentValues().apply {
            put("source_entity", source)
            put("relation", relation)
            put("target_entity", target)
            put("confidence", confidence)
            put("created_at", System.currentTimeMillis())
        }
        return writableDatabase.insert("relations", null, values)
    }

    fun addEvent(description: String, location: String = "", entity: String = ""): Long {
        val values = ContentValues().apply {
            put("description", description)
            put("timestamp", System.currentTimeMillis())
            put("location", location)
            put("associated_entity", entity)
        }
        return writableDatabase.insert("events", null, values)
    }

    fun queryReadOnlySql(sql: String): String {
        val sanitized = sql.trim()
        val lower = sanitized.lowercase()

        // Strict read-only guard: prevent data modification
        if (!lower.startsWith("select")) {
            return JSONObject().put("error", "Only SELECT read-only queries are permitted").toString()
        }
        if (lower.contains("drop ") || lower.contains("delete ") || lower.contains("update ") || lower.contains("insert ") || lower.contains("alter ")) {
            return JSONObject().put("error", "Forbidden query token detected").toString()
        }

        return try {
            val db = readableDatabase
            val cursor = db.rawQuery(sanitized, null)
            val jsonArray = JSONArray()
            cursor.use { c ->
                val columns = c.columnNames
                while (c.moveToNext()) {
                    val obj = JSONObject()
                    for (col in columns) {
                        val idx = c.getColumnIndex(col)
                        obj.put(col, c.getString(idx) ?: "")
                    }
                    jsonArray.put(obj)
                }
            }
            jsonArray.toString()
        } catch (e: Exception) {
            JSONObject().put("error", e.message ?: "Query failed").toString()
        }
    }

    fun answerNaturalLanguage(nlQuestion: String): String {
        val lower = nlQuestion.lowercase()
        return when {
            lower.contains("who is") || lower.contains("what is") -> {
                val entityName = nlQuestion.substringAfter("is ").substringBefore("?").trim()
                val res = queryReadOnlySql("SELECT name, type, attributes FROM entities WHERE name LIKE '%$entityName%' LIMIT 1")
                if (res != "[]") res else "I don't have records for $entityName yet."
            }
            lower.contains("relation") || lower.contains("connected") -> {
                queryReadOnlySql("SELECT source_entity, relation, target_entity FROM relations LIMIT 10")
            }
            lower.contains("events") || lower.contains("what happened") -> {
                queryReadOnlySql("SELECT description, location, timestamp FROM events ORDER BY timestamp DESC LIMIT 5")
            }
            else -> {
                queryReadOnlySql("SELECT name, type FROM entities LIMIT 5")
            }
        }
    }

    fun pruneOldEvents(daysRetention: Int = 30): Int {
        val cutoff = System.currentTimeMillis() - (daysRetention.toLong() * 24 * 60 * 60 * 1000)
        return writableDatabase.delete("events", "timestamp < ?", arrayOf(cutoff.toString()))
    }
}
