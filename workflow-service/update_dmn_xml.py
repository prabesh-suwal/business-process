#!/usr/bin/env python3
"""One-time script to update existing decision tables from DMN 1.2 to DMN 1.3 namespace."""
import requests
import sys

API = "http://localhost:9002/api/dmn"

# Get all tables
tables = requests.get(API).json()
print(f"Found {len(tables)} decision table(s)")

updated = 0
for t in tables:
    tid = t['id']
    detail = requests.get(f"{API}/{tid}").json()
    xml = detail.get('dmnXml', '')
    
    # Check if using old DMN 1.2 namespace
    if 'http://www.omg.org/spec/DMN/20180521' in xml or 'http://www.omg.org/spec/DMN/20191111' in xml:
        new_xml = xml.replace(
            'http://www.omg.org/spec/DMN/20180521', 
            'https://www.omg.org/spec/DMN/20191111'
        ).replace(
            'http://www.omg.org/spec/DMN/20191111',
            'https://www.omg.org/spec/DMN/20191111'
        )
        # Also update namespace if using flowable
        if 'namespace="http://www.flowable.org/dmn"' in new_xml:
            new_xml = new_xml.replace(
                'namespace="http://www.flowable.org/dmn"',
                'namespace="http://camunda.org/schema/1.0/dmn"'
            )
        
        if t['status'] == 'DRAFT':
            resp = requests.put(f"{API}/{tid}", json={"dmnXml": new_xml})
            if resp.ok:
                print(f"  ✓ Updated '{t['name']}' (DRAFT) to DMN 1.3")
                updated += 1
            else:
                print(f"  ✗ Failed to update '{t['name']}': {resp.text}")
        else:
            print(f"  ⚠ Skipped '{t['name']}' ({t['status']}) - only DRAFT tables can be updated")
    else:
        print(f"  - '{t['name']}' already using DMN 1.3 (https)")

print(f"\nDone. Updated {updated} table(s).")
