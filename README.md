# Uami - Asistente Gourmet e IA de Cocina Offline

**Uami** es una aplicación móvil nativa para Android diseñada para simplificar y enriquecer la experiencia de cocinar en el hogar. Combina un catálogo gourmet inteligente con un asistente de nutrición basado en modelos de lenguaje avanzados que se ejecutan completamente en el dispositivo de forma segura y privada.

---

## 🚀 Características Principales

*   **Modo Cocina Interactivo:** Preparación guiada paso a paso con una lista de ingredientes previa e interactiva al 100% que avanza automáticamente. Transición visual tipo "hoja de papel despegada de la pared" al pasar de página.
*   **Asistente de Voz (TTS):** Lectura por síntesis de voz del paso actual para cocinar con manos libres.
*   **Nutriólogo AI Local:** Un planificador de dietas y menús de 7 días impulsado por **Gemma-2B** corriendo localmente en el procesador del dispositivo, garantizando privacidad total y funcionamiento sin internet.
*   **Traducción Inteligente On-Device:** Traducción en tiempo real de recetas del inglés al español usando **Google ML Kit**.
*   **Explorador de Supermercados (Google Maps):** Mapa interactivo de supermercados y mercados tradicionales en Arequipa con estilo oscuro personalizado para buscar los ingredientes, y enlace a navegación con Google Maps.
*   **Notificaciones Push Inteligentes (FCM):** Canal de notificaciones remotas mediante Firebase Cloud Messaging para alertas de recetas del día, sugerencias personalizadas de nutrición y tips de salud interactivos.
*   **Opiniones de la Comunidad y Reacciones (Firestore):** Módulo para publicar reseñas con calificación por estrellas y comentarios en tiempo real. Cuenta con un dashboard de calificación animado, un halo sweep-gradient rotatorio para destacar avatares con calificaciones de 5 estrellas, y soporte de reacciones ("Me Gusta") en tiempo real con físicas de rebote elástico.
*   **Sincronización en la Nube de Favoritos**: Sistema híbrido (local y remoto) que sincroniza en tiempo real los favoritos del chef con Firebase Firestore. Si el usuario inicia sesión por primera vez, realiza una fusión inteligente (unión) de sus favoritos de invitado con la nube para evitar pérdida de datos.
*   **Avatares Personalizados y Perfil de Chef**: Registro e inicio de sesión integrados donde el chef puede elegir un avatar preestablecido o subir su propia foto de perfil de galería (comprimida y convertida a Base64 en memoria de forma optimizada para caber en Firestore), actualizando su identidad al instante en todas las pantallas.
*   **Diseño Premium y Fluido:** Interfaz oscura con gradientes metálicos y glassmorphic, efectos de deformación elástica (bouncy click), animaciones Lottie, transiciones de tarjetas de perfil y confeti dinámico para marcar favoritos.

---

## 🛠️ Stack Tecnológico

*   **Lenguaje:** Kotlin
*   **Interfaz Gráfica:** Jetpack Compose (Material Design 3)
*   **Consumo de APIs:** Retrofit 2 + OkHttp (para descargar catálogo de recetas)
*   **Manejo de Imágenes:** Coil Compose (con descarga y almacenamiento local en caché)
*   **Animaciones:** Lottie Compose + confeti vectorial personalizado en Canvas
*   **Servicios de Mapas:** Google Maps SDK para Android + Jetpack Compose Maps wrapper
*   **Navegación:** Jetpack Navigation Compose
*   **Servicios en la Nube / Firebase:**
    *   **Firebase Cloud Messaging (FCM v1)** (Notificaciones Push remotas)
    *   **Firebase Authentication** (Registro e inicio de sesión de Chefs de Uami)
    *   **Firebase Firestore** (Base de datos NoSQL en tiempo real para comentarios, reacciones y favoritos en la nube)
*   **Inteligencia Artificial Local:**
    *   **Google MediaPipe Tasks GenAI** (Inferencia de LLM local)
    *   **Google Gemma-2B-it CPU Quantized** (Modelo de lenguaje local de 4 bits)
    *   **Google ML Kit Translation** (Traductor on-device en tiempo real)

---

## 🧠 Componentes de IA y Funcionamiento

### 1. Planificador Nutricional (Gemma-2B)
El asistente lee el perfil físico y los objetivos del usuario (pérdida de peso, ganancia de masa, etc.) y genera mediante inferencia local un plan de alimentación semanal estructurado en formato JSON, integrando recetas del menú de la app.
*   **Ruta de Inferencia:** `com.google.mediapipe.tasks.genai.llminference.LlmInference`

### 2. Traducción en Dispositivo (ML Kit)
Las recetas obtenidas de la API externa se traducen automáticamente al español término a término y se almacenan en un caché local para acelerar futuras cargas sin consumir ancho de banda.
*   **Ruta de Inferencia:** `com.google.mlkit.nl.translate.Translator`

---

## 📸 Capturas de Pantalla

| Pantalla Principal | Lista de Ingredientes | Nutriólogo AI |
| :---: | :---: | :---: |
| ![Inicio](app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png) | *(Captura 2)* | *(Captura 3)* |

---

## 📺 Demostración en Video
Puedes ver el video explicativo y la demostración de la app con datos reales e inferencia local aquí:
*   🎥 **Enlace al Video de YouTube:** [Ver Demo en YouTube](https://www.youtube.com/watch?v=TU_VIDEO_AQUI) *(Reemplazar por tu enlace real)*

---

## 👥 Integrantes del Grupo
*   **Integrante:** [Tu Nombre Completo]

---

## ⚙️ Instrucciones de Configuración y Uso

### 1. Clonar el repositorio
```bash
git clone https://github.com/tu-usuario/Uami.git
```

### 2. Archivo del Modelo Gemma-2B
Para que el Nutriólogo AI funcione, la aplicación necesita el modelo de lenguaje Gemma-2B.
*   **Descarga Automática:** La primera vez que abras la app y accedas a la pestaña "Nutriólogo", la app intentará descargar el archivo `gemma-2b-it-cpu-int4.bin` automáticamente desde HuggingFace y lo almacenará en el almacenamiento interno de la app.
*   **Descarga Manual:** Puedes descargar el archivo desde HuggingFace y transferirlo al dispositivo en la carpeta interna de la app:
    `Android/data/com.example.uami/files/gemma-2b-it-cpu-int4.bin`

### 3. API Keys de Servicios Externos
*   **Inteligencia Artificial y Traducción:** Este proyecto **no requiere de ninguna credencial o API Key** en la nube (las dependencias de IA y traducción se ejecutan local y offline).
*   **Firebase Cloud Messaging:** El proyecto cuenta con un archivo `google-services.json` configurado en el directorio `/app` para enlazar la aplicación con el proyecto Firebase `uami-3bfe5`.
*   **Google Maps SDK:** El mapa interactivo utiliza una API Key. Por seguridad, la clave se lee de tu archivo local no trackeado. Puedes configurar tu clave en el archivo `local.properties` del proyecto raíz:
    ```properties
    MAPS_API_KEY=AIzaSyTuClaveRealDeGoogleMapsAqui
    ```
    Si no se especifica ninguna clave, el build de Gradle utilizará un valor de demostración por defecto (`AIzaSyDummyKeyForGoogleMapsShowcase`), el cual te permitirá compilar y ver el mapa (aunque Google Maps podría mostrar marcas de agua o cuadrículas si la clave no es válida).
