from fastapi import FastAPI, HTTPException
from fastapi.staticfiles import StaticFiles
from pydantic import BaseModel
import uvicorn
from fastapi.middleware.cors import CORSMiddleware
import os
import re

app = FastAPI(title="SnipIT Server", version="1.0.0")

# Allow connections from your Android emulator/device
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

# Load GitHub Models token from local.properties
GITHUB_TOKEN = None

def load_github_token():
    global GITHUB_TOKEN
    try:
        local_props_path = os.path.join(os.path.dirname(__file__), "local.properties")
        if os.path.exists(local_props_path):
            with open(local_props_path, 'r') as f:
                for line in f:
                    if line.startswith('OPENROUTER_API_KEY='):
                        GITHUB_TOKEN = line.split('=', 1)[1].strip()
                        print(f"✓ Loaded GitHub token from local.properties")
                        return True
    except Exception as e:
        print(f"⚠️ Could not load token from local.properties: {e}")
    return False

# Load token on startup
load_github_token()

class CodeRequest(BaseModel):
    code: str
    language: str

@app.get("/api/config")
async def get_config():
    """Expose GitHub token to web portal for AI"""
    return {
        "github_token": GITHUB_TOKEN if GITHUB_TOKEN else None,
        "has_token": bool(GITHUB_TOKEN)
    }

@app.post("/analyze")
async def analyze_code(request: CodeRequest):
    print(f"Received request: {request.code}")

    # QUICK PRE-CHECK FOR GREETINGS (to ensure natural response)
    greetings = ["hello", "hi", "huy", "huy!", "yo", "hey"]
    if request.code.lower().strip() in greetings:
        return {
            "fixed_code": "No code provided.",
            "tags": "#Greeting",
            "explanation": "Hello! I am Snip-AI, your dedicated assistant for the SnipIT project. How can I help you with your code or your Vault today?"
        }

    # If GitHub Token is missing, use fallback
    if not GITHUB_TOKEN:
        return {
            "fixed_code": "// Token Missing",
            "tags": "#Offline",
            "explanation": "GitHub Token not found in local.properties. Please add your token to use real AI."
        }

    try:
        import requests
        
        # SNIP-AI PERSONA (Strict & Context-Aware)
        system_prompt = (
            "You are Snip-AI, the official intelligent assistant for the SnipIT project. "
            "You are an INTEGRATED part of the SnipIT ecosystem, not a generic code analyzer.\n\n"
            "**CORE INSTRUCTIONS**:\n"
            "1. BE CONVERSATIONAL. If the user talks to you normally, respond normally. "
            "2. DO NOT assume every message is a code snippet. Do not try to 'fix' words like 'hello' or 'thanks'.\n"
            "3. If the user provides code, analyze it professionally. If they don't, just chat.\n"
            "4. Never start a response with 'The provided code snippet is...' unless there is actually code.\n"
            "5. You are an expert in Android (Kotlin/Java) and Web (JS/CSS) because SnipIT is for these developers.\n"
            "6. Use professional Taglish if appropriate."
        )

        response = requests.post(
            "https://models.inference.ai.azure.com/chat/completions",
            headers={
                "Authorization": f"Bearer {GITHUB_TOKEN}",
                "Content-Type": "application/json"
            },
            json={
                "messages": [
                    {"role": "system", "content": system_prompt},
                    {"role": "user", "content": request.code}
                ],
                "model": "gpt-4o", # Or any other model available on GitHub Models
                "temperature": 0.7
            }
        )
        
        data = response.json()
        reply = data['choices'][0]['message']['content']
        
        # Extract potential code block for 'fixed_code' field
        code_match = re.search(r'```(?:\w+)?\n([\s\S]+?)\n```', reply)
        fixed_code = code_match.group(1) if code_match else "No specific code found."

        return {
            "fixed_code": fixed_code,
            "tags": "#SnipAI,#CloudGenerated",
            "explanation": reply
        }

    except Exception as e:
        print(f"AI Error: {e}")
        return {
            "fixed_code": "// AI Error",
            "tags": "#Error",
            "explanation": f"Failed to connect to GitHub Models: {str(e)}"
        }

# Mount web portal static files AFTER API routes
web_dir = os.path.join(os.path.dirname(__file__), "web")
if os.path.exists(web_dir):
    app.mount("/", StaticFiles(directory=web_dir, html=True), name="web")

if __name__ == "__main__":
    print("🚀 Starting SnipIT Server...")
    print(f"📡 API available at: http://localhost:8000/analyze")
    print(f"🌐 Web Portal at: http://localhost:8000")
    uvicorn.run(app, host="0.0.0.0", port=8000)
