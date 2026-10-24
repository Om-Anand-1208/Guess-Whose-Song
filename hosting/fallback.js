(() => {
  const segments = window.location.pathname.split("/").filter(Boolean);
  const code = segments.length === 2 && segments[0] === "join"
    ? segments[1].toUpperCase()
    : "";
  const validCode = /^[A-Z0-9]{6}$/.test(code);

  if (!validCode) {
    document.getElementById("room-message").textContent =
      "Open a room invite on an Android device to join your friends.";
    document.getElementById("open-app").hidden = true;
    document.getElementById("room-code-wrap").hidden = true;
    return;
  }

  document.getElementById("room-code").textContent = code;
  document.getElementById("room-code-wrap").hidden = false;
  const apkUrl = "https://github.com/Om-Anand-1208/Guess-Whose-Song/raw/refs/heads/main/artifacts/GuessWhoseSong.apk";
  const fallbackUrl = encodeURIComponent(apkUrl);
  const openApp = document.getElementById("open-app");
  openApp.href = `intent://guess-whose-song-game.web.app/join/${code}#Intent;scheme=https;package=com.guesswhosesong.app;S.browser_fallback_url=${fallbackUrl};end`;
  openApp.hidden = false;
})();
