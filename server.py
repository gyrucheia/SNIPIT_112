from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import uvicorn
from fastapi.middleware.cors import CORSMiddleware

app = FastAPI()

# Allow connections from your Android emulator/device
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

class CodeRequest(BaseModel):
    code: str
    language: str

@app.post("/analyze")
async def analyze_code(request: CodeRequest):
    print(f"Received request: {request.code}")

    # This is the "ping" check your Android app uses to see if the server is online
    if request.code == "ping":
        return {
            "fixed_code": "pong",
            "tags": "system",
            "explanation": "Server is online!"
        }

    # Fallback responses for common chips
    msg = request.code.lower()
    if "tagalog" in msg:
        reply = "Narito ang paliwanag sa Tagalog: Ang code na ito ay gumagamit ng RecyclerView para sa listahan."
    elif "kotlin" in msg:
        reply = "Converting to Kotlin...\n\nval message = \"Hello SnipIT\""
    else:
        reply = f"I received your message: '{request.code}'. To use real AI, you can integrate Gemini or OpenAI here."

    return {
        "fixed_code": "Processed Code Here",
        "tags": "#AI,#LocalServer",
        "explanation": reply
    }

if __name__ == "__main__":
    print("Starting Snip-AI Local Server...")
    uvicorn.run(app, host="0.0.0.0", port=8000)
