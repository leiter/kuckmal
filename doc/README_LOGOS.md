# How to Update TV Channel Logos

## Quick Start

### Step 1: Download Logos
Open `LOGO_DOWNLOAD_GUIDE.md` and click the links to download high-quality logos for each channel.

**Recommended download locations per channel:**
- **Most channels:** Use **Wikimedia Commons** (highest quality, free, SVG format)
- **Alternative:** Use **SeekLogo** (PNG/SVG available, easy download)

**Download to:** `tmp/downloaded_logos/`

### Step 2: Process Logos
Once you've downloaded logos, run the processing script:

```bash
cd tmp
python3 process_logos.py
```

This will:
- ✅ Resize all logos to 300px width (maintains aspect ratio)
- ✅ Convert to PNG with transparency
- ✅ Optimize file size
- ✅ Rename to correct filenames
- ✅ Copy to `app/src/main/res/drawable/`

### Step 3: Build App
```bash
cd ..
./gradlew assembleDebug
```

---

## Files Created

### 1. `LOGO_DOWNLOAD_GUIDE.md`
Complete guide with direct links to download all 19 channel logos from multiple sources.

### 2. `process_logos.py`
Python script that processes downloaded logos automatically.

### 3. `downloaded_logos/` (directory)
Place your downloaded logo files here (any format: PNG, SVG, JPG, etc.)

---

## Detailed Workflow

### Download Logos (Manual)
1. Open `LOGO_DOWNLOAD_GUIDE.md` in a browser or text editor
2. For each channel, click the recommended link (usually Wikimedia Commons)
3. Download PNG or SVG format (SVG preferred for quality)
4. Save to `tmp/downloaded_logos/` with any recognizable name
   - Examples: `3sat-logo.svg`, `ard.png`, `zdf_2024.svg`
   - The script will auto-detect by channel name

### Process Logos (Automated)
```bash
cd tmp
python3 process_logos.py
```

The script will:
- Search for logos by channel name (case-insensitive)
- Resize to optimal size for Android (300px width)
- Convert everything to PNG with transparency
- Save with correct Android resource names
- Automatically duplicate ARTE and SRF logos where needed

### Rebuild App
```bash
cd ..
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## Tips

### If logo download fails or you can't find a logo:
- Try the alternative sources listed in `LOGO_DOWNLOAD_GUIDE.md`
- Search Google Images for "[channel name] logo svg transparent"
- Check the broadcaster's official website press/media section

### If the processing script can't find a logo:
- Make sure the filename contains the channel name
- Examples: `3sat.png` ✅, `ard-logo.svg` ✅, `image123.png` ❌

### Current logos:
Your current logos are already in `app/src/main/res/drawable/`
- They will be overwritten when you process new ones
- You can back them up first if needed:
  ```bash
  cp -r app/src/main/res/drawable app/src/main/res/drawable_backup
  ```

---

## Logo Requirements

**For best results:**
- **Format:** PNG with transparency (alpha channel)
- **Size:** 200-400px width
- **Aspect ratio:** Maintain original (don't stretch)
- **Background:** Transparent
- **Quality:** High resolution, crisp edges

**The processing script handles all of this automatically!**

---

## Channels Needed

1. _3sat.png
2. ard.png
3. arte_de.png (+ arte_fr.png - auto-duplicated)
4. br.png
5. hr.png
6. kika.png
7. mdr.png
8. ndr.png
9. orf.png
10. phoenix.png
11. rbb.png
12. sr.png
13. srf.png (+ srf_podcast.png - auto-duplicated)
14. swr.png
15. wdr.png
16. zdf.png
17. zdf_tivi.png

**Total:** 17 unique logos (19 files after duplication)

---

## Questions?

If you have any issues or need help with specific logos, let me know!
