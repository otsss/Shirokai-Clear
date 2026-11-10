#!/usr/bin/env python3
"""Prepare a disposable, pinned production Fabric server. Never targets an existing live server."""
import hashlib, json, os, pathlib, shutil, subprocess, urllib.parse, zipfile
repo=pathlib.Path(__file__).resolve().parents[1]
root=repo/'run-production'
root.mkdir(exist_ok=True)
cache=pathlib.Path(os.environ.get('GRADLE_USER_HOME',str(pathlib.Path.home()/'.gradle')))/'caches'

def fetch(url,path,expected=None):
    subprocess.run(['curl','-fsSL','--retry','2','--max-time','120',url,'-o',str(path)],check=True)
    if expected:
        assert hashlib.sha256(path.read_bytes()).hexdigest()==expected, f'Checksum mismatch: {path.name}'

def verified(url,path):
    checksum=path.with_suffix(path.suffix+'.sha256')
    fetch(url+'.sha256',checksum)
    fetch(url,path,checksum.read_text().split()[0])

verified('https://maven.fabricmc.net/net/fabricmc/fabric-installer/1.1.0/fabric-installer-1.1.0.jar',root/'fabric-installer.jar')
verified('https://maven.fabricmc.net/net/fabricmc/fabric-loader/0.18.3/fabric-loader-0.18.3.jar',root/'fabric-loader.jar')
java=str(pathlib.Path(os.environ['JAVA_HOME'])/'bin/java')
options=[]
proxy=urllib.parse.urlparse(os.environ.get('HTTPS_PROXY',''))
if proxy.hostname:
    options += [f'-Dhttps.proxyHost={proxy.hostname}',f'-Dhttps.proxyPort={proxy.port or 80}',f'-Dhttp.proxyHost={proxy.hostname}',f'-Dhttp.proxyPort={proxy.port or 80}']
if pathlib.Path('/etc/ssl/certs/java/cacerts').exists():
    options += ['-Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts']
subprocess.run([java,*options,'--class-path',str(root/'fabric-installer.jar'),str(repo/'scripts/InstallPinnedFabric.java'),str(root),str(root/'fabric-loader.jar')],check=True)
with zipfile.ZipFile(root/'fabric-loader.jar') as z: libraries=json.loads(z.read('fabric-installer.json'))['libraries']
for entry in libraries['common']+libraries['server']:
    group,name,version=entry['name'].split(':')
    path=root/'libraries'/group.replace('.','/')/name/version/f'{name}-{version}.jar'
    assert hashlib.sha256(path.read_bytes()).hexdigest()==entry['sha256'],f'Checksum mismatch: {path}'
p=root/'libraries/net/fabricmc/intermediary/1.21.1/intermediary-1.21.1.jar'
fetch('https://maven.fabricmc.net/net/fabricmc/intermediary/1.21.1/intermediary-1.21.1.jar.sha256',root/'intermediary.sha256')
assert hashlib.sha256(p.read_bytes()).hexdigest()==(root/'intermediary.sha256').read_text().split()[0]
mc=cache/'fabric-loom/1.21.1'
meta=json.loads((mc/'mojang_minecraft_info.json').read_text())
assert hashlib.sha1((mc/'minecraft-server.jar').read_bytes()).hexdigest()==meta['downloads']['server']['sha1']
shutil.copy2(mc/'minecraft-server.jar',root/'server.jar')
mods=root/'mods';mods.mkdir(exist_ok=True)
for group,artifact,version in [('com.cobblemon','fabric','1.8.3+1.21.1'),('net.fabricmc.fabric-api','fabric-api','0.116.7+1.21.1'),('net.fabricmc','fabric-language-kotlin','1.13.6+kotlin.2.2.20')]:
    jars=[p for p in (cache/'modules-2/files-2.1'/group/artifact/version).glob('*/*.jar') if not p.name.endswith('-sources.jar')]
    assert len(jars)==1,(artifact,jars)
    shutil.copy2(jars[0],mods/jars[0].name)
easy=pathlib.Path(os.environ['EASY_NPC_JAR'])
assert hashlib.sha256(easy.read_bytes()).hexdigest()=='16a10cf0d4f651bf3ed0db15883814643544a00720e27f22e352c2693a2e4f04'
shutil.copy2(easy,mods/easy.name)
for path in [repo/'build/libs/Shirokai-Clear-1.0.0.jar',repo/'build/integration/shirokaiclear-integration.jar']:
    shutil.copy2(path,mods/path.name)
for name,content in [('server.properties','online-mode=false\nserver-port=25575\nlevel-type=minecraft:flat\ngenerate-structures=false\nspawn-protection=0\nview-distance=2\nsimulation-distance=2\nmax-tick-time=120000\n'),('eula.txt','eula=true\n')]:
    path=root/name
    if not path.exists():path.write_text(content)
print('Pinned production test server prepared:',root)
