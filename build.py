"""Build and sign the offline Android APK with local Android SDK command-line tools."""

from pathlib import Path
import secrets
import shutil
import subprocess
import sys
import zipfile

ROOT = Path(__file__).resolve().parent
TOOLCHAIN = ROOT / ".toolchain"
JDK = TOOLCHAIN / "jdk-17.0.20.1+1" / "bin"
TOOLS = TOOLCHAIN / "android-37.0"
ANDROID_JAR = TOOLCHAIN / "android.jar"
BUILD = ROOT / "build"
DIST = ROOT / "dist"
PACKAGE = DIST / "OfflineProfiles-1.1.0-android16.apk"


def run(*args):
    print("Running:", Path(str(args[0])).name, "...", flush=True)
    subprocess.run([str(a) for a in args], cwd=ROOT, check=True)


def main():
    for item in [JDK / "javac.exe", JDK / "java.exe", JDK / "keytool.exe",
                 TOOLS / "aapt2.exe", TOOLS / "zipalign.exe", ANDROID_JAR,
                 TOOLS / "lib" / "d8.jar", TOOLS / "lib" / "apksigner.jar"]:
        if not item.exists():
            raise SystemExit(f"Missing build tool: {item}")

    BUILD.mkdir(exist_ok=True)
    DIST.mkdir(exist_ok=True)
    classes = BUILD / "classes"
    dex = BUILD / "dex"
    for target in (classes, dex):
        if target.resolve().parent != BUILD.resolve():
            raise SystemExit(f"Unexpected build directory: {target}")
    shutil.rmtree(classes, ignore_errors=True)
    shutil.rmtree(dex, ignore_errors=True)
    classes.mkdir()
    dex.mkdir()

    sources = sorted((ROOT / "app" / "src").rglob("*.java"))
    run(JDK / "javac.exe", "-encoding", "UTF-8", "-source", "8", "-target", "8",
        "-classpath", ANDROID_JAR, "-d", classes, *sources)
    for name in ("MainActivity", "ScheduleManager", "TemporaryManager", "ScheduleReceiver"):
        if not (classes / "com" / "walid" / "offlineprofiles" / f"{name}.class").exists():
            raise SystemExit(f"Java compilation omitted {name}.class")

    classes_jar = BUILD / "classes.jar"
    with zipfile.ZipFile(classes_jar, "w", zipfile.ZIP_DEFLATED) as out:
        for path in classes.rglob("*.class"):
            out.write(path, path.relative_to(classes).as_posix())
    run(JDK / "java.exe", "-cp", TOOLS / "lib" / "d8.jar",
        "com.android.tools.r8.D8", "--min-api", "26", "--lib", ANDROID_JAR,
        "--output", dex, classes_jar)

    base = BUILD / "base.ap_"
    resources = BUILD / "resources.zip"
    run(TOOLS / "aapt2.exe", "compile", "--dir", ROOT / "app" / "res",
        "-o", resources)
    run(TOOLS / "aapt2.exe", "link", "-o", base, "-I", ANDROID_JAR,
        "--manifest", ROOT / "app" / "AndroidManifest.xml",
        "--min-sdk-version", "26", "--target-sdk-version", "36", resources)

    unsigned = BUILD / "unsigned.apk"
    with zipfile.ZipFile(base) as source, zipfile.ZipFile(unsigned, "w") as out:
        for member in source.infolist():
            out.writestr(member, source.read(member.filename))
        out.write(dex / "classes.dex", "classes.dex", compress_type=zipfile.ZIP_DEFLATED)

    aligned = BUILD / "aligned.apk"
    run(TOOLS / "zipalign.exe", "-f", "-p", "4", unsigned, aligned)

    key = TOOLCHAIN / "offlineprofiles-signing.p12"
    password_file = TOOLCHAIN / "offlineprofiles-signing-password.txt"
    if not key.exists():
        password = secrets.token_urlsafe(32)
        password_file.write_text(password + "\n", encoding="ascii")
        run(JDK / "keytool.exe", "-genkeypair", "-alias", "offlineprofiles",
            "-keyalg", "RSA", "-keysize", "3072", "-validity", "10000",
            "-dname", "CN=Offline Profiles", "-keystore", key,
            "-storetype", "PKCS12", "-storepass", password,
            "-keypass", password, "-noprompt")
    if not password_file.exists():
        raise SystemExit("Signing password file missing; cannot use existing key")
    run(JDK / "java.exe", "-jar", TOOLS / "lib" / "apksigner.jar", "sign",
        "--ks", key, "--ks-key-alias", "offlineprofiles",
        "--ks-pass", f"file:{password_file}",
        "--out", PACKAGE, aligned)
    run(JDK / "java.exe", "-jar", TOOLS / "lib" / "apksigner.jar", "verify",
        "--verbose", "--print-certs", PACKAGE)
    print("APK:", PACKAGE)


if __name__ == "__main__":
    try:
        main()
    except subprocess.CalledProcessError as exc:
        sys.exit(exc.returncode)
