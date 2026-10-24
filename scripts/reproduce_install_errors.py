import subprocess
import os
import shutil
import time
import json

ADB_DEVICE = "127.0.0.1:5563"
APKSIGNER = r"C:\Users\loved\AppData\Local\Android\Sdk\build-tools\34.0.0\apksigner.bat"
KEYTOOL = r"keytool" # standard Java keytool

def run_cmd(cmd):
    res = subprocess.run(cmd, shell=True, capture_output=True, text=True, encoding="utf-8", errors="replace")
    return res.stdout.strip(), res.stderr.strip(), res.returncode

def adb(args):
    cmd = f'adb -s {ADB_DEVICE} {args}'
    return run_cmd(cmd)

def collect_recent_logcat(keyword="PackageManager"):
    out, _, _ = adb(f'shell "logcat -d | grep -iE \'{keyword}|PackageInstaller|installd\' | tail -n 30"')
    return out

def main():
    print("=== REPRODUCING AND TESTING INSTALLATION SCENARIOS ===")
    os.makedirs("test_keystores", exist_ok=True)
    os.makedirs("test_apks", exist_ok=True)
    
    # Base APK: local built APK (signed with default debug key: c1ea4285c43ccc6da990b93e9a323494609aaa68dbe2d1f500c24c3d6956c1f6)
    base_apk = r"app\build\outputs\apk\debug\app-debug.apk"
    shutil.copy(base_apk, "test_apks/base_app_keyA_vc1.apk")
    
    # 1. Create a DIFFERENT keystore (Key B) simulating a CI runner or previous install with different key
    diff_keystore = "test_keystores/different_debug.keystore"
    if not os.path.exists(diff_keystore):
        cmd = f'keytool -genkeypair -v -keystore {diff_keystore} -alias androiddebugkey -keypass android -storepass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug Runner,O=CI Runner,C=US"'
        out, err, code = run_cmd(cmd)
        print("Generated different keystore:", code)
        
    # Sign an APK with Key B
    apk_key_b = "test_apks/base_app_keyB_vc1.apk"
    shutil.copy(base_apk, apk_key_b)
    sign_cmd = f'"{APKSIGNER}" sign --ks {diff_keystore} --ks-pass pass:android --key-pass pass:android --ks-key-alias androiddebugkey "{apk_key_b}"'
    out, err, code = run_cmd(sign_cmd)
    print("Signed APK with Key B:", code)
    
    # Verify cert of Key B
    verify_cmd = f'"{APKSIGNER}" verify --print-certs "{apk_key_b}"'
    out, err, _ = run_cmd(verify_cmd)
    for l in out.splitlines():
        if "SHA-256" in l:
            print("Key B Cert:", l)

    # Begin Test Matrix
    results = {}
    
    # CASE A: Clean Install
    print("\n--- CASE A: Clean Install ---")
    adb("uninstall com.progalleryx.app")
    adb("shell logcat -c")
    out, err, code = adb(f'install -r "test_apks/base_app_keyA_vc1.apk"')
    log = collect_recent_logcat()
    print("Result:", out, err)
    results["Case_A_Clean_Install"] = {
        "status": "PASS" if "Success" in out else "FAIL",
        "output": out,
        "logcat_snippet": log
    }
    
    # CASE B: Same Key Re-install / Update
    print("\n--- CASE B: Same Key Re-install / Update ---")
    adb("shell logcat -c")
    out, err, code = adb(f'install -r "test_apks/base_app_keyA_vc1.apk"')
    print("Result:", out, err)
    results["Case_B_Same_Key_Update"] = {
        "status": "PASS" if "Success" in out else "FAIL",
        "output": out
    }
    
    # CASE C: Mismatched Signing Key Update (KEY B over KEY A)
    print("\n--- CASE C: Mismatched Signature Update (Key B over Key A) ---")
    adb("shell logcat -c")
    out, err, code = adb(f'install -r "test_apks/base_app_keyB_vc1.apk"')
    log = collect_recent_logcat("signatures do not match|INSTALL_FAILED")
    print("Result:", out, err)
    print("Logcat Snippet:\n", log)
    results["Case_C_Mismatched_Signature"] = {
        "status": "PASS (Correctly Rejected)" if "INSTALL_FAILED_UPDATE_INCOMPATIBLE" in out or "INSTALL_FAILED_UPDATE_INCOMPATIBLE" in err else "UNEXPECTED",
        "output": out or err,
        "logcat_snippet": log
    }
    
    # CASE E: Corrupt / Truncated APK
    print("\n--- CASE E: Corrupt / Incomplete APK ---")
    corrupt_apk = "test_apks/corrupted.apk"
    with open("test_apks/base_app_keyA_vc1.apk", "rb") as f_in:
        content = f_in.read()[:500000] # truncate
    with open(corrupt_apk, "wb") as f_out:
        f_out.write(content)
        
    adb("shell logcat -c")
    out, err, code = adb(f'install -r "{corrupt_apk}"')
    log = collect_recent_logcat("INSTALL_PARSE_FAILED|INSTALL_FAILED")
    print("Result:", out, err)
    results["Case_E_Corrupt_APK"] = {
        "status": "PASS (Correctly Rejected)" if "INSTALL_FAILED" in out or "INSTALL_FAILED" in err or "INSTALL_PARSE_FAILED" in out or "INSTALL_PARSE_FAILED" in err else "UNEXPECTED",
        "output": out or err,
        "logcat_snippet": log
    }
    
    with open("forensic_apks/installation_matrix_reproduction.json", "w", encoding="utf-8") as f:
        json.dump(results, f, indent=2, ensure_ascii=False)
        
    print("\nResults written to forensic_apks/installation_matrix_reproduction.json")

if __name__ == "__main__":
    main()
