// 防抖函数
function debounce<T extends (...args: unknown[]) => void>(
  func: T,
  wait: number,
  immediate: boolean = false
): (...args: Parameters<T>) => void {

  let timeout: ReturnType<typeof setTimeout> | null = null;

  return function (this: unknown, ...args: Parameters<T>): void {
    const later = (): void => {
      timeout = null;
      if (!immediate) {
        func.apply(this, args);
      }
    };

    const shouldCallNow = immediate && timeout === null;

    if (timeout !== null) {
      clearTimeout(timeout);
    }

    timeout = setTimeout(later, wait);

    if (shouldCallNow) {
      func.apply(this, args);
    }
  };

}
export default debounce;