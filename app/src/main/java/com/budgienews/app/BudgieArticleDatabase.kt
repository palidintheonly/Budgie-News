package com.budgienews.app

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal data class LocalArticle(
    val articleId: String,
    val title: String,
    val description: String,
    val link: String,
    val source: String,
    val publishedAt: String,
    val imageUrl: String?,
    val category: String,
    val isRead: Boolean,
)

internal object ArticleSignals {
    private val _version = MutableStateFlow(0L)
    val version = _version.asStateFlow()
    private val _openArticleId = MutableStateFlow<String?>(null)
    val openArticleId = _openArticleId.asStateFlow()

    fun changed() {
        _version.value = _version.value + 1
    }

    fun open(articleId: String) {
        _openArticleId.value = articleId
    }

    fun clearOpenRequest(articleId: String) {
        if (_openArticleId.value == articleId) {
            _openArticleId.value = null
        }
    }
}

internal object BudgieTime {
    val RESET_EPOCH_MILLIS = java.time.ZonedDateTime.parse("2026-07-04T00:00:00Z").toInstant().toEpochMilli()

    fun minAllowedMillis(): Long {
        val sevenDaysAgo = System.currentTimeMillis() - 604_800_000L
        return maxOf(sevenDaysAgo, RESET_EPOCH_MILLIS)
    }
}

internal class BudgieArticleDatabase private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, "budgie_articles.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS articles (
                article_id TEXT NOT NULL PRIMARY KEY,
                title TEXT NOT NULL,
                description TEXT NOT NULL,
                link TEXT NOT NULL,
                source TEXT NOT NULL,
                published_at TEXT NOT NULL,
                image_url TEXT,
                category TEXT NOT NULL,
                is_read INTEGER NOT NULL DEFAULT 0,
                received_at INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_articles_category_received ON articles(category, received_at DESC)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_articles_received ON articles(received_at DESC)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onCreate(db)
    }

    fun upsertArticle(article: LocalArticle) {
        val oldestAllowed = BudgieTime.minAllowedMillis()
        synchronized(this) {
            writableDatabase.beginTransaction()
            try {
                val values = ContentValues().apply {
                    put("article_id", article.articleId)
                    put("title", article.title)
                    put("description", article.description)
                    put("link", article.link)
                    put("source", article.source)
                    put("published_at", article.publishedAt)
                    put("image_url", article.imageUrl)
                    put("category", article.category)
                    put("is_read", if (article.isRead) 1 else 0)
                    put("received_at", System.currentTimeMillis())
                }
                writableDatabase.insertWithOnConflict("articles", null, values, SQLiteDatabase.CONFLICT_REPLACE)
                writableDatabase.delete("articles", "received_at < ?", arrayOf(oldestAllowed.toString()))
                writableDatabase.setTransactionSuccessful()
            } finally {
                writableDatabase.endTransaction()
            }
        }
        ArticleSignals.changed()
    }

    fun markRead(articleId: String) {
        synchronized(this) {
            val values = ContentValues().apply { put("is_read", 1) }
            writableDatabase.update("articles", values, "article_id = ?", arrayOf(articleId))
        }
        ArticleSignals.changed()
    }

    fun recentArticles(limit: Int = 40): List<LocalArticle> {
        val newestAllowed = BudgieTime.minAllowedMillis()
        synchronized(this) {
            writableDatabase.delete("articles", "received_at < ?", arrayOf(newestAllowed.toString()))
        }
        val rows = readableDatabase.query(
            "articles",
            arrayOf("article_id", "title", "description", "link", "source", "published_at", "image_url", "category", "is_read"),
            "received_at >= ?",
            arrayOf(newestAllowed.toString()),
            null,
            null,
            "received_at DESC",
            limit.toString(),
        )
        rows.use { cursor ->
            val result = mutableListOf<LocalArticle>()
            while (cursor.moveToNext()) {
                result += LocalArticle(
                    articleId = cursor.getString(0),
                    title = cursor.getString(1),
                    description = cursor.getString(2),
                    link = cursor.getString(3),
                    source = cursor.getString(4),
                    publishedAt = cursor.getString(5),
                    imageUrl = cursor.getString(6),
                    category = cursor.getString(7),
                    isRead = cursor.getInt(8) == 1,
                )
            }
            return result
        }
    }

    fun articleById(articleId: String): LocalArticle? {
        val rows = readableDatabase.query(
            "articles",
            arrayOf("article_id", "title", "description", "link", "source", "published_at", "image_url", "category", "is_read"),
            "article_id = ?",
            arrayOf(articleId),
            null,
            null,
            null,
            "1",
        )
        rows.use { cursor ->
            if (!cursor.moveToFirst()) return null
            return LocalArticle(
                articleId = cursor.getString(0),
                title = cursor.getString(1),
                description = cursor.getString(2),
                link = cursor.getString(3),
                source = cursor.getString(4),
                publishedAt = cursor.getString(5),
                imageUrl = cursor.getString(6),
                category = cursor.getString(7),
                isRead = cursor.getInt(8) == 1,
            )
        }
    }

    companion object {
        @Volatile
        private var instance: BudgieArticleDatabase? = null

        fun get(context: Context): BudgieArticleDatabase =
            instance ?: synchronized(this) {
                instance ?: BudgieArticleDatabase(context).also { instance = it }
            }
    }
}
