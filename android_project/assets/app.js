(function () {
  var video = document.getElementById('videoPlayer');
  var placeholder = document.getElementById('placeholder');
  var fileInput = document.getElementById('fileInput');
  var btnPlay = document.getElementById('btnPlay');
  var btnStop = document.getElementById('btnStop');
  var btnImport = document.getElementById('btnImport');
  var btnTrim = document.getElementById('btnTrim');
  var btnFilter = document.getElementById('btnFilter');
  var btnSpeed = document.getElementById('btnSpeed');
  var btnExport = document.getElementById('btnExport');
  var timeDisplay = document.getElementById('timeDisplay');
  var progressBar = document.getElementById('progressBar');
  var progressFill = document.getElementById('progressFill');
  var trimPanel = document.getElementById('trimPanel');
  var filterPanel = document.getElementById('filterPanel');
  var speedPanel = document.getElementById('speedPanel');
  var currentFilter = 'none';
  var videoLoaded = false;

  function formatTime(s) {
    var m = Math.floor(s / 60);
    var sec = Math.floor(s % 60);
    return String(m).padStart(2, '0') + ':' + String(sec).padStart(2, '0');
  }

  function updateTime() {
    if (!videoLoaded) return;
    timeDisplay.textContent = formatTime(video.currentTime) + ' / ' + formatTime(video.duration);
    var pct = (video.currentTime / video.duration) * 100;
    progressFill.style.width = pct + '%';
    requestAnimationFrame(updateTime);
  }

  btnImport.addEventListener('click', function () { fileInput.click(); });

  fileInput.addEventListener('change', function () {
    var file = fileInput.files[0];
    if (!file) return;
    var url = URL.createObjectURL(file);
    video.src = url;
    video.style.display = 'block';
    video.classList.add('visible');
    placeholder.classList.add('hidden');
    videoLoaded = true;
    video.addEventListener('loadedmetadata', function () { updateTime(); });
  });

  btnPlay.addEventListener('click', function () {
    if (!videoLoaded) return;
    if (video.paused) { video.play(); btnPlay.textContent = '⏸'; updateTime(); }
    else { video.pause(); btnPlay.textContent = '▶'; }
  });

  btnStop.addEventListener('click', function () {
    if (!videoLoaded) return;
    video.pause(); video.currentTime = 0; btnPlay.textContent = '▶';
  });

  progressBar.addEventListener('click', function (e) {
    if (!videoLoaded) return;
    var rect = progressBar.getBoundingClientRect();
    var pct = (e.clientX - rect.left) / rect.width;
    video.currentTime = pct * video.duration;
  });

  function closeAllPanels() { trimPanel.style.display = 'none'; filterPanel.style.display = 'none'; speedPanel.style.display = 'none'; }

  btnTrim.addEventListener('click', function () {
    closeAllPanels();
    trimPanel.style.display = 'block';
    if (videoLoaded) {
      var dur = video.duration;
      var ts = document.getElementById('trimStart');
      var te = document.getElementById('trimEnd');
      ts.max = Math.floor(dur); te.max = Math.floor(dur); te.value = Math.floor(dur);
      document.getElementById('trimStartVal').textContent = ts.value;
      document.getElementById('trimEndVal').textContent = te.value;
    }
  });

  document.getElementById('trimStart').addEventListener('input', function (e) { document.getElementById('trimStartVal').textContent = e.target.value; });
  document.getElementById('trimEnd').addEventListener('input', function (e) { document.getElementById('trimEndVal').textContent = e.target.value; });

  document.getElementById('btnApplyTrim').addEventListener('click', function () {
    if (!videoLoaded) return;
    var start = parseFloat(document.getElementById('trimStart').value);
    video.currentTime = start; video.play(); btnPlay.textContent = '⏸';
    var end = parseFloat(document.getElementById('trimEnd').value);
    video.addEventListener('timeupdate', function checkEnd() {
      if (video.currentTime >= end) { video.pause(); btnPlay.textContent = '▶'; video.removeEventListener('timeupdate', checkEnd); }
    });
    trimPanel.style.display = 'none';
  });

  document.getElementById('btnCancelTrim').addEventListener('click', function () { trimPanel.style.display = 'none'; });

  btnFilter.addEventListener('click', function () { closeAllPanels(); filterPanel.style.display = 'block'; });

  document.querySelectorAll('.btn-filter').forEach(function (btn) {
    btn.addEventListener('click', function () {
      document.querySelectorAll('.btn-filter').forEach(function (b) { b.classList.remove('active'); });
      btn.classList.add('active');
      currentFilter = btn.dataset.filter;
      video.style.filter = currentFilter === 'none' ? '' : currentFilter;
    });
  });

  document.getElementById('btnCancelFilter').addEventListener('click', function () { filterPanel.style.display = 'none'; });

  btnSpeed.addEventListener('click', function () { closeAllPanels(); speedPanel.style.display = 'block'; });

  document.querySelectorAll('.btn-speed').forEach(function (btn) {
    btn.addEventListener('click', function () {
      document.querySelectorAll('.btn-speed').forEach(function (b) { b.classList.remove('active'); });
      btn.classList.add('active');
      if (videoLoaded) video.playbackRate = parseFloat(btn.dataset.speed);
    });
  });

  document.getElementById('btnCancelSpeed').addEventListener('click', function () { speedPanel.style.display = 'none'; });

  btnExport.addEventListener('click', function () {
    if (!videoLoaded) { alert('请先导入视频'); return; }
    alert('视频已应用当前滤镜和速度设置。');
  });

  video.addEventListener('ended', function () { btnPlay.textContent = '▶'; });
})();
