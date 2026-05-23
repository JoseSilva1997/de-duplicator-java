# Guest List Cleaner

A small desktop tool that deduplicates a guest list against a list of confirmed attendees. Reads `.xlsx`, `.xls`, and `.csv`; writes cleaned `.xlsx` output.

## For end users

### Install

Download the latest installer from the [Releases page](../../releases) and run it. The installer:

- Installs to `%LOCALAPPDATA%\Programs\Guest List Cleaner` (no admin rights required)
- Creates a desktop shortcut and a Start menu entry
- Bundles its own Java runtime, so you don't need to install anything else

To uninstall, go to **Settings → Apps → Installed apps**, find "Guest List Cleaner", and click Uninstall.

### Use

1. Open the app from the desktop shortcut.
2. Drop your **guest list** onto the left card, or click to choose a file.
3. Drop your **attendees list** onto the right card.
4. If a file has multiple sheets, pick which ones to include.
5. Click **Remove duplicates**. The cleaned files are written to your **Desktop** as:
   - `Updated guests list from <name>.xlsx`: the cleaned guest list.
   - `People removed from <name>.xlsx`: every removed guest with the match reason and confidence score.

Use the cog icon in the top-right to open the side panel for settings (e.g. whether to drop rows with no email address) and a description of how the matching pipeline works.

## For developers

### Prerequisites

- **JDK 17+** (Eclipse Temurin, Oracle, etc.)
- **Maven** on PATH
- **WiX Toolset 3.x** (only needed for building installers, not for running the app)

### Run from source

```powershell
mvn -q compile exec:java "-Dexec.mainClass=App"
```

Outputs are written to `<project>/outputs/` during development.

### Build a jar

```powershell
mvn -q clean package
java -jar target/guest-list-cleaner-1.0.0-SNAPSHOT.jar
```

`maven-shade-plugin` bundles all dependencies into the jar; no extra classpath needed.

### Build a Windows installer

```powershell
.\build-exe.ps1
```

This produces a `.exe` installer at the repo root, named `Guest List Cleaner-<version>.exe`.

How it works:

1. Runs `mvn clean package` to produce the shaded jar.
2. Calls `jpackage` (bundled with JDK 14+) to package the jar plus a minimal JRE into a Windows installer.
3. Auto-bumps the patch version on every run, reading and writing `installer-version.txt`.

To make a non-patch version jump (e.g. `1.0.5` → `1.1.0`), edit `installer-version.txt` manually before running the script.

### Releasing

1. Run `.\build-exe.ps1` to produce a fresh installer.
2. Test the installer on a clean machine (or a VM).
3. Tag the commit: `git tag v<version> && git push --tags` (where `<version>` matches `installer-version.txt`).
4. Create a release on GitHub for that tag, drag the `.exe` into the release as an asset.
5. End users download from the Releases page.

## License

[PolyForm Noncommercial 1.0.0](LICENSE). You may use, modify, and share this software for any non-commercial purpose. Commercial use is not permitted.

