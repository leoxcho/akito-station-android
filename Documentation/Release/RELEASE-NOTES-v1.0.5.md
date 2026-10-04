# Android 1.0.5 · build 6

Fix logo switching in optimized release builds: launcher alias names now use the manifest namespace, independent of R8 class relocation. The previous build could report `r4.LogoAURORA does not exist`. Release-optimizer UI regression coverage uses an isolated test installation.
