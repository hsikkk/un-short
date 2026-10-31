(() => {
  const views = ['block', 'timer', 'choice', 'done'];
  const $ = id => document.getElementById(id);
  let remaining = 30, flipped = false, interval = null, current = 'block';
  function stop() { if (interval) clearInterval(interval); interval = null; }
  function caption(message) { $('demo-caption').textContent = message; $('demo-status').textContent = message; }
  function show(view) {
    stop(); current = view;
    views.forEach(name => { $('demo-' + name).hidden = name !== view; });
  }
  function update() {
    $('timer-value').textContent = remaining;
    $('timer-progress').style.strokeDashoffset = (590.62 * (1 - remaining / 30)).toFixed(2);
    $('flip-label').textContent = flipped ? '다시 들기 · 일시정지' : '뒤집기 체험';
    $('demo-flip').classList.toggle('flipped', flipped);
  }
  function reset() { show('block'); remaining = 30; flipped = false; update(); caption('버튼을 눌러 차단 흐름을 체험해 보세요.'); }
  function done(message) {
    show('done'); $('done-title').replaceChildren(...message.split('\n').flatMap((line, index) => index ? [document.createElement('br'), document.createTextNode(line)] : [document.createTextNode(line)]));
    caption('웹 체험입니다. 실제 앱에서 나에게 맞게 설정해 보세요.'); $('demo-reset').focus({preventScroll:true});
  }
  $('demo-start').addEventListener('click', () => {
    show('timer'); remaining = 30; flipped = false; update(); caption('‘뒤집기 체험’을 눌러 타이머를 시작하세요.'); $('demo-flip').focus({preventScroll:true});
  });
  $('demo-flip').addEventListener('click', () => {
    stop(); flipped = !flipped; update();
    caption(flipped ? '뒤집힌 동안 진행됩니다. 다시 들면 멈춰요.' : '타이머가 멈췄어요. 다시 뒤집으면 이어집니다.');
    if (flipped) interval = setInterval(() => {
      remaining = Math.max(0, remaining - 1); update();
      if (!remaining) { show('choice'); flipped = false; caption('잠깐 멈춘 뒤, 계속 볼지 직접 선택하세요.'); $('demo-no').focus({preventScroll:true}); }
    }, 200);
  });
  $('demo-watch').addEventListener('click', () => done('계속 보기 선택.\n무료 이용에는 일일 한도가 있어요.'));
  $('demo-close').addEventListener('click', () => done('좋아요.\n내 시간으로 돌아가요.'));
  $('demo-no').addEventListener('click', () => done('좋아요.\n내 시간으로 돌아가요.'));
  $('demo-yes').addEventListener('click', () => done('계속 보기 선택.\n이번에는 의식적으로.'));
  $('demo-cancel').addEventListener('click', () => { reset(); $('demo-start').focus({preventScroll:true}); });
  $('demo-reset').addEventListener('click', () => { reset(); $('demo-start').focus({preventScroll:true}); });
  document.addEventListener('visibilitychange', () => {
    if (document.hidden && current === 'timer' && flipped) { stop(); flipped = false; update(); caption('체험이 일시정지됐어요. 다시 뒤집어 이어가세요.'); }
  });
})();
