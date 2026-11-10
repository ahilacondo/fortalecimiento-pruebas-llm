#!/usr/bin/env bash
# Instala Defects4J y las herramientas necesarias en Ubuntu (WSL2 en Windows). Ejecutar desde WSL:
#   bash scripts/instalar_defects4j.sh
set -euo pipefail

echo "== 1/4 Paquetes del sistema =="
sudo apt-get update
sudo apt-get install -y openjdk-11-jdk git perl cpanminus subversion curl unzip build-essential python3 python3-venv python3-pip

echo "== 2/4 Defects4J =="
cd "$HOME"
if [ ! -d defects4j ]; then
  git clone https://github.com/rjust/defects4j.git
fi
cd defects4j
cpanm --installdeps .
./init.sh        # descarga los repositorios de los proyectos; puede tardar bastante

echo "== 3/4 Variables de entorno =="
if ! grep -q "defects4j/framework/bin" "$HOME/.bashrc"; then
  echo 'export PATH=$PATH:$HOME/defects4j/framework/bin' >> "$HOME/.bashrc"
fi
export PATH=$PATH:$HOME/defects4j/framework/bin
# Defects4J necesita Java 11
export JAVA_HOME=$(dirname "$(dirname "$(readlink -f "$(which javac)")")")
echo "JAVA_HOME=$JAVA_HOME"
java -version

echo "== 4/4 Comprobación =="
defects4j info -p Lang | head -n 12
cd "$HOME"
rm -rf /tmp/lang_1b && defects4j checkout -p Lang -v 1b -w /tmp/lang_1b
cd /tmp/lang_1b && defects4j compile && defects4j test -t org.apache.commons.lang3.math.NumberUtilsTest::TestLang747 || true
echo "Listo. Si 'defects4j info' y el checkout funcionaron, el entorno está bien."
