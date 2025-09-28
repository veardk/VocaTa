(function () {
  function setRem() {
    const width = document.documentElement.clientWidth

    if (width <= 768) {
      // 移动端：基于375px设计稿，1rem = 100px
      document.documentElement.style.fontSize = (width / 375 * 100) + 'px'
    } else {
      // PC端：基于1920px设计稿，1rem = 100px
      const fontSize = Math.min(width / 1920 * 100, 100) // 限制最大字体大小
      document.documentElement.style.fontSize = fontSize + 'px'
    }
  }

  setRem()
  window.addEventListener('resize', setRem)
  window.addEventListener('pageshow', (e) => e.persisted && setRem())
})()