
(function() {
  // 设计稿宽度（根据你的设计稿修改，通常是1920或1440）
  const designWidth = 1920
  
  // 设置根元素字体大小（1rem = 100px）
  function setRem() {
    const clientWidth = document.documentElement.clientWidth || document.body.clientWidth
    // 计算比例：当前宽度 / 设计稿宽度
    const scale = clientWidth / designWidth
    // 设置根元素字体大小：比例 × 100（实现1rem = 100px）
    document.documentElement.style.fontSize = scale * 100 + 'px'
  }
  
  // 初始化
  setRem()
  
  // 监听窗口变化
  window.addEventListener('resize', setRem)
  
  // 监听页面显示（解决浏览器后退问题）
  window.addEventListener('pageshow', function(e) {
    if (e.persisted) {
      setRem()
    }
  })
})()