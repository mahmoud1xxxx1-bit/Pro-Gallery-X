import os
import sys
import time
import subprocess
import json

ADB_DEVICE = "127.0.0.1:5563"

def run_adb(args):
    cmd = ["adb", "-s", ADB_DEVICE] + args
    res = subprocess.run(cmd, capture_output=True, text=True, encoding="utf-8", errors="replace")
    return res.stdout.strip()

def check_logcat_errors():
    out = run_adb(["shell", "logcat", "-d", "*:E"])
    crashes = []
    for line in out.splitlines():
        if any(keyword in line for keyword in ["FATAL EXCEPTION", "AndroidRuntime", "SIGSEGV", "Fatal signal", "ANR in com.progalleryx"]):
            crashes.append(line)
    return crashes

def run_25x_stress_test(total_cycles=25):
    print("==================================================")
    print("STARTING 25x LIVE STUDIO STRESS TEST ON EMULATOR")
    print(f"Device: {ADB_DEVICE} (Android 16 AVD medium_phone)")
    print("==================================================")
    
    results = []
    start_all = time.time()
    
    for cycle in range(1, total_cycles + 1):
        # Clear logcat before each cycle to isolate cycle-specific events
        run_adb(["shell", "logcat", "-c"])
        t_cycle_start = time.time()
        print(f"\n[Cycle {cycle:02d}/{total_cycles:02d}] Beginning navigation loop...")
        
        cycle_result = {
            "cycle": cycle,
            "actions": [],
            "crashes": [],
            "success": True
        }
        
        # 1. Ensure MainActivity is focused and on Studio tab
        run_adb(["shell", "am", "start", "-n", "com.progalleryx.app/.MainActivity"])
        time.sleep(0.8)
        run_adb(["shell", "input", "tap", "900", "2230"]) # Tap Studio Tab
        time.sleep(0.6)
        
        # 2. Transition to PDF Studio
        run_adb(["shell", "input", "swipe", "540", "1600", "540", "1000", "200"])
        time.sleep(0.6)
        run_adb(["shell", "input", "tap", "540", "1250"])
        time.sleep(0.8)
        cycle_result["actions"].append({"target": "PDF Studio", "action": "open", "timestamp": time.time()})
        
        # Press back to Studio Hub
        run_adb(["shell", "input", "keyevent", "KEYCODE_BACK"])
        time.sleep(0.6)
        cycle_result["actions"].append({"target": "PDF Studio", "action": "close", "timestamp": time.time()})
        
        # 3. Transition to Duplicate Scanner
        run_adb(["shell", "input", "swipe", "540", "1600", "540", "1000", "200"])
        time.sleep(0.6)
        run_adb(["shell", "input", "tap", "540", "1900"])
        time.sleep(0.8)
        cycle_result["actions"].append({"target": "Duplicate Scanner", "action": "open", "timestamp": time.time()})
        
        # Press back to Studio Hub
        run_adb(["shell", "input", "keyevent", "KEYCODE_BACK"])
        time.sleep(0.6)
        cycle_result["actions"].append({"target": "Duplicate Scanner", "action": "close", "timestamp": time.time()})
        
        # 4. Open Photo Studio with real photo
        run_adb([
            "shell", "am", "start",
            "-a", "android.intent.action.EDIT",
            "-d", "file:///sdcard/DCIM/Camera/real_test_photo.jpg",
            "-t", "image/jpeg",
            "com.progalleryx.app/.MainActivity"
        ])
        time.sleep(1.0)
        cycle_result["actions"].append({"target": "Photo Studio (Real JPEG)", "action": "open", "timestamp": time.time()})
        
        # Press back to close Photo Studio
        run_adb(["shell", "input", "keyevent", "KEYCODE_BACK"])
        time.sleep(0.6)
        cycle_result["actions"].append({"target": "Photo Studio", "action": "close", "timestamp": time.time()})
        
        # 5. Open Video Studio with real video
        run_adb([
            "shell", "am", "start",
            "-a", "android.intent.action.EDIT",
            "-d", "file:///sdcard/DCIM/Camera/real_test_video.mp4",
            "-t", "video/mp4",
            "com.progalleryx.app/.MainActivity"
        ])
        time.sleep(1.0)
        cycle_result["actions"].append({"target": "Video Studio (Real MP4)", "action": "open", "timestamp": time.time()})
        
        # Press back to close Video Studio
        run_adb(["shell", "input", "keyevent", "KEYCODE_BACK"])
        time.sleep(0.6)
        cycle_result["actions"].append({"target": "Video Studio", "action": "close", "timestamp": time.time()})
        
        # Check crashes in this cycle
        crashes = check_logcat_errors()
        cycle_duration = round(time.time() - t_cycle_start, 2)
        cycle_result["duration_seconds"] = cycle_duration
        
        if crashes:
            cycle_result["success"] = False
            cycle_result["crashes"] = crashes
            print(f"  --> Cycle {cycle:02d}: ISSUE DETECTED! ({len(crashes)} errors)")
        else:
            print(f"  --> Cycle {cycle:02d}: PASSED in {cycle_duration}s (0 crashes)")
            
        results.append(cycle_result)
        
    total_duration = round(time.time() - start_all, 2)
    passed_count = sum(1 for r in results if r["success"])
    total_crashes = sum(len(r["crashes"]) for r in results)
    
    print("\n" + "=" * 50)
    print("LIVE 25x STRESS TEST SUMMARY")
    print(f"Total Duration: {total_duration} seconds")
    print(f"Total Cycles: {total_cycles}")
    print(f"Passed Cycles: {passed_count}/{total_cycles} ({(passed_count/total_cycles)*100:.1f}%)")
    print(f"Total Fatal Crashes / ANRs: {total_crashes}")
    print("=" * 50)
    
    os.makedirs("docs", exist_ok=True)
    with open("docs/studio_stress_25x_real_results.json", "w", encoding="utf-8") as f:
        json.dump({
            "test": "Studio 25x Navigation Stress Test",
            "device": ADB_DEVICE,
            "total_cycles": total_cycles,
            "passed_cycles": passed_count,
            "total_crashes": total_crashes,
            "total_duration_seconds": total_duration,
            "cycles": results
        }, f, indent=2, ensure_ascii=False)
        
    return results

if __name__ == "__main__":
    cycles = int(sys.argv[1]) if len(sys.argv) > 1 else 25
    run_25x_stress_test(cycles)
