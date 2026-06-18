package com.example.uami.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.widget.RemoteViews
import com.example.uami.MainActivity
import com.example.uami.R
import com.example.uami.database.RecipeEntity
import com.example.uami.database.UamiDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar

/**
 * Widget de recomendación de comidas de Uami.
 * Muestra recetas reales desde Room con imagen descargada desde la URL.
 * Al hacer click navega directamente al detalle de esa receta.
 */
class MealRecommendationWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_REFRESH = "com.example.uami.widget.ACTION_REFRESH"
        const val EXTRA_RECIPE_ID = "widget_recipe_id"

        fun getMealTypeKeywords(): List<String> {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            return when (hour) {
                in 5..10  -> listOf("breakfast", "brunch", "morning")
                in 11..14 -> listOf("lunch", "brunch", "salad", "main course", "snack")
                in 15..21 -> listOf("dinner", "main course", "main dish", "supper")
                else      -> listOf("snack", "dessert", "appetizer", "breakfast")
            }
        }

        fun getMealLabel(): String {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            return when (hour) {
                in 5..10  -> "Desayuno 🌅"
                in 11..14 -> "Almuerzo ☀️"
                in 15..21 -> "Cena 🌙"
                else      -> "Snack ⭐"
            }
        }

        private fun getTimeLabel(): String {
            val cal  = Calendar.getInstance()
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val min  = cal.get(Calendar.MINUTE)
            val ampm = if (hour < 12) "AM" else "PM"
            val h12  = when {
                hour == 0 -> 12
                hour > 12 -> hour - 12
                else      -> hour
            }
            return "$h12:${min.toString().padStart(2, '0')} $ampm"
        }

        private fun getHeaderEmoji(): String {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            return when (hour) {
                in 5..10  -> "🌅"
                in 11..14 -> "☀️"
                in 15..21 -> "🌙"
                else      -> "⭐"
            }
        }

        private fun buildDesc(recipe: RecipeEntity): String {
            val parts = mutableListOf<String>()
            val cuisine = recipe.cuisine?.takeIf { it.isNotBlank() }
            val diff    = recipe.difficulty?.takeIf { it.isNotBlank() }
            val time    = (recipe.prepTimeMinutes ?: 0) + (recipe.cookTimeMinutes ?: 0)
            if (cuisine != null) parts.add(cuisine)
            if (diff != null)    parts.add(diff)
            if (time > 0)        parts.add("$time min")
            return if (parts.isNotEmpty()) parts.joinToString(" · ") else "Receta de Uami"
        }

        /**
         * Descarga la imagen desde la URL o la carga desde un archivo local,
         * y la devuelve como Bitmap circular.
         * Usa un tamaño de muestreo para no cargar imágenes enormes en memoria.
         */
        private fun downloadBitmap(imageUrl: String?, targetPx: Int = 168): Bitmap? {
            if (imageUrl.isNullOrBlank()) return null
            return try {
                val raw: Bitmap? = if (imageUrl.startsWith("file://")) {
                    val filePath = imageUrl.substring(7)
                    val file = java.io.File(filePath)
                    if (!file.exists()) return null

                    // Primero decodificar solo las dimensiones para calcular inSampleSize
                    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(filePath, opts)
                    val sampleSize = calculateInSampleSize(opts, targetPx, targetPx)

                    // Decodificar con subsampling
                    val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                    BitmapFactory.decodeFile(filePath, decodeOpts)
                } else {
                    val url = URL(imageUrl)
                    val connection = (url.openConnection() as HttpURLConnection).apply {
                        doInput = true
                        connectTimeout = 6000
                        readTimeout    = 8000
                        connect()
                    }
                    if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                        connection.disconnect()
                        return null
                    }
                    val bytes = connection.inputStream.use { it.readBytes() }
                    connection.disconnect()

                    // Primero decodificar solo las dimensiones para calcular inSampleSize
                    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
                    val sampleSize = calculateInSampleSize(opts, targetPx, targetPx)

                    // Decodificar con subsampling
                    val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts)
                }

                if (raw == null) return null
                toCircleBitmap(raw, targetPx)
            } catch (e: Exception) {
                null
            }
        }

        private fun calculateInSampleSize(
            options: BitmapFactory.Options,
            reqWidth: Int,
            reqHeight: Int
        ): Int {
            val (height, width) = options.outHeight to options.outWidth
            var inSampleSize = 1
            if (height > reqHeight || width > reqWidth) {
                val halfHeight = height / 2
                val halfWidth  = width  / 2
                while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                    inSampleSize *= 2
                }
            }
            return inSampleSize
        }

        /** Recorta y escala el Bitmap en forma circular al tamaño deseado */
        private fun toCircleBitmap(src: Bitmap, size: Int): Bitmap {
            val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val paint  = Paint(Paint.ANTI_ALIAS_FLAG)
            val rectF  = RectF(0f, 0f, size.toFloat(), size.toFloat())

            canvas.drawOval(rectF, paint)
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)

            val scaled = Bitmap.createScaledBitmap(src, size, size, true)
            canvas.drawBitmap(scaled, Rect(0, 0, scaled.width, scaled.height), rectF, paint)
            if (scaled != src) scaled.recycle()

            return output
        }

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                val db         = UamiDatabase.getDatabase(context)
                val allRecipes = db.recipeDao().getAllRecipes()

                val keywords  = getMealTypeKeywords()
                val filtered  = allRecipes.filter { recipe ->
                    val typeEn = recipe.mealTypeEn?.lowercase() ?: recipe.mealType?.lowercase() ?: ""
                    keywords.any { kw -> kw in typeEn }
                }
                val candidates = if (filtered.isNotEmpty()) filtered else allRecipes
                val recipe: RecipeEntity? = if (candidates.isNotEmpty()) candidates.random() else null

                // Descargar imagen real de la receta
                val bitmap: Bitmap? = recipe?.let { downloadBitmap(it.image) }

                withContext(Dispatchers.Main) {
                    val views = RemoteViews(context.packageName, R.layout.widget_meal_recommendation)

                    if (recipe != null) {
                        val desc   = buildDesc(recipe)
                        val rating = recipe.rating?.let { "⭐ ${"%.1f".format(it)}" } ?: ""

                        views.setTextViewText(R.id.widget_meal_emoji,  getHeaderEmoji())
                        views.setTextViewText(R.id.widget_meal_type,   getMealLabel())
                        views.setTextViewText(R.id.widget_time_label,  getTimeLabel())
                        views.setTextViewText(R.id.widget_dish_name,   recipe.name ?: "Sin nombre")
                        views.setTextViewText(R.id.widget_dish_desc,   desc)
                        views.setTextViewText(R.id.widget_dish_rating, rating)

                        // Imagen real de la receta (si se descargó) o ícono de la app
                        if (bitmap != null) {
                            views.setImageViewBitmap(R.id.widget_dish_image, bitmap)
                        } else {
                            views.setImageViewResource(R.id.widget_dish_image, R.mipmap.ic_launcher)
                        }

                        // ── Click en el widget → abrir detalle de ESTA receta ──
                        val detailIntent = Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            putExtra(EXTRA_RECIPE_ID, recipe.id)
                        }
                        val detailPending = PendingIntent.getActivity(
                            context, appWidgetId * 1000 + recipe.id, detailIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        views.setOnClickPendingIntent(R.id.widget_root, detailPending)

                    } else {
                        // DB vacía — abrir la app normalmente
                        views.setTextViewText(R.id.widget_meal_emoji,  "🍽️")
                        views.setTextViewText(R.id.widget_meal_type,   getMealLabel())
                        views.setTextViewText(R.id.widget_time_label,  getTimeLabel())
                        views.setTextViewText(R.id.widget_dish_name,   "Abre Uami primero")
                        views.setTextViewText(R.id.widget_dish_desc,   "Para cargar tus recetas")
                        views.setTextViewText(R.id.widget_dish_rating, "")
                        views.setImageViewResource(R.id.widget_dish_image, R.mipmap.ic_launcher)

                        val launchIntent = Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        val launchPending = PendingIntent.getActivity(
                            context, appWidgetId, launchIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        views.setOnClickPendingIntent(R.id.widget_root, launchPending)
                    }

                    // Botón ↻ — refrescar con otra receta
                    val refreshIntent = Intent(context, MealRecommendationWidget::class.java).apply {
                        action = ACTION_REFRESH
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    }
                    val refreshPending = PendingIntent.getBroadcast(
                        context, appWidgetId, refreshIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_refresh_btn, refreshPending)

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (id in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, id)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            val widgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
            if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                val manager = AppWidgetManager.getInstance(context)
                updateAppWidget(context, manager, widgetId)
            }
        }
    }

    override fun onEnabled(context: Context) {}
    override fun onDisabled(context: Context) {}
}
