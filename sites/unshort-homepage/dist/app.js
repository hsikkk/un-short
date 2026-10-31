(() => {
  const t = (ko, en) => document.documentElement.lang === 'en' ? en : ko;
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
    $('flip-label').textContent = flipped ? t("다시 들기 · 일시정지", "Pick up \u00b7 Pause") : t("뒤집기 체험", "Try flipping");
    $('demo-flip').classList.toggle('flipped', flipped);
  }
  function reset() { show('block'); remaining = 30; flipped = false; update(); caption(t("버튼을 눌러 차단 흐름을 체험해 보세요.", "Press a button to try the blocking flow.")); }
  function done(message) {
    show('done'); $('done-title').replaceChildren(...message.split('\n').flatMap((line, index) => index ? [document.createElement('br'), document.createTextNode(line)] : [document.createTextNode(line)]));
    caption(t("웹 체험입니다. 실제 앱에서 나에게 맞게 설정해 보세요.", "This is a web demo. Customize your settings in the actual app.")); $('demo-reset').focus({preventScroll:true});
  }
  $('demo-start').addEventListener('click', () => {
    show('timer'); remaining = 30; flipped = false; update(); caption(t("‘뒤집기 체험’을 눌러 타이머를 시작하세요.", "Press Try flipping to start the timer.")); $('demo-flip').focus({preventScroll:true});
  });
  $('demo-flip').addEventListener('click', () => {
    stop(); flipped = !flipped; update();
    caption(flipped ? t("뒤집힌 동안 진행됩니다. 다시 들면 멈춰요.", "The timer runs face down. Pick up to pause.") : t("타이머가 멈췄어요. 다시 뒤집으면 이어집니다.", "Timer paused. Flip again to continue."));
    if (flipped) interval = setInterval(() => {
      remaining = Math.max(0, remaining - 1); update();
      if (!remaining) { show('choice'); flipped = false; caption(t("잠깐 멈춘 뒤, 계속 볼지 직접 선택하세요.", "After a pause, choose whether to keep watching.")); $('demo-no').focus({preventScroll:true}); }
    }, 200);
  });
  $('demo-watch').addEventListener('click', () => done(t("계속 보기 선택.\n무료 이용에는 일일 한도가 있어요.", "You chose to watch.\nFree use has a daily limit.")));
  $('demo-close').addEventListener('click', () => done(t("좋아요.\n내 시간으로 돌아가요.", "Sounds good.\nBack to your own time.")));
  $('demo-no').addEventListener('click', () => done(t("좋아요.\n내 시간으로 돌아가요.", "Sounds good.\nBack to your own time.")));
  $('demo-yes').addEventListener('click', () => done(t("계속 보기 선택.\n이번에는 의식적으로.", "You chose to watch.\nThis time, intentionally.")));
  $('demo-cancel').addEventListener('click', () => { reset(); $('demo-start').focus({preventScroll:true}); });
  $('demo-reset').addEventListener('click', () => { reset(); $('demo-start').focus({preventScroll:true}); });
  document.addEventListener('visibilitychange', () => {
    if (document.hidden && current === 'timer' && flipped) { stop(); flipped = false; update(); caption(t("체험이 일시정지됐어요. 다시 뒤집어 이어가세요.", "Demo paused. Flip again to continue.")); }
  });
})();
