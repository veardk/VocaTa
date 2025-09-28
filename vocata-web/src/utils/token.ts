const TOKEN_KEY = 'vocata_token'

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string, time?: number) {
  localStorage.setItem(TOKEN_KEY, token)
  if (time) {
    const expireTime = Date.now() + time * 1000
    localStorage.setItem(`${TOKEN_KEY}_expire`, String(expireTime))
  }
}

export function removeToken() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(`${TOKEN_KEY}_expire`)
}

export function isTokenExpired(): boolean {
  const expireTime = localStorage.getItem(`${TOKEN_KEY}_expire`)
  if (!expireTime) return false
  return Date.now() > Number(expireTime)
}