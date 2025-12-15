# Test Database Setup

This folder contains assets for integration testing.

## Required File: `test_media_database.db`

The `SearchQueryIntegrationTest` requires a production database snapshot.

### How to Create the Database File

1. **Run the app and load Filmliste:**
   ```bash
   ./gradlew assembleDebug
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   # Open app, load Filmliste-akt.json, wait for completion (685k+ entries)
   ```

2. **Export database from device:**
   ```bash
   adb exec-out run-as cut.the.crap cat databases/media_database > test_media_database.db
   ```

3. **Place in this folder:**
   ```bash
   mv test_media_database.db app/src/androidTest/assets/
   ```

4. **Verify file size:**
   ```bash
   ls -lh app/src/androidTest/assets/test_media_database.db
   # Should be 50-150MB depending on data
   ```

5. **Add to .gitignore (optional, if too large for repo):**
   ```bash
   echo "app/src/androidTest/assets/test_media_database.db" >> .gitignore
   ```

### Running Tests

```bash
./gradlew connectedDebugAndroidTest
```

The test will automatically:
1. Copy the database from assets to the test device
2. Open and verify the database
3. Run search queries against it
4. Clean up after tests complete

### Notes

- **File size**: Full database is 50-150MB. Consider trimming if too large.
- **Git**: Large binary files may bloat the repo. Use Git LFS or .gitignore.
- **Updates**: Re-export periodically to keep test data fresh.
- **CI/CD**: Store database file as build artifact or in cloud storage.
