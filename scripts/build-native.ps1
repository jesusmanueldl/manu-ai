param(
  [string]$SdkRoot = "$(Join-Path $env:LOCALAPPDATA 'Android\Sdk')",
  [string]$LlamaRoot = "$(Join-Path (Split-Path $PSScriptRoot -Parent) 'third_party\llama.cpp')",
  [string]$OutputDir = "$(Join-Path $env:TEMP 'manu-native-out')"
)

$ErrorActionPreference = 'Stop'
$ndk = Join-Path $SdkRoot 'ndk\26.3.11579264'
$clang = Join-Path $ndk 'toolchains\llvm\prebuilt\windows-x86_64\bin\clang++.exe'
$jni = Join-Path $ndk 'toolchains\llvm\prebuilt\windows-x86_64\sysroot\usr\include'
$source = Join-Path (Split-Path $PSScriptRoot -Parent) 'native\manu_jni.cpp'

foreach ($path in @($clang, (Join-Path $LlamaRoot 'include\llama.h'), (Join-Path $LlamaRoot 'ggml\include\ggml.h'))) {
  if (-not (Test-Path -LiteralPath $path)) { throw "No se encontró: $path" }
}
New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null
& $clang --target=aarch64-linux-android26 -fPIC -shared $source `
  -I $jni -I (Join-Path $LlamaRoot 'include') -I (Join-Path $LlamaRoot 'ggml\include') `
  -L (Join-Path $LlamaRoot 'build-android\bin') -lllama -llog -landroid `
  -o (Join-Path $OutputDir 'libmanu_jni.so')
if ($LASTEXITCODE -ne 0) { throw "Falló la compilación de libmanu_jni.so" }
Write-Host "JNI compilado: $(Join-Path $OutputDir 'libmanu_jni.so')"
