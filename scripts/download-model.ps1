param(
  [string]$ModelDir = "$(Join-Path (Split-Path $PSScriptRoot -Parent) 'models')"
)

$ErrorActionPreference = 'Stop'
$repo = 'Qwen/Qwen2.5-1.5B-Instruct-GGUF'
$file = 'qwen2.5-1.5b-instruct-q4_k_m.gguf'

New-Item -ItemType Directory -Force -Path $ModelDir | Out-Null
if (-not (Get-Command huggingface-cli -ErrorAction SilentlyContinue)) {
  throw "Instala primero: python -m pip install -U huggingface_hub"
}

huggingface-cli download $repo $file --local-dir $ModelDir
Write-Host "Modelo descargado en $ModelDir\$file"
