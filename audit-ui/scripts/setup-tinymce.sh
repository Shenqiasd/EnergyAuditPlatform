#!/bin/bash
# Copy TinyMCE static assets from node_modules to public/tinymce
# Run this after `npm install` or when setting up the project for the first time.

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
TINYMCE_SRC="$PROJECT_DIR/node_modules/tinymce"
TINYMCE_DEST="$PROJECT_DIR/public/tinymce"

if [ ! -d "$TINYMCE_SRC" ]; then
  echo "[setup-tinymce] tinymce not found in node_modules. Run 'npm install' first."
  exit 1
fi

echo "[setup-tinymce] Copying TinyMCE assets to public/tinymce..."

rm -rf "$TINYMCE_DEST"
mkdir -p "$TINYMCE_DEST"

# Copy only the directories needed for self-hosted TinyMCE
cp -r "$TINYMCE_SRC/skins"   "$TINYMCE_DEST/skins"
cp -r "$TINYMCE_SRC/themes"  "$TINYMCE_DEST/themes"
cp -r "$TINYMCE_SRC/icons"   "$TINYMCE_DEST/icons"
cp -r "$TINYMCE_SRC/models"  "$TINYMCE_DEST/models"
cp -r "$TINYMCE_SRC/plugins" "$TINYMCE_DEST/plugins"

# Copy Chinese language file
mkdir -p "$TINYMCE_DEST/langs"
if [ -f "$SCRIPT_DIR/tinymce-zh_CN.js" ]; then
  cp "$SCRIPT_DIR/tinymce-zh_CN.js" "$TINYMCE_DEST/langs/zh_CN.js"
  echo "[setup-tinymce] Chinese language file copied."
fi

echo "[setup-tinymce] Done. TinyMCE assets are in public/tinymce/"
