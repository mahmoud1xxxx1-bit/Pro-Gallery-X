import sys
import xml.etree.ElementTree as ET

sys.stdout.reconfigure(encoding='utf-8')
tree = ET.parse('ui_about_ar.xml')
for e in tree.iter():
    bounds = e.attrib.get('bounds', '')
    desc = e.attrib.get('content-desc', '')
    text = e.attrib.get('text', '')
    if desc or text:
        print(f"text='{text}' | desc='{desc}' | bounds={bounds}")
