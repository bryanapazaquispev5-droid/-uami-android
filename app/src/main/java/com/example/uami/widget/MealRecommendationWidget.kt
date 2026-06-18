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
        const val EXTRA_GO_TO_COOKING = "widget_go_to_cooking"

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
            appWidgetId: Int,
            newOptions: android.os.Bundle? = null,
            forceRefresh: Boolean = false
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                val db         = UamiDatabase.getDatabase(context)
                val allRecipes = db.recipeDao().getAllRecipes()

                // Obtener opciones del widget para determinar el tamaño en dp
                val options = newOptions ?: appWidgetManager.getAppWidgetOptions(appWidgetId)
                val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 180)
                val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 50)

                // Determinar layout según las dimensiones (1x3, 2x3 o 2x4)
                val layoutId = if (minHeight > 0 && minHeight < 100) {
                    // 1x3 (1 fila de alto, 3-4 columnas de ancho)
                    R.layout.widget_meal_recommendation_1x3
                } else {
                    // 2 filas de alto
                    if (minWidth >= 240) {
                        // 2x4 (2 filas de alto, 4 columnas de ancho)
                        R.layout.widget_meal_recommendation_2x4
                    } else {
                        // 2x3 (2 filas de alto, 3 columnas de ancho)
                        R.layout.widget_meal_recommendation_2x3
                    }
                }

                android.util.Log.d("UAMI_WIDGET", "updateAppWidget id=$appWidgetId size=${minWidth}x${minHeight}dp -> layoutId=$layoutId")

                val currentMealLabel = getMealLabel()
                val prefs = context.getSharedPreferences("uami_widget_prefs", Context.MODE_PRIVATE)
                val savedRecipeId = prefs.getInt("widget_${appWidgetId}_recipe_id", -1)
                val savedMealType = prefs.getString("widget_${appWidgetId}_meal_type", "")

                val recipe: RecipeEntity?
                val keywords = getMealTypeKeywords()

                if (!forceRefresh && savedRecipeId != -1 && savedMealType == currentMealLabel) {
                    // Cargar la receta que ya se estaba mostrando
                    val savedRecipe = allRecipes.find { it.id == savedRecipeId }
                    recipe = savedRecipe ?: if (allRecipes.isNotEmpty()) {
                        val filtered = allRecipes.filter { r ->
                            val typeEn = r.mealTypeEn?.lowercase() ?: r.mealType?.lowercase() ?: ""
                            keywords.any { kw -> kw in typeEn }
                        }
                        val cand = if (filtered.isNotEmpty()) filtered else allRecipes
                        val chosen = cand.random()
                        prefs.edit()
                            .putInt("widget_${appWidgetId}_recipe_id", chosen.id)
                            .putString("widget_${appWidgetId}_meal_type", currentMealLabel)
                            .apply()
                        chosen
                    } else null
                } else {
                    // Elegir una nueva receta (porque cambió de horario, es la primera vez, o se presionó refresh)
                    if (allRecipes.isNotEmpty()) {
                        val filtered = allRecipes.filter { r ->
                            val typeEn = r.mealTypeEn?.lowercase() ?: r.mealType?.lowercase() ?: ""
                            keywords.any { kw -> kw in typeEn }
                        }
                        val cand = if (filtered.isNotEmpty()) filtered else allRecipes
                        val chosen = cand.random()
                        prefs.edit()
                            .putInt("widget_${appWidgetId}_recipe_id", chosen.id)
                            .putString("widget_${appWidgetId}_meal_type", currentMealLabel)
                            .apply()
                        recipe = chosen
                    } else {
                        recipe = null
                    }
                }

                // Descargar o cargar imagen real de la receta
                val targetPx = if (layoutId == R.layout.widget_meal_recommendation_2x4) 240 else 168
                val bitmap: Bitmap? = recipe?.let { downloadBitmap(it.image, targetPx) }

                withContext(Dispatchers.Main) {
                    val views = RemoteViews(context.packageName, layoutId)

                    // ── Paleta de colores según el momento del día ──
                    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                    val headerBg: Int
                    val cookingBg: Int
                    val refreshBg: Int
                    val accentColorStr: String

                    if (hour in 5..10) {
                        headerBg = R.drawable.widget_header_bg_desayuno
                        cookingBg = R.drawable.widget_btn_cooking_desayuno
                        refreshBg = R.drawable.widget_btn_refresh_desayuno
                        accentColorStr = "#FFF57C00"
                    } else if (hour in 11..14) {
                        headerBg = R.drawable.widget_header_bg_almuerzo
                        cookingBg = R.drawable.widget_btn_cooking_almuerzo
                        refreshBg = R.drawable.widget_btn_refresh_almuerzo
                        accentColorStr = "#FF00796B"
                    } else if (hour in 15..21) {
                        headerBg = R.drawable.widget_header_bg_cena
                        cookingBg = R.drawable.widget_btn_cooking_cena
                        refreshBg = R.drawable.widget_btn_refresh_cena
                        accentColorStr = "#FF3F51B5"
                    } else {
                        headerBg = R.drawable.widget_header_bg_snack
                        cookingBg = R.drawable.widget_btn_cooking_snack
                        refreshBg = R.drawable.widget_btn_refresh_snack
                        accentColorStr = "#FF8E24AA"
                    }

                    // Aplicar fondos dinámicos y colores de acento generales
                    views.setInt(R.id.widget_header, "setBackgroundResource", headerBg)
                    views.setInt(R.id.widget_refresh_btn, "setBackgroundResource", refreshBg)

                    if (layoutId == R.layout.widget_meal_recommendation_2x4) {
                        views.setInt(R.id.widget_cooking_btn, "setBackgroundResource", cookingBg)
                        views.setTextColor(R.id.widget_ingredients_title, android.graphics.Color.parseColor(accentColorStr))
                    } else if (layoutId == R.layout.widget_meal_recommendation_1x3) {
                        views.setTextColor(R.id.widget_meal_type, android.graphics.Color.parseColor(accentColorStr))
                    }

                    if (recipe != null) {
                        val desc   = buildDesc(recipe)
                        val rating = recipe.rating?.let { "⭐ ${"%.1f".format(it)}" } ?: ""

                        views.setTextViewText(R.id.widget_meal_emoji,  getHeaderEmoji())
                        views.setTextViewText(R.id.widget_meal_type,   getMealLabel())
                        views.setTextViewText(R.id.widget_time_label,  getTimeLabel())
                        views.setTextViewText(R.id.widget_dish_name,   recipe.name ?: "Sin nombre")
                        views.setTextViewText(R.id.widget_dish_desc,   desc)
                        views.setTextViewText(R.id.widget_dish_rating, rating)

                        // Configurar información extra (Dificultad + tiempo)
                        val totalTime = (recipe.prepTimeMinutes ?: 0) + (recipe.cookTimeMinutes ?: 0)
                        val difficultyTrans = when (recipe.difficulty?.lowercase()) {
                            "easy" -> "Fácil"
                            "medium" -> "Media"
                            "hard" -> "Difícil"
                            else -> recipe.difficulty ?: ""
                        }
                        val extraInfoText = when {
                            difficultyTrans.isNotBlank() && totalTime > 0 -> "$difficultyTrans · $totalTime min"
                            difficultyTrans.isNotBlank() -> difficultyTrans
                            totalTime > 0 -> "$totalTime min"
                            else -> ""
                        }
                        views.setTextViewText(R.id.widget_extra_info, extraInfoText)

                        // Configurar ingredientes si es el layout de 2x4
                        val ingredientsList = recipe.ingredients
                        if (layoutId == R.layout.widget_meal_recommendation_2x4 && ingredientsList != null && ingredientsList.isNotEmpty()) {
                            val ingredientsText = ingredientsList.take(5).joinToString(", ")
                            views.setTextViewText(R.id.widget_ingredients_list, ingredientsText)
                            views.setViewVisibility(R.id.widget_ingredients_container, android.view.View.VISIBLE)
                        } else {
                            views.setViewVisibility(R.id.widget_ingredients_container, android.view.View.GONE)
                        }

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

                        // ── Click en el botón Cocinar → abrir modo cocina de ESTA receta ──
                        val cookingIntent = Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            putExtra(EXTRA_RECIPE_ID, recipe.id)
                            putExtra(EXTRA_GO_TO_COOKING, true)
                        }
                        val cookingPending = PendingIntent.getActivity(
                            context, appWidgetId * 1000 + recipe.id + 50000, cookingIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        views.setOnClickPendingIntent(R.id.widget_cooking_btn, cookingPending)

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

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateAppWidget(context, appWidgetManager, appWidgetId, newOptions)
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
                updateAppWidget(context, manager, widgetId, forceRefresh = true)
            }
        } else if (intent.action == Intent.ACTION_TIME_CHANGED ||
                   intent.action == Intent.ACTION_TIMEZONE_CHANGED) {
            val manager = AppWidgetManager.getInstance(context)
            val component = android.content.ComponentName(context, MealRecommendationWidget::class.java)
            val ids = manager.getAppWidgetIds(component)
            for (id in ids) {
                updateAppWidget(context, manager, id, forceRefresh = false)
            }
        }
    }

    override fun onEnabled(context: Context) {}
    override fun onDisabled(context: Context) {}
}
