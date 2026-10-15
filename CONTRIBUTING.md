# 🤝 Contributing to Kingston Campus 3D

Thank you for your interest in contributing to **Kingston Campus 3D**! We welcome all contributions — whether it's fixing bugs, adding new campus buildings, improving vehicle dynamics, optimizing physics performance, or polishing gameplay animations.

---

## 📋 Code of Conduct

By participating in this project, you agree to abide by our [Code of Conduct](CODE_OF_CONDUCT.md). Please treat all contributors and community members with respect and courtesy.

---

## 🚀 How to Contribute

### 1. Fork & Clone
1. Fork the repository on GitHub.
2. Clone your fork locally with Git LFS enabled:
   ```bash
   git clone https://github.com/YOUR_USERNAME/Kingston-Campus-3D-Game.git
   cd Kingston-Campus-3D-Game
   git lfs pull
   ```

### 2. Create a Feature Branch
Always create a descriptive branch for your work:
```bash
git checkout -b feature/new-campus-building
# or
git checkout -b bugfix/vehicle-collision
```

### 3. Local Development & Testing
Make sure you have **Java 21 LTS** installed.
- **Run the game locally:**
  ```powershell
  # Windows:
  .\gradlew.bat run

  # Linux / macOS:
  ./gradlew run
  ```
- **Compile and verify syntax:**
  ```powershell
  .\gradlew.bat compileJava
  ```

### 4. Commit Guidelines
Write clear, conventional commit messages:
- `feat: add student library interior navigation`
- `fix: prevent golf cart clipping through curbs`
- `perf: optimize NPC distance culling algorithm`
- `docs: update controls table in README`

### 5. Submit a Pull Request
1. Push your branch to your GitHub fork:
   ```bash
   git push origin feature/your-feature-name
   ```
2. Open a **Pull Request (PR)** against the `master` branch.
3. Fill out the PR template describing your changes, screenshots (if visual), and testing steps.

---

## 🛠️ Development Standards

- **Java Version:** Strictly **Java 21** (using modern features where beneficial).
- **Engine Rules:**
  - Preserve `setThreadingType(SEQUENTIAL)` on BulletAppState to avoid multi-threaded physics race conditions.
  - All scene graph modifications (`rootNode.attachChild`, etc.) must run on the main render thread.
  - Scale transforms must be strictly positive (`scale > 0`) before generating Bullet collision shapes.
- **Asset Guidelines:**
  - Proprietary 3D models must be encrypted using `AssetProtector` into `.pak` format before distribution.
  - Texture dimensions should be powers of two (512x512, 1024x1024, 2048x2048) for optimal GPU mipmapping.

---

## ❓ Need Help?
Have questions or ideas? Feel free to open a [Discussion](https://github.com/kamalesh404/Kingston-Campus-3D-Game/discussions) or submit an [Issue](https://github.com/kamalesh404/Kingston-Campus-3D-Game/issues).
