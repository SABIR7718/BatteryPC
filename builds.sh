#!/usr/bin/env bash
set -e

APP_DIR="$HOME/apps/battery-pc"
SDK="$PREFIX/share/android-sdk/platforms/android-34/android.jar"
AAPT2="$PREFIX/bin/aapt2"
DX="/data/data/com.termux/files/usr/share/android-sdk/build-tools/28.0.3/android-9/dx"
D8="$PREFIX/share/android-sdk/build-tools/android-11/d8"
KEYSTORE="$APP_DIR/mykey.keystore"

cd "$APP_DIR"

echo "🧹 Cleaning old build..."
rm -rf build dex

echo "📁 Creating build directories..."
mkdir -p build/classes build/generated build/res dex build/staging

echo "🎨 Compiling resources..."
$AAPT2 compile -o build/res $(find res -type f)

echo "🔗 Linking resources..."
$AAPT2 link \
    -I "$SDK" \
    --manifest AndroidManifest.xml \
    --java build/generated \
    --custom-package com.sabir7718 \
    --min-sdk-version 26 \
    --target-sdk-version 34 \
    -o build/unsigned.apk \
    build/res/*.flat

echo "☕ Compiling Java files..."
javac -source 1.8 -target 1.8 -g \
  -bootclasspath "$SDK" \
  -classpath "libs/*:build/generated" \
  -d build/classes \
  $(find src -name "*.java") \
  $(find build/generated -name "*.java")

echo "📦 Merging class files..."
cp -r build/classes/* build/staging/

# Unpack library classes directly into staging
for jar in libs/*.jar; do
    unzip -o "$jar" -d build/staging/ >/dev/null 2>&1 || true
done

# Remove META-INF from staging
rm -rf build/staging/META-INF

echo "📦 Dexing with D8..."
( cd build/staging && zip -qr ../staging.jar . )
$D8 \
    --min-api 26 \
    --output build \
    build/staging.jar

echo "📦 Packaging APK..."
cd build

if [ -d "../assets" ]; then
    cp -r ../assets .
    zip -ur unsigned.apk assets/
fi

zip -u unsigned.apk classes.dex

echo "🔐 Signing APK..."
apksigner sign \
    --ks "$KEYSTORE" \
    --ks-pass pass:123456 \
    --key-pass pass:123456 \
    --out signed.apk \
    unsigned.apk

echo "🎉 BUILD SUCCESSFUL: $APP_DIR/build/signed.apk"