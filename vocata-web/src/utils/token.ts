import Cookies from "js-cookie";

export function getToken() {
  return Cookies.get("token");
}

export function setToken(token: string, time: number) {
  Cookies.set("token", token, {
    expires: time,
  });
}

export function removeToken() {
  Cookies.remove("token");
}