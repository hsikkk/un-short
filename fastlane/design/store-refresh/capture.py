"""Capture localized, real UI on the prepared emulator. Temporary locale helper required."""
from pathlib import Path
import subprocess, json, time, xml.etree.ElementTree as ET, re
ROOT=Path(__file__).resolve().parent
ADB=['adb','-s','emulator-5556']; PKG='com.muuu.unshort.debug'
TASKS={
'en-US':['Read 10 pages','Study for 20 minutes','Stretch a little'],
'ja-JP':['本を10ページ読む','英語を20分勉強する','ストレッチをする'],
'zh-CN':['阅读10页','学习英语20分钟','做拉伸运动'],
'zh-TW':['閱讀10頁','學習英文20分鐘','做伸展運動'],
'es-ES':['Leer 10 páginas','Estudiar 20 minutos','Hacer estiramientos'],
'fr-FR':['Lire 10 pages','Étudier 20 minutes',"Faire des étirements"],
'de-DE':['10 Seiten lesen','20 Minuten lernen','Dehnübungen machen'],
'pt-BR':['Ler 10 páginas','Estudar por 20 minutos','Fazer alongamentos'],
'ru-RU':['Прочитать 10 страниц','Учиться 20 минут','Сделать растяжку'],
'it-IT':['Leggere 10 pagine','Studiare per 20 minuti','Fare stretching'],
'hi-IN':['10 पन्ने पढ़ें','20 मिनट पढ़ाई करें','थोड़ी स्ट्रेचिंग करें'],
'ar':['قراءة 10 صفحات','الدراسة لمدة 20 دقيقة','تمارين التمدد'],
' th'.strip():['อ่านหนังสือ 10 หน้า','เรียน 20 นาที','ยืดเส้นยืดสาย'],
' tr-TR'.strip():['10 sayfa kitap oku','20 dakika ders çalış','Esneme hareketleri yap'],
'vi':['Đọc 10 trang sách','Học trong 20 phút','Tập giãn cơ'],
'in':['Baca 10 halaman','Belajar selama 20 menit','Lakukan peregangan']}
def adb(*args,raw=False):
 r=subprocess.run(ADB+list(args),check=True,capture_output=True)
 return r.stdout if raw else r.stdout.decode().strip()
def shell(*args): return adb('shell',*args)
def start(activity):
 shell('run-as',PKG,'am','start','--user','0','-f','0x04000000','-n',PKG+'/com.muuu.unshort.'+activity)
 time.sleep(.7)
def dump():
 shell('uiautomator','dump','/sdcard/store-locales.xml')
 return ET.fromstring(adb('exec-out','cat','/sdcard/store-locales.xml'))
def capture(folder,name):
 expected={'main':'toggleContainer','timer':'timerNumber','tasks':'taskLockSummary','limits':'dailyLimitItem'}[name]
 for attempt in range(5):
  tree=dump()
  if any(n.get('resource-id','').endswith(':id/'+expected) for n in tree.iter('node')): break
  time.sleep(1)
 else: raise RuntimeError('Screen not ready: '+str(folder)+' '+name)
 packages={n.get('package') for n in tree.iter('node')}
 assert PKG in packages,(name,packages)
 (folder/(name+'.png')).write_bytes(adb('exec-out','screencap','-p',raw=True))
 return tree
backup=Path('/tmp/unshort-store-locales/task-prefs-backup.xml')
r=subprocess.run(ADB+['exec-out','run-as',PKG,'cat','shared_prefs/task_lock_preferences.xml'],capture_output=True)
if r.returncode==0 and not backup.exists(): backup.write_bytes(r.stdout)
LOCALES={'en-US':'values','ja-JP':'values-ja','zh-CN':'values-zh-rCN','zh-TW':'values-zh-rTW','es-ES':'values-es','fr-FR':'values-fr','de-DE':'values-de','pt-BR':'values-pt','ru-RU':'values-ru','it-IT':'values-it','hi-IN':'values-hi','ar':'values-ar','th':'values-th','tr-TR':'values-tr','vi':'values-vi','in':'values-in'}
for locale,titles in TASKS.items():
 folder=ROOT/'sources'/locale
 folder.mkdir(parents=True,exist_ok=True)
 if all((folder/(name+'.png')).exists() for name in ['main','timer','tasks','limits']):
  print(locale,'already captured',flush=True); continue
 shell('am','force-stop',PKG)
 shell('am','start','-n','dev.unshort.capturelocale/.LocaleActivity','--es','locale','id-ID' if locale=='in' else locale)
 time.sleep(1)
 day=shell('date','+%Y-%m-%d')
 tasks=[dict(id='store-demo-'+str(i),title=t,isLocking=True,isCompleted=False,date=day,repeatMode='daily',verificationMode='direct') for i,t in enumerate(titles)]
 root=ET.Element('map'); ET.SubElement(root,'string',name='tasks').text=json.dumps(tasks,ensure_ascii=False)
 subprocess.run(ADB+['shell','run-as',PKG,'tee','shared_prefs/task_lock_preferences.xml'],input=ET.tostring(root,encoding='utf-8',xml_declaration=True),stdout=subprocess.DEVNULL,check=True)
 shell('appops','set',PKG,'SYSTEM_ALERT_WINDOW','allow')
 shell('settings','delete','secure','enabled_accessibility_services')
 shell('settings','put','secure','enabled_accessibility_services',PKG+'/com.muuu.unshort.ShortsBlockService')
 shell('settings','put','secure','accessibility_enabled','1')
 time.sleep(1)
 start('ui.activity.MainActivity')
 tree=capture(folder,'main')
 resources=ET.parse(ROOT.parents[2]/'app/src/main/res'/LOCALES[locale]/'strings.xml')
 # Verify that the UI, not just the sample tasks, uses the intended locale.
 expected_status=next(n.text for n in resources.iter('string') if n.get('name')=='status_active')
 actual_status=next(n.get('text') for n in tree.iter('node') if n.get('resource-id','').endswith(':id/statusLabel'))
 assert actual_status==expected_status,(locale,expected_status,actual_status)
 assert any(n.get('resource-id','').endswith(':id/toggleContainer') for n in tree.iter('node')), 'Main is not active: '+locale
 start('timer.ShortsBlockTimerActivity'); capture(folder,'timer')
 start('ui.activity.TaskLockActivity'); tree=capture(folder,'tasks')
 assert any(n.get('text')==titles[0] for n in tree.iter('node')), 'Missing localized sample tasks: '+locale
 start('ui.activity.SettingsActivity')
 tree=dump()
 # Scroll the actual settings list until the sleep-mode row sits near the top.
 node=next(n for n in tree.iter('node') if n.get('resource-id','').endswith(':id/sleepModeItem'))
 _,y1,_,y2=map(int,re.findall(r'\d+',node.get('bounds')))
 delta=max(0,min(1200,(y1+y2)//2-650))
 if delta>40: shell('input','swipe','540','1750','540',str(1750-delta),'1200')
 tree=capture(folder,'limits')
 assert any(n.get('resource-id','').endswith(':id/dailyLimitItem') for n in tree.iter('node')), 'Missing daily limit: '+locale
 print(locale,'4 screens captured',flush=True)
print('All 16 locales captured.',flush=True)
