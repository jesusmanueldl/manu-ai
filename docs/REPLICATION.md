# Replicar MANU

## Requisitos

- Android Studio reciente con SDK 35.
- JDK 17.
- Android NDK 26.3.11579264.
- CMake 3.22 o posterior.
- Un teléfono Android ARM64 con depuración USB autorizada.
- Python 3.10 o posterior para descargar el modelo.
- Espacio libre: aproximadamente 2 GB en el PC y 1.5 GB en el teléfono.

## 1. Clonar el proyecto

```bash
git clone https://github.com/jesusmanueldl/manu-ai.git
cd manu-ai
```

## 2. Obtener llama.cpp

El código de llama.cpp no se sube a este repositorio para evitar duplicar un
proyecto de terceros. Descárgalo en `third_party/llama.cpp`:

```bash
git clone --depth 1 https://github.com/ggml-org/llama.cpp third_party/llama.cpp
```

Para compilar la biblioteca Android, usa el procedimiento Android oficial de
llama.cpp y coloca sus bibliotecas ARM64 en:

```text
third_party/llama.cpp/build-android/bin/
```

Se necesitan `libllama.so`, `libllama-common.so`, `libggml.so`,
`libggml-base.so`, `libggml-cpu.so` y `libc++_shared.so`.

Con PowerShell, la biblioteca JNI de MANU se puede compilar con:

```powershell
.\scripts\build-native.ps1
```

El script usa el NDK 26.3.11579264, la arquitectura `arm64-v8a` y rutas
relativas al repositorio. También puede usarse CMake con la variable
`LLAMA_ROOT`:

```powershell
cmake -S native -B "$env:TEMP\manu-native-cmake" -G Ninja `
  -DCMAKE_TOOLCHAIN_FILE="$env:LOCALAPPDATA\Android\Sdk\ndk\26.3.11579264\build\cmake\android.toolchain.cmake" `
  -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=android-26 `
  -DLLAMA_ROOT="$PWD\third_party\llama.cpp"
cmake --build "$env:TEMP\manu-native-cmake"
```

## 3. Descargar el modelo

Los pesos no forman parte del repositorio. Cada usuario debe descargarlos
desde el repositorio del modelo y aceptar sus condiciones de uso:

```powershell
python -m pip install -U huggingface_hub
.\scripts\download-model.ps1
```

El archivo esperado es:

```text
models/qwen2.5-1.5b-instruct-q4_k_m.gguf
```

También se puede usar otro modelo GGUF compatible, modificando el nombre en
`LocalModel.java` y comprobando que su plantilla de chat sea compatible.

## 4. Preparar el teléfono

Activa Opciones de desarrollador y Depuración USB. Conecta el teléfono y
comprueba:

```bash
adb devices
```

El modelo se copia al teléfono como:

```text
/data/local/tmp/manu/model.gguf
```

MANU usa primero el archivo que ya existe en su almacenamiento privado
(`files/manu/model.gguf`). Solo usa `/data/local/tmp/manu/model.gguf` como
fuente de primera instalación o recuperación. Después de copiarlo, las
siguientes ejecuciones no dependen de `/data/local/tmp`. No se necesita root
ni desbloquear el bootloader.

## 5. Abrir y compilar

Abre la carpeta en Android Studio y sincroniza Gradle. Ejecuta la aplicación en
un dispositivo ARM64. El proyecto utiliza Java, layouts nativos y JNI; no usa
Compose.

La integración actual también puede compilarse con las herramientas Android
SDK (`aapt2`, `javac`, `d8`, `zipalign` y `apksigner`) si se trabaja en un
entorno sin Gradle Wrapper.

## Arquitectura

```text
MainActivity
   ├── IntentRouter → herramientas Android (RAM, batería, apps, SMS)
   ├── MemoryStore → SQLite local
   └── LocalModel → JNI → llama.cpp → modelo GGUF
```

El modelo no ejecuta acciones directamente. La aplicación valida la intención,
ejecuta una herramienta y devuelve el resultado al chat. Las acciones sensibles
requieren confirmación.

`llama.cpp` mantiene el modelo cargado en una caché nativa durante la vida del
proceso. Cada consulta crea un contexto de inferencia independiente y libera
solo ese contexto; los pesos no se vuelven a cargar en cada mensaje.

La API JNI `nativeGenerateStream` informa tokens parciales, latencia del primer
token, tokens del prompt, tokens generados y tiempo total. La llamada se ejecuta
en el `ExecutorService` de `LocalModel`, por lo que no bloquea el hilo de UI.

## Privacidad y límites

- El modelo funciona localmente y no requiere internet.
- El historial SQLite queda en el almacenamiento privado de la app.
- El repositorio no contiene pesos, conversaciones, APKs, bases de datos ni
  datos del teléfono del autor.
- La búsqueda web y la cámara no están habilitadas todavía.
- El rendimiento depende de la RAM, CPU, temperatura y modelo elegido.

## Problemas frecuentes

- `Permission denied` al ejecutar `llama-cli`: no ejecutes un binario desde
  `/data/local/tmp`; MANU utiliza la biblioteca JNI cargada por Android.
- `UnsatisfiedLinkError`: comprueba que todas las bibliotecas ARM64 estén en
  el APK y que `libmanu_jni.so` tenga sus dependencias disponibles.
- Respuestas lentas: reduce el contexto, usa un modelo cuantizado más pequeño
  o baja el número máximo de tokens.
- Si se cancela un SMS, MANU elimina el estado de procesamiento y conserva la
  conversación en SQLite.
