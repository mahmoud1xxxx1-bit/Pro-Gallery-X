import urllib.request
import json

REPO = "mahmoud1xxxx1-bit/Pro-Gallery-X"
HEADERS = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)",
    "Accept": "application/vnd.github.v3+json"
}

def main():
    url = f"https://api.github.com/repos/{REPO}/actions/runs?per_page=15"
    req = urllib.request.Request(url, headers=HEADERS)
    with urllib.request.urlopen(req) as resp:
        data = json.loads(resp.read().decode("utf-8"))
        
    runs = data.get("workflow_runs", [])
    print(f"Total CI Runs found: {len(runs)}")
    for r in runs:
        print(f"ID: {r.get('id')} | Event: {r.get('event')} | Branch: {r.get('head_branch')} | SHA: {r.get('head_sha')[:7]} | Status: {r.get('status')} | Conclusion: {r.get('conclusion')} | Created: {r.get('created_at')}")

if __name__ == "__main__":
    main()
