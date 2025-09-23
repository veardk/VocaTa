(function () {
  function setRem() {
    const width = document.documentElement.clientWidth
    // 移动端（≤768px）：基于375px设计稿
    if (width <= 768) {
      document.documentElement.style.fontSize = (width / 375 * 100) + 'px'
    } else {
      // PC端（>768px）：基于1920px设计稿
      document.documentElement.style.fontSize = (width / 1920 * 100) + 'px'
    }
  }

  setRem()
  window.addEventListener('resize', setRem)
  window.addEventListener('pageshow', (e) => e.persisted && setRem())
})()