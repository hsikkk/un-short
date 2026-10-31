"""Validate assets, stage canonical Play locales, or verify a committed Play snapshot."""
from pathlib import Path
import argparse, json, struct, hashlib, shutil
ROOT=Path(__file__).resolve().parent
META=ROOT.parents[1]/'metadata/android'
CONTENT=json.loads((ROOT/'content.json').read_text())
FILES=['1_01-main.png','2_02-timer.png','3_03-tasks.png','4_04-limits.png']
FIELDS={'title':30,'short_description':80,'full_description':4000}
def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def validate():
 assert len(CONTENT)==16 and 'ko-KR' not in CONTENT
 assert len({d['playLocale'] for d in CONTENT.values()})==16
 for locale,d in CONTENT.items():
  base=META/locale
  for field,limit in FIELDS.items():
   value=(base/(field+'.txt')).read_text().strip()
   assert value==d[field],(locale,field,'does not match copy source')
   assert 0<len(value)<=limit,(locale,field,len(value))
   assert not any('\uac00'<=c<='\ud7a3' for c in value),(locale,field,'Korean text found')
  actual=sorted(p.name for p in (base/'images/phoneScreenshots').iterdir() if p.suffix.lower() in ['.png','.jpg','.jpeg','.webp'])
  assert actual==FILES,(locale,actual)
  for p in [base/'images/phoneScreenshots'/f for f in FILES]+[base/'images/featureGraphic.png']:
   data=p.read_bytes();assert data[:8]==b'\x89PNG\r\n\x1a\n'
   w,h,depth,color=struct.unpack('>IIBB',data[16:26])
   assert (w,h)==((1024,500) if p.name=='featureGraphic.png' else (1080,1920)),(p,w,h)
   assert (depth,color)==(8,2),(p,depth,color)
   assert len(data)<8*1024*1024
 print('Validated: 16 locales, 48 text fields, 64 screenshots and 16 feature graphics.')
def stage(dest):
 assert not dest.exists() or not any(dest.iterdir()),'Stage must be new or empty'
 for locale,d in CONTENT.items():
  src=META/locale;target=dest/d['playLocale']
  (target/'images/phoneScreenshots').mkdir(parents=True,exist_ok=True)
  for field in FIELDS:shutil.copy2(src/(field+'.txt'),target/(field+'.txt'))
  for file in FILES:shutil.copy2(src/'images/phoneScreenshots'/file,target/'images/phoneScreenshots'/file)
  shutil.copy2(src/'images/featureGraphic.png',target/'images/featureGraphic.png')
 print('Staged 16 locales; Indonesian mapped from in to id.')
def verify(before_file,after_file):
 before=json.loads(before_file.read_text());after=json.loads(after_file.read_text())
 assert set(before['locales'])==set(after['locales']), 'Locale set changed'
 assert before['tracks']==after['tracks'],'Release tracks changed'
 assert before['locales']['ko-KR']==after['locales']['ko-KR'],'Korean listing changed'
 report={'status':'committed and verified','public_visibility':'not confirmed','text_fields':48,'screenshots':64,'feature_graphics':16,'korean_unchanged':True,'release_tracks_unchanged':True,'locales':{}}
 for locale,d in CONTENT.items():
  remote=after['locales'][d['playLocale']];base=META/locale
  for field in FIELDS:assert remote['listing'][field].strip()==d[field],(locale,field)
  hashes=[sha(base/'images/phoneScreenshots'/f) for f in FILES]
  assert [i['sha256'] for i in remote['screenshots']['images']]==hashes,(locale,'screenshot order or content mismatch')
  feature_hash=sha(base/'images/featureGraphic.png')
  assert len(remote['feature']['images'])==1 and remote['feature']['images'][0]['sha256']==feature_hash,(locale,'feature mismatch')
  report['locales'][d['playLocale']]={'screenshots':dict(zip(FILES,hashes)),'featureGraphic':feature_hash}
 (ROOT/'upload-verification.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
 print('Play verified: all 48 text fields and 80 image hashes match, including screenshot order. Korean and release tracks unchanged.')
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--stage',type=Path);p.add_argument('--before',type=Path);p.add_argument('--after',type=Path);args=p.parse_args()
 validate()
 if args.stage:stage(args.stage)
 if args.after:
  assert args.before,'--before is required for remote verification'
  verify(args.before,args.after)
