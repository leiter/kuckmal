#!/usr/bin/env bash
#
# Generates the GitHub Pages site under docs/ from the canonical store documents
# in appstore/. Run this after editing any of the source files below, then commit
# both the source and the generated page.
#
# The privacy policy and support pages must stay reachable at stable URLs because
# App Store Connect and the Play Console link to them directly:
#
#   https://leiter.github.io/kuckmal/privacy/       (de, App Store primary)
#   https://leiter.github.io/kuckmal/privacy/en/
#   https://leiter.github.io/kuckmal/support/       (de)
#   https://leiter.github.io/kuckmal/support/en/
#
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SRC="$ROOT/appstore"
OUT="$ROOT/docs"

# emit <source.md> <output/path/index.md> <front-matter title>
emit() {
  local src="$1" dest="$2" title="$3"
  mkdir -p "$(dirname "$dest")"
  {
    printf -- '---\n'
    printf -- 'layout: default\n'
    printf -- 'title: "%s"\n' "$title"
    printf -- '---\n\n'
    # Jekyll only renders files that carry YAML front matter, hence the prepend.
    cat "$src"
  } > "$dest"
  echo "  $(basename "$(dirname "$dest")")/$(basename "$dest")  <-  appstore/$(basename "$src")"
}

echo "Building docs/ site from appstore/ sources:"
emit "$SRC/privacy-policy-de.md" "$OUT/privacy/index.md"    "Datenschutzerklärung"
emit "$SRC/privacy-policy-en.md" "$OUT/privacy/en/index.md" "Privacy Policy"
emit "$SRC/support-page-de.md"   "$OUT/support/index.md"    "Hilfe und Support"
emit "$SRC/support-page.md"      "$OUT/support/en/index.md" "Help and Support"

echo
echo "Done. docs/index.md is maintained by hand and was not touched."
