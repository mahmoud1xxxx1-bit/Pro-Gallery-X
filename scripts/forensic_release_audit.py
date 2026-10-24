import urllib.request
import urllib.error
import json
import os
import hashlib
import subprocess

REPO = "mahmoud1xxxx1-bit/Pro-Gallery-X"
HEADERS = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)",
    "Accept": "application/vnd.github.v3+json"
}

def fetch_json(url):
    req = urllib.request.Request(url, headers=HEADERS)
    try:
        with urllib.request.urlopen(req) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        print(f"HTTP Error {e.code} for {url}: {e.read().decode('utf-8', errors='replace')}")
        return None
    except Exception as e:
        print(f"Error fetching {url}: {e}")
        return None

def main():
    print("=== FORENSIC RELEASE AND APK AUDIT ===")
    os.makedirs("forensic_apks", exist_ok=True)
    
    # 1. Fetch releases
    releases_url = f"https://api.github.com/repos/{REPO}/releases"
    releases = fetch_json(releases_url)
    if not releases:
        print("Failed to fetch releases or no releases found.")
        return
    
    print(f"Found {len(releases)} releases on GitHub.")
    release_info = []
    
    for r in releases:
        tag = r.get("tag_name")
        name = r.get("name")
        published_at = r.get("published_at")
        target_commitish = r.get("target_commitish")
        assets = r.get("assets", [])
        print(f"\nRelease: {name} (tag: {tag}, target: {target_commitish}, published: {published_at})")
        
        for asset in assets:
            asset_name = asset.get("name")
            download_url = asset.get("browser_download_url")
            size = asset.get("size")
            print(f"  Asset: {asset_name} ({size} bytes) -> {download_url}")
            
            # Download asset for forensics
            dest_filename = f"forensic_apks/{tag}_{asset_name}"
            if not os.path.exists(dest_filename) or os.path.getsize(dest_filename) != size:
                print(f"    Downloading {asset_name} to {dest_filename}...")
                try:
                    urllib.request.urlretrieve(download_url, dest_filename)
                    print(f"    Downloaded successfully ({os.path.getsize(dest_filename)} bytes).")
                except Exception as e:
                    print(f"    Failed to download: {e}")
            else:
                print(f"    Already downloaded ({os.path.getsize(dest_filename)} bytes).")
                
            # Compute SHA-256
            if os.path.exists(dest_filename):
                with open(dest_filename, "rb") as f:
                    file_sha256 = hashlib.sha256(f.read()).hexdigest()
            else:
                file_sha256 = "N/A"
                
            release_info.append({
                "tag": tag,
                "name": name,
                "target_commitish": target_commitish,
                "published_at": published_at,
                "asset_name": asset_name,
                "local_path": dest_filename,
                "size_bytes": size,
                "file_sha256": file_sha256,
                "download_url": download_url
            })
            
    with open("forensic_apks/releases_metadata.json", "w", encoding="utf-8") as f:
        json.dump(release_info, f, indent=2, ensure_ascii=False)
        
    print("\nReleases metadata saved to forensic_apks/releases_metadata.json")

if __name__ == "__main__":
    main()
