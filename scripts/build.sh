#!/bin/bash

echo "🛠️ Compilando todos os arquivos Java..."

mkdir -p ../run

javac -cp "../lib/*" -d ../run ../src/main/common/*.java ../src/main/entities/*.java ../src/main/server/*.java ../src/main/client/*.java

if [ $? -eq 0 ]; then
  echo "✅ Compilação bem-sucedida!"
else
  echo "❌ Erro na compilação."
fi
