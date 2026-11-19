# Dido — Free AI Chat

A ChatGPT-style AI assistant that is **100% free**: no subscription, no login, no API key.

The AI model runs **inside your browser** with [WebLLM](https://github.com/mlc-ai/web-llm) (WebGPU), so there is no server bill and your chats never leave your device.

## Features
- Multiple chats with history (saved in your browser)
- Streaming replies with markdown and code formatting
- Choose a model: Llama 3.2, Qwen 2.5, Phi 3.5
- Works on desktop and phone (light and dark mode)

## Run it
It's one file. Open `index.html` through any static server:

```bash
python3 -m http.server 8080
# open http://localhost:8080
```

Or host it free on GitHub Pages, Vercel or Netlify.

## Requirements
A browser with WebGPU: recent Chrome or Edge (desktop or Android). The first message downloads the model once (about 1 GB for the default); later visits load it from cache.
