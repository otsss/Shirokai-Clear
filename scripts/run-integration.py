#!/usr/bin/env python3
"""Execute the isolated production fixture and reject absent/stale/failed results."""
import json, os, pathlib, subprocess, sys
repo=pathlib.Path(__file__).resolve().parents[1]
root=repo/'run-production'
result=root/'integration-result.json'
result.unlink(missing_ok=True)
java=str(pathlib.Path(os.environ['JAVA_HOME'])/'bin/java')
try:
    process=subprocess.run([java,'-Xms512M','-Xmx3G','-Dshirokaiclear.integration=true','-jar','fabric-server-launch.jar','nogui'],cwd=root,timeout=240)
except subprocess.TimeoutExpired:
    sys.exit('Integration server timed out')
if process.returncode: sys.exit(process.returncode)
if not result.exists(): sys.exit('Server exited without a current integration result')
data=json.loads(result.read_text())
if data.get('success') is not True: sys.exit('Integration failed: '+str(data.get('error')))
passed=data.get('passed',[])
if not all(any(name.startswith(f'{i:02d} ') for name in passed) for i in range(1,19)):
    sys.exit('Required scenarios did not all execute')
print(f'Integration verified: {len(passed)} checks passed, required scenarios 01–18 present')
