<h1 align="center">Lineage Launcher (Trebuchet)</h1>

<div align="center">

<p><i>Launcher independiente y moderno basado en LineageOS Launcher3 (Trebuchet) con Dock Desplazable, creación dinámica de páginas al arrastrar, animaciones en blanco y negro (Material You Monocromático) y actualizador integrado vía GitHub Releases.</i></p>

[![Android CI & Release Build](https://github.com/rhythmcreative/lineage-launcher/actions/workflows/android-build.yml/badge.svg)](https://github.com/rhythmcreative/lineage-launcher/actions/workflows/android-build.yml)
[![Release](https://img.shields.io/github/v/release/rhythmcreative/lineage-launcher?style=for-the-badge&color=2EA44F)](https://github.com/rhythmcreative/lineage-launcher/releases/latest)
[![Platform](https://img.shields.io/badge/Android-10%2B%20(API%2029%2B)-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg?style=for-the-badge)](LICENSE)

</div>

---

## 📥 Descarga del APK / Download APK

Puedes descargar el archivo `.apk` listo para instalar directamente desde los lanzamientos oficiales de GitHub:

👉 **[Descargar LineageLauncher APK (Última versión)](https://github.com/rhythmcreative/lineage-launcher/releases/latest)**

---

## ✨ Características Principales (Features)

### 📱 1. Dock Desplazable Multipágina (Scrollable Dock)
* **Navegación horizontal fluida:** Desliza entre múltiples páginas de iconos en la barra inferior (dock), manteniendo tus aplicaciones esenciales siempre al alcance.
* **Arrastrar a nueva página (Drag-to-New-Page):** Arrastra cualquier icono o acceso directo hacia el borde derecho o izquierdo del dock; tras una pausa de 450 ms, se crea automáticamente una nueva página vacía y se desplaza hacia ella.
* **Desplazamiento infinito (Bucle):** Opción en ajustes para pasar de forma continua de la última página a la primera.
* **Indicadores adaptativos monocromáticos:** Puntos de página Material 3 de alto contraste con halo protector para máxima visibilidad sobre cualquier fondo de pantalla.

### 🎨 2. Animaciones y Diseño en Blanco y Negro (B&W / Themed Icons)
* **Previsualización vectorial interactiva:** En **Ajustes ➔ Dock desplazable**, disfruta de una animación vectorizada fluida en blanco y negro puro, con iconos estilizados monocromáticos y sin colores estridentes ni puntos azules.
* **Animación Lottie oficial:** Incluye el archivo interactivo `dock_swipe.json` en los recursos de la aplicación para demostrar el gesto de navegación con asistencia de movimiento.
* **Integración Material You:** Compatible con temas oscuros, claros y paletas dinámicas monocromáticas.

### 🔄 3. Actualizador Integrado en la App (In-App Auto-Updater)
* **Conexión directa con GitHub Releases:** La aplicación consulta directamente la API oficial de GitHub (`https://api.github.com/repos/rhythmcreative/lineage-launcher/releases/latest`).
* **Instalación en un toque:** Detecta automáticamente si hay una nueva versión disponible, descarga el archivo `.apk` mediante `DownloadManager` y abre el instalador de Android mediante `FileProvider`.
* **Comprobación automática:** Opción configurable en ajustes para comprobar actualizaciones al iniciar el launcher.

### ⚡ 4. Arquitectura Standalone y Ligera
* Proyecto Gradle moderno (`compileSdk = 35`, `minSdk = 29`).
* Compatible con cualquier dispositivo Android 10, 11, 12, 13, 14, 15 y 16 (Pixel, LineageOS, Samsung, Motorola, etc.).
* Automatización CI/CD con **GitHub Actions** que genera los archivos APK automáticamente en cada commit y versión etiquetada.

---

## ⚙️ Ajustes del Launcher (Settings)

Dentro de la aplicación, accede a los ajustes pulsando el icono de engranaje en la barra de búsqueda inferior (QSB) o desde el menú de aplicaciones:

* **Dock desplazable:**
  * Activar / Desactivar dock desplazable.
  * Arrastrar a nueva página automáticamente.
  * Desplazamiento infinito (bucle).
  * Mostrar / Ocultar indicadores de página.
  * Previsualización animada en tiempo real.
* **Animación y Asistencia de Movimiento:**
  * Reproductor interactivo de la animación Lottie.
* **Actualizaciones:**
  * Botón para buscar actualizaciones manualmente.
  * Interruptor para búsqueda automática en segundo plano.
  * Notas de versión (Changelog) detalladas.

---

## 🛠️ Compilación desde el código fuente (Building)

### Requisitos:
* JDK 17 o superior.
* Android SDK (API 35).

```bash
# Clonar el repositorio
git clone https://github.com/rhythmcreative/lineage-launcher.git
cd lineage-launcher

# Compilar APK Debug
./gradlew assembleDebug

# Compilar APK Release
./gradlew assembleRelease
```

Los archivos APK generados se guardarán en:
* `app/build/outputs/apk/debug/app-debug.apk`
* `app/build/outputs/apk/release/app-release.apk`

---

## 📄 Licencia

Este proyecto está bajo la licencia [Apache 2.0](LICENSE).
Basado en LineageOS Launcher3 (Trebuchet) y desarrollado por [rhythmcreative](https://github.com/rhythmcreative).
