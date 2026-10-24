import subprocess
import time
import json
import xml.etree.ElementTree as ET
import os
import sys

ADB_DEVICE = "127.0.0.1:5563"

def adb(cmd, timeout=30):
    full_cmd = f"adb connect {ADB_DEVICE} >nul 2>&1 & adb -s {ADB_DEVICE} {cmd}"
    result = subprocess.run(full_cmd, shell=True, capture_output=True, text=True, timeout=timeout)
    return result.stdout.strip()

def dump_ui_texts():
    xml_path = "/sdcard/temp_dump.xml"
    adb(f"shell uiautomator dump {xml_path}")
    raw_xml = adb(f"shell cat {xml_path}")
    texts = []
    if "<hierarchy" in raw_xml:
        try:
            root = ET.fromstring(raw_xml)
            for node in root.iter("node"):
                t = node.attrib.get("text", "")
                if t:
                    texts.append(t)
        except Exception as e:
            pass
    return texts

def check_logcat_crashes():
    out = adb("shell \"logcat -d *:E | grep -i 'AndroidRuntime\\|FATAL\\|SIGSEGV' | tail -n 20\"")
    lines = [l for l in out.splitlines() if "com.progalleryx" in l or "FATAL EXCEPTION" in l]
    return lines

def run_stress_test(cycles=25):
    print(f"=== Starting Studio 25x Navigation Stress Test on {ADB_DEVICE} ===")
    results = []
    
    # Clear logcat first
    adb("shell logcat -c")
    
    for cycle in range(1, cycles + 1):
        t0 = time.time()
        print(f"\n--- Cycle {cycle}/{cycles} ---")
        cycle_data = {
            "cycle": cycle,
            "steps": [],
            "crashes": [],
            "passed": True
        }
        
        # Step 1: Open PDF Studio
        adb("shell am start -n com.progalleryx.app/.MainActivity")
        time.sleep(0.5)
        # Tap Studio tab
        adb("shell input tap 900 2230")
        time.sleep(0.6)
        # Scroll down to see PDF & Duplicate cards
        adb("shell input swipe 540 1800 540 800 200")
        time.sleep(0.5)
        
        # Tap "Open PDF Tools" (approx center: 540, 1600 or tap card)
        # Let's tap Open PDF Tools button
        adb("shell input tap 540 1350")
        time.sleep(0.8)
        pdf_texts = dump_ui_texts()
        has_pdf = any("PDF" in t for t in pdf_texts)
        cycle_data["steps"].append({"step": "Open PDF Studio", "verified": has_pdf})
        print(f"  [Cycle {cycle}] PDF Studio verified: {has_pdf}")
        
        # Return to Studio Hub
        adb("shell input keyevent KEYCODE_BACK")
        time.sleep(0.5)
        
        # Step 2: Open Duplicate Scanner
        adb("shell input tap 540 1950")
        time.sleep(0.8)
        dup_texts = dump_ui_texts()
        has_dup = any("Duplicate" in t or "فحص" in t or "Scanner" in t for t in dup_texts)
        cycle_data["steps"].append({"step": "Open Duplicate Scanner", "verified": has_dup})
        print(f"  [Cycle {cycle}] Duplicate Scanner verified: {has_dup}")
        
        # Return to Studio Hub
        adb("shell input keyevent KEYCODE_BACK")
        time.sleep(0.5)
        
        # Step 3: Open Photo Studio with real photo
        adb("shell am start -a android.intent.action.EDIT -d file:///sdcard/DCIM/Camera/real_test_photo.jpg -t image/jpeg com.progalleryx.app/.MainActivity")
        time.sleep(1.0)
        photo_texts = dump_ui_texts()
        has_photo = any("استوديو الصور" in t or "Photo Studio" in t or "الفلاتر" in t or "الضبط" in t for t in photo_texts)
        cycle_data["steps"].append({"step": "Open Photo Studio", "verified": has_photo})
        print(f"  [Cycle {cycle}] Photo Studio verified: {has_photo}")
        
        # Close Photo Studio
        adb("shell input keyevent KEYCODE_BACK")
        time.sleep(0.5)
        
        # Step 4: Open Video Studio with real video
        adb("shell am start -a android.intent.action.EDIT -d file:///sdcard/DCIM/Camera/real_test_video.mp4 -t video/mp4 com.progalleryx.app/.MainActivity")
        time.sleep(1.2)
        video_texts = dump_ui_texts()
        has_video = any("Video Studio" in t or "00:05.00" in t or "Trim" in t for t in video_texts)
        cycle_data["steps"].append({"step": "Open Video Studio", "verified": has_video})
        print(f"  [Cycle {cycle}] Video Studio verified: {has_video}")
        
        # Close Video Studio
        adb("shell input keyevent KEYCODE_BACK")
        time.sleep(0.5)
        
        # Check crashes in this cycle
        crashes = check_logcat_crashes()
        cycle_data["crashes"] = crashes
        if crashes:
            cycle_data["passed"] = False
            print(f"  [ERROR] Crash detected in cycle {cycle}: {crashes}")
        
        duration = time.time() - t0
        cycle_data["duration_seconds"] = round(duration, 2)
        print(f"  [Cycle {cycle}] Completed in {duration:.2f}s - Status: {'PASSED' if cycle_data['passed'] else 'FAILED'}")
        results.append(cycle_data)
        
    return results

if __name__ == "__main__":
    count = int(sys.argv[1]) if len(sys.argv) > 1 else 25
    results = run_stress_test(count)
    with open("docs/studio_stress_test_results.json", "w", encoding="utf-8") as f:
        json.dump(results, f, indent=2, ensure_ascii=False)
    
    total = len(results)
    passed = sum(1 for r in results if r["passed"])
    crashes = sum(len(r["crashes"]) for r in results)
    print("\n" + "=" * 50)
    print(f"TOTAL CYCLES: {total}")
    print(f"PASSED CYCLES: {passed} / {total} ({(passed/total)*100:.1f}%)")
    print(f"CRASHES OBSERVED: {crashes}")
    print("=" * 50)
