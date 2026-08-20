# User-Agent

GAME THEME CATEGORY: crash / casual fruit garden.

Do **not** append `appid/` or `appname/`. Those tokens do not exist in
real Chrome and fingerprint the wrapper.

Shape (Chrome major 149, unique build/patch):

```
Mozilla/5.0 (Linux; Android {RELEASE}; {MANUFACTURER} {MODEL} Build/{ID})
AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.7742.118 Mobile Safari/537.36
```

Same string on OkHttp (`VineChrome.http`) and `WebSettings.userAgentString`.
Forbidden substrings: `WebView`, `wv/`, `Dart`, `Flutter`, package name.
