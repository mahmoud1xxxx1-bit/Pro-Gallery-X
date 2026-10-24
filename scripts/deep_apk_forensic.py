import subprocess
import os
import glob
import json
import hashlib

APKSIGNER = r"C:\Users\loved\AppData\Local\Android\Sdk\build-tools\34.0.0\apksigner.bat"
AAPT = r"C:\Users\loved\AppData\Local\Android\Sdk\build-tools\35.0.0\aapt.exe"

def run_cmd(cmd):
    res = subprocess.run(cmd, shell=True, capture_output=True, text=True, encoding="utf-8", errors="replace")
    return res.stdout, res.stderr, res.returncode

def analyze_apk(apk_path):
    print(f"\n==========================================")
    print(f"ANALYZING: {apk_path}")
    print(f"==========================================")
    
    # 1. File size and SHA256
    size = os.path.getsize(apk_path)
    with open(apk_path, "rb") as f:
        sha256 = hashlib.sha256(f.read()).hexdigest()
    print(f"Size: {size:,} bytes")
    print(f"SHA-256: {sha256}")
    
    # 2. AAPT badging (versionCode, versionName, package)
    aapt_cmd = f'"{AAPT}" dump badging "{apk_path}"'
    out_aapt, err_aapt, code_aapt = run_cmd(aapt_cmd)
    
    package_name = "UNKNOWN"
    version_code = "UNKNOWN"
    version_name = "UNKNOWN"
    sdk_version = "UNKNOWN"
    target_sdk = "UNKNOWN"
    
    for line in out_aapt.splitlines():
        if line.startswith("package:"):
            # package: name='com.progalleryx.app' versionCode='1' versionName='0.1.0'
            parts = line.split()
            for p in parts:
                if p.startswith("name='"):
                    package_name = p.split("'")[1]
                elif p.startswith("versionCode='"):
                    version_code = p.split("'")[1]
                elif p.startswith("versionName='"):
                    version_name = p.split("'")[1]
        elif line.startswith("sdkVersion:'"):
            sdk_version = line.split("'")[1]
        elif line.startswith("targetSdkVersion:'"):
            target_sdk = line.split("'")[1]
            
    print(f"Package: {package_name}")
    print(f"VersionCode: {version_code}")
    print(f"VersionName: {version_name}")
    print(f"MinSdk: {sdk_version} | TargetSdk: {target_sdk}")
    
    # 3. APK Signer verification
    signer_cmd = f'"{APKSIGNER}" verify --verbose --print-certs "{apk_path}"'
    out_signer, err_signer, code_signer = run_cmd(signer_cmd)
    
    schemes = []
    certs = []
    cur_cert = {}
    
    for line in out_signer.splitlines():
        line_s = line.strip()
        if "Verified using v1 scheme (JAR signing):" in line:
            schemes.append(line_s)
        elif "Verified using v2 scheme (APK Signature Scheme v2):" in line:
            schemes.append(line_s)
        elif "Verified using v3 scheme (APK Signature Scheme v3):" in line:
            schemes.append(line_s)
        elif line_s.startswith("Signer #"):
            if cur_cert:
                certs.append(cur_cert)
            cur_cert = {"signer": line_s}
        elif "Signer certificate DN:" in line_s:
            cur_cert["dn"] = line_s.split(":", 1)[1].strip()
        elif "Signer certificate SHA-256 digest:" in line_s:
            cur_cert["sha256"] = line_s.split(":", 1)[1].strip()
        elif "Signer certificate SHA-1 digest:" in line_s:
            cur_cert["sha1"] = line_s.split(":", 1)[1].strip()
            
    if cur_cert:
        certs.append(cur_cert)
        
    print(f"Signing Schemes: {', '.join(schemes)}")
    for i, c in enumerate(certs, 1):
        print(f"  Cert #{i}: DN={c.get('dn')}")
        print(f"  Cert #{i} SHA-256 Fingerprint: {c.get('sha256')}")
        
    return {
        "path": apk_path,
        "filename": os.path.basename(apk_path),
        "size_bytes": size,
        "sha256": sha256,
        "package_name": package_name,
        "version_code": version_code,
        "version_name": version_name,
        "sdk_version": sdk_version,
        "target_sdk": target_sdk,
        "schemes": schemes,
        "certs": certs,
        "apksigner_raw": out_signer
    }

def main():
    apks = glob.glob("forensic_apks/*.apk")
    local_apk = r"app\build\outputs\apk\debug\app-debug.apk"
    if os.path.exists(local_apk):
        apks.append(local_apk)
        
    results = []
    for apk in apks:
        res = analyze_apk(apk)
        results.append(res)
        
    with open("forensic_apks/detailed_apk_forensic.json", "w", encoding="utf-8") as f:
        json.dump(results, f, indent=2, ensure_ascii=False)
        
    print("\nDetailed audit saved to forensic_apks/detailed_apk_forensic.json")

if __name__ == "__main__":
    main()
