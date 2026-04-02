Dev-Dex extra commands (optional)
==================================
Add JSON arrays here to ship more reference without changing Java.

Files:
  git_extra.json, linux_extra.json, powershell_extra.json, adb_extra.json

Each entry:
{
  "command": "git example --flag",
  "summary": "One line shown in the list",
  "documentation": "Full mini man-page text. Use \\n for newlines."
}

You can also host a JSON URL and download/cache it at runtime (future enhancement).
