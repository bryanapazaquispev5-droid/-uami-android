# <img src="uamilogo.png" align="right" width="90px"/> Uami - Asistente Gourmet e IA de Cocina Offline

**Uami** es una aplicación móvil nativa para Android diseñada para simplificar y enriquecer la experiencia de cocinar en el hogar. Combina un catálogo gourmet inteligente con un asistente de nutrición basado en modelos de lenguaje avanzados que se ejecutan completamente en el dispositivo de forma segura y privada.

---

## 🚀 Características Principales

*   **Modo Cocina Interactivo:** Preparación guiada paso a paso con una lista de ingredientes previa e interactiva al 100% que avanza automáticamente. Transición visual tipo "hoja de papel despegada de la pared" al pasar de página.
    <p><img src="screenshots/recipe_detail_premium_ui.jpeg" width="30%" /> <img src="screenshots/cooking_mode_ingredient_checklist.jpeg" width="30%" /> <img src="screenshots/cooking_mode_progress_and_tts.jpeg" width="30%" /></p>
*   **Asistente de Voz (TTS):** Lectura por síntesis de voz del paso actual para cocinar con manos libres (botón gigante de audio en el Modo Cocina).

    https://github.com/bryanapazaquispev5-droid/-uami-android/raw/master/screenshots/cooking_mode_voice_assistant.mp4
*   **Nutriólogo AI Local:** Un planificador de dietas y menús de 7 días impulsado por **Gemma-2B** corriendo localmente en el procesador del dispositivo, garantizando privacidad total y funcionamiento sin internet. Incluye un **chatbot conversacional** que responde preguntas sobre recetas, ingredientes y temas de alimentación directamente desde el modelo local (con filtrado inteligente de preguntas fuera de contexto culinario).
    <p><img src="screenshots/ai_diet_planner_empty.jpeg" width="30%" /> <img src="screenshots/ai_diet_planner_generating.jpeg" width="30%" /> <img src="screenshots/ai_diet_planner_weekly_menu.jpeg" width="30%" /></p>
*   **Traducción Inteligente On-Device:** Traducción en tiempo real de recetas del inglés al español usando **Google ML Kit**, complementada con un diccionario local de ~150 términos culinarios (ingredientes, técnicas, medidas) como fallback.
    <p><img src="screenshots/smart_translation_es.jpeg" width="30%" /> <img src="screenshots/smart_translation_en.jpeg" width="30%" /></p>
*   **Soporte Bilingüe (Español / Inglés):** Pantalla de selección de idioma en el primer inicio con persistencia de preferencia. La app adapta la interfaz y traducciones según el idioma seleccionado.
*   **Explorador de Supermercados (Google Maps):** Mapa interactivo de supermercados y mercados tradicionales en Arequipa con estilo oscuro personalizado para buscar los ingredientes, y enlace a navegación con Google Maps.
    <p><img src="screenshots/supermarkets_map_explorer.jpeg" width="30%" /></p>
*   **Notificaciones Push Inteligentes (FCM):** Canal de notificaciones remotas mediante Firebase Cloud Messaging para alertas de recetas del día, sugerencias personalizadas de nutrición y tips de salud interactivos.
    <p><img src="screenshots/firebase_fcm_console.png" width="45%" /> <img src="screenshots/push_notification_alert.jpeg" width="30%" /></p>
*   **Opiniones de la Comunidad y Reacciones (Firestore):** Módulo para publicar reseñas con calificación por estrellas y comentarios en tiempo real. Cuenta con un dashboard de calificación animado, un halo sweep-gradient rotatorio para destacar avatares con calificaciones de 5 estrellas, y soporte de reacciones ("Me Gusta") en tiempo real con físicas de rebote elástico.
    <p><img src="screenshots/community_reviews_empty.jpeg" width="30%" /></p>
*   **Base de Datos Local (Room):** Persistencia local de recetas y traducción de textos mediante Room Database. Implementa transacciones de base de datos seguras, flujos de datos reactivos con Flow y corrutinas. Incluye **vista dual** (Lista / Cuadrícula), **búsqueda por texto**, filtros por cocina/dificultad/tipo de comida, y **6 modos de ordenamiento** (A-Z, Z-A, Rating, Tiempo, Ingredientes).
    <p><img src="screenshots/recipes_list_view.jpeg" width="30%" /> <img src="screenshots/recipes_grid_view.jpeg" width="30%" /> <img src="screenshots/recipes_filter_bottom_sheet.jpeg" width="30%" /></p>
*   **Sincronización en la Nube de Favoritos**: Sistema híbrido (local y remoto) que sincroniza en tiempo real los favoritos del chef con Firebase Firestore. Si el usuario inicia sesión por primera vez, realiza una fusión inteligente (unión) de sus favoritos de invitado con la nube para evitar pérdida de datos.
    <p><img src="screenshots/favorites_cloud_sync.jpeg" width="30%" /></p>
*   **Avatares Personalizados y Perfil de Chef**: Registro e inicio de sesión integrados donde el chef puede elegir un avatar preestablecido o subir su propia foto de perfil de galería (comprimida y convertida a Base64 en memoria de forma optimizada para caber en Firestore), actualizando su identidad al instante en todas las pantallas.
    <p><img src="screenshots/auth_login_screen.jpeg" width="30%" /> <img src="screenshots/auth_register_chef_screen.jpeg" width="30%" /> <img src="screenshots/profile_edit_avatar.jpeg" width="30%" /></p>
*   **Widget de Recomendación Adaptativo (App Widgets):** Widget interactivo de pantalla de inicio en formatos **1x3**, **2x3** y **2x4** que sugiere recetas basadas en la hora del día. Ofrece refresco manual (`↻`), acceso directo al modo cocina (en el formato 2x4), y una paleta de colores degradados dinámica que cambia según el momento del día (Desayuno, Almuerzo, Cena, Snack).
    <p>
      <img src="screenshots/widget_recommendation_lunch_large.jpeg" width="24%" /> 
      <img src="screenshots/widget_recommendation_breakfast_large.jpeg" width="24%" /> 
      <img src="screenshots/widget_recommendation_snack_large.jpeg" width="24%" /> 
      <img src="screenshots/widget_recommendation_dinner_large.jpeg" width="24%" />
    </p>
    <p>
      <img src="screenshots/widget_recommendation_dinner_small_1.jpeg" width="30%" /> 
      <img src="screenshots/widget_recommendation_dinner_small_2.jpeg" width="30%" /> 
      <img src="screenshots/widget_recommendation_dinner_small_3.jpeg" width="30%" />
    </p>
*   **Diseño Premium y Fluido:** Interfaz oscura con gradientes metálicos y glassmorphic, efectos de deformación elástica (bouncy click), animaciones Lottie, transiciones de tarjetas de perfil y confeti dinámico para marcar favoritos.
    <p><img src="screenshots/home_dashboard_top.jpeg" width="30%" /> <img src="screenshots/home_dashboard_bottom.jpeg" width="30%" /> <img src="screenshots/catalog_menu_navigation.jpeg" width="30%" /></p>

---

## 🛠️ Stack Tecnológico

*   **Lenguaje:** Kotlin
*   **Interfaz Gráfica:** Jetpack Compose (Material Design 3) con diseño Edge-to-Edge
*   **Fuente de Datos Remota:** Firebase Firestore como catálogo principal de recetas + OkHttp para descarga y caché local de imágenes
*   **Manejo de Imágenes:** Coil Compose (con descarga y almacenamiento local en caché)
*   **Animaciones:** Lottie Compose + confeti vectorial personalizado en Canvas
*   **Servicios de Mapas:** Google Maps SDK para Android + Jetpack Compose Maps wrapper
*   **Navegación:** Jetpack Navigation Compose
*   **Componentes de Escritorio:** Android App Widgets (`AppWidgetProvider` + `RemoteViews`) con soporte para redimensionamiento inteligente y persistencia de estado por Widget ID.
*   **Base de Datos Local (Room):** Room 2.8.4 (con TypeConverters basados en Gson) para almacenamiento local de recetas y traducciones de ML Kit.
*   **Servicios en la Nube / Firebase:**
    *   **Firebase Cloud Messaging (FCM v1)** (Notificaciones Push remotas)
    *   **Firebase Authentication** (Registro e inicio de sesión de Chefs de Uami)
    *   **Firebase Firestore** (Catálogo de recetas, comentarios, reacciones y favoritos en la nube)
    *   **Firebase Analytics** (Analítica de uso de la aplicación)
*   **Inteligencia Artificial Local:**
    *   **Google MediaPipe Tasks GenAI** (Inferencia de LLM local)
    *   **Google Gemma-2B-it CPU Quantized** (Modelo de lenguaje local de 4 bits)
    *   **Google ML Kit Translation** (Traductor on-device en tiempo real)

---

## 🧠 Componentes de IA y Funcionamiento

### 1. Planificador Nutricional (Gemma-2B)
El asistente lee el perfil físico y los objetivos del usuario (pérdida de peso, ganancia de masa, etc.) y genera mediante inferencia local un plan de alimentación semanal estructurado en formato JSON, integrando recetas del menú de la app. Si el modelo no está disponible, se activa un **generador offline** con selección ponderada que prioriza las cocinas favoritas del usuario.
*   **Ruta de Inferencia:** `com.google.mediapipe.tasks.genai.llminference.LlmInference`

### 2. Chatbot Culinario (Gemma-2B)
El mismo modelo local potencia un chatbot conversacional que responde preguntas sobre recetas, ingredientes y alimentación. Incluye detección de saludos, clasificación de consultas culinarias, inyección contextual de recetas del catálogo local en los prompts, y filtrado de preguntas fuera de tema.

### 3. Traducción en Dispositivo (ML Kit)
Las recetas obtenidas de Firestore se traducen automáticamente al español término a término y se almacenan en un caché local para acelerar futuras cargas sin consumir ancho de banda. Complementado con un diccionario hardcoded de ~150 términos culinarios como fallback.
*   **Ruta de Inferencia:** `com.google.mlkit.nl.translate.Translator`

---

## 💾 Persistencia de Datos y APIs REST

La aplicación implementa la persistencia de datos y una Arquitectura Limpia (Clean Architecture) estricta:
*   **Arquitectura MVVM Pura**: Separación estricta de responsabilidades. Los `ViewModel` (como `MainViewModel`, `ReviewsViewModel`) exponen estados inmutables mediante `StateFlow`, mientras que las lógicas de red, autenticación y bases de datos están totalmente delegadas a repositorios (`FirebaseRepository`, `AuthManager`, `RecipeRepository`). ¡Cero lógica de negocio o referencias a Firebase en las Vistas (Composables)!
*   **Room Database**: Gestiona localmente las entidades `RecipeEntity` (recetas en caché) y `TranslationEntity` (traducciones localizadas en dispositivo) a través de `RecipeDao` y `TranslationDao`.
*   **Coroutines y Flow**: Todas las operaciones asíncronas de la base de datos se ejecutan en hilos en segundo plano (`Dispatchers.IO`) utilizando `StateFlow` y `Flow` para actualizaciones en tiempo real a la interfaz de usuario de Jetpack Compose.
*   **Sincronización Inteligente sin Caídas**: Si hay un fallo de red o se pierde la conexión, `UpdateManager` maneja correctamente el error de red y recupera de forma transparente los datos desde la base de datos local de Room sin interrumpir la experiencia de usuario.
*   **Aislamiento de Cuentas (UID)**: Los favoritos y planes de alimentación se almacenan de manera independiente en el dispositivo utilizando claves dinámicas basadas en el UID del usuario registrado actual de Firebase Auth para evitar mezcla de datos.

---

## 📋 Requisitos Mínimos

| Requisito | Valor |
|-----------|-------|
| **Android mínimo** | API 24 (Android 7.0 Nougat) |
| **Android objetivo** | API 36 |
| **RAM recomendada** | ≥ 4 GB (el modelo Gemma-2B requiere `largeHeap`) |
| **Espacio en disco** | ~1.3 GB adicionales para el modelo de IA |


## 📺 Demostración en Video
Puedes ver el video explicativo y la demostración de la app con datos reales e inferencia local aquí:
*   🎬 **Asistente de Voz en Modo Cocina:** [Ver video demo](screenshots/cooking_mode_voice_assistant.mp4) — Demostración de la lectura por voz paso a paso.

---

## 👥 Integrante
*   **Ivan Apaza Quispe**

---

## ⚙️ Instrucciones de Configuración y Uso

### 1. Clonar el repositorio
```bash
git clone https://github.com/bryanapazaquispev5-droid/GLAB---S9---BPAREJA---2025.git
```

### 2. Archivo del Modelo Gemma-2B
Para que el Nutriólogo AI funcione, la aplicación necesita el modelo de lenguaje Gemma-2B.
*   **Descarga Automática:** La primera vez que abras la app y accedas a la pestaña "Nutriólogo", la app intentará descargar el archivo `gemma-2b-it-cpu-int4.bin` automáticamente desde HuggingFace y lo almacenará en el almacenamiento interno de la app.
*   **Descarga Manual:** Puedes descargar el archivo desde HuggingFace y transferirlo al dispositivo en la carpeta interna de la app:
    `Android/data/com.example.uami/files/gemma-2b-it-cpu-int4.bin`
*   **Importación Local:** También puedes importar el modelo desde un archivo local en tu dispositivo usando el selector de archivos integrado en la app.

### 3. API Keys de Servicios Externos
*   **Inteligencia Artificial y Traducción:** Este proyecto **no requiere de ninguna credencial o API Key** en la nube (las dependencias de IA y traducción se ejecutan local y offline).
*   **Firebase Cloud Messaging:** El proyecto cuenta con un archivo `google-services.json` configurado en el directorio `/app` para enlazar la aplicación con el proyecto Firebase `uami-3bfe5`.
*   **Google Maps SDK:** El mapa interactivo utiliza una API Key. Por seguridad, la clave se lee de tu archivo local no trackeado. Puedes configurar tu clave en el archivo `local.properties` del proyecto raíz:
    ```properties
    MAPS_API_KEY=AIzaSyTuClaveRealDeGoogleMapsAqui
    ```
    Si no se especifica ninguna clave, el build de Gradle utilizará un valor de demostración por defecto (`AIzaSyDummyKeyForGoogleMapsShowcase`), el cual te permitirá compilar y ver el mapa (aunque Google Maps podría mostrar marcas de agua o cuadrículas si la clave no es válida).
