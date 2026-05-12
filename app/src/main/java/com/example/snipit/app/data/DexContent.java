package com.example.snipit.app.data;

import android.content.Context;

/** Offline reference content — built-in rows plus optional {@code assets/dex/*.json} overlays. */
public final class DexContent {

    private DexContent() {}

    /** Built-in Git rows + {@code assets/dex/git_extra.json} (if present). */
    public static DexDoc[] gitWith(Context ctx) {
        return DexAssetLoader.concat(GIT, DexAssetLoader.loadArray(ctx, "dex/git_extra.json"));
    }

    /** Built-in Linux rows + {@code assets/dex/linux_extra.json}. */
    public static DexDoc[] linuxWith(Context ctx) {
        return DexAssetLoader.concat(LINUX, DexAssetLoader.loadArray(ctx, "dex/linux_extra.json"));
    }

    public static DexDoc[] powershellWith(Context ctx) {
        return DexAssetLoader.concat(POWERSHELL, DexAssetLoader.loadArray(ctx, "dex/powershell_extra.json"));
    }

    public static DexDoc[] adbWith(Context ctx) {
        return DexAssetLoader.concat(ADB, DexAssetLoader.loadArray(ctx, "dex/adb_extra.json"));
    }

    public static DexDoc[] regexWith(Context ctx) {
        // Regex is primarily in handbook.json, but we can add built-ins here
        return DexAssetLoader.concat(REGEX, DexAssetLoader.loadArray(ctx, "dex/regex_extra.json"));
    }

    public static final DexDoc[] GIT =
            new DexDoc[] {
                new DexDoc(
                        "git status",
                        "Show working tree",
                        "Shows staged and unstaged changes, branch name, and ahead/behind counts.\n\n"
                                + "Flags:\n"
                                + "  -s / --short  Compact output (two columns)\n"
                                + "  -b            Show branch info\n"
                                + "  --porcelain   Machine-readable (for scripts)\n\n"
                                + "Example:\n  git status -s"),
                new DexDoc(
                        "git pull",
                        "Fetch & merge remote",
                        "Downloads objects from remote and integrates into current branch (fetch + merge/rebase depending on config).\n\n"
                                + "Flags:\n"
                                + "  --rebase       Rebase instead of merge\n"
                                + "  --ff-only      Fail if not fast-forward\n\n"
                                + "Example:\n  git pull origin main"),
                new DexDoc(
                        "git push",
                        "Push commits",
                        "Uploads local commits to the remote branch.\n\n"
                                + "Flags:\n"
                                + "  -u origin NAME  Set upstream for new branch\n"
                                + "  --force-with-lease  Safer force (checks remote moved)\n\n"
                                + "Example:\n  git push -u origin feature/login"),
                new DexDoc(
                        "git merge main",
                        "Merge branch",
                        "Combines the named branch into your current HEAD.\n\n"
                                + "Resolve conflicts in files, then git add && git commit.\n\n"
                                + "Example:\n  git merge origin/develop"),
                new DexDoc(
                        "git reset --soft HEAD~1",
                        "Undo last commit (keep files)",
                        "Moves HEAD back one commit but keeps index and working tree — useful to squash or reword the last commit.\n\n"
                                + "Contrast: --mixed (default) unstages; --hard discards changes.\n\n"
                                + "Example:\n  git reset --soft HEAD~1"),
                new DexDoc(
                        "git stash",
                        "Shelve work in progress",
                        "Temporarily shelves changes so you can switch branches or pull clean — without committing half-done work.\n\n"
                                + "Flags:\n"
                                + "  push -m \"msg\"   Save with a message\n"
                                + "  pop               Apply latest stash and remove it\n"
                                + "  list              Show stash@{n} entries\n"
                                + "  drop stash@{n}    Delete one entry\n\n"
                                + "Example:\n  git stash push -m \"WIP: auth feature\""),
            };

    public static final DexDoc[] LINUX =
            new DexDoc[] {
                new DexDoc(
                        "ls -la",
                        "List all files",
                        "Lists directory entries including hidden files (-a), long format (-l).\n\n"
                                + "Flags: -h human sizes, -t sort by time.\n\n"
                                + "Example:\n  ls -la ~/.ssh"),
                new DexDoc(
                        "chmod +x app.sh",
                        "Make executable",
                        "Changes file mode bits; +x adds execute permission for user/group/others depending on umask.\n\n"
                                + "Example:\n  chmod 755 deploy.sh"),
                new DexDoc(
                        "grep -R \"TODO\" .",
                        "Recursive search",
                        "Searches file contents recursively. Quote patterns with spaces.\n\n"
                                + "Flags: -n line numbers, -i ignore case.\n\n"
                                + "Example:\n  grep -RIn \"FIXME\" src/"),
            };

    public static final DexDoc[] POWERSHELL =
            new DexDoc[] {
                new DexDoc(
                        "Get-ChildItem",
                        "List directory",
                        "Lists files/folders; alias dir / ls in PowerShell.\n\n"
                                + "Example:\n  Get-ChildItem -Recurse -Filter *.cs"),
                new DexDoc(
                        "Get-NetIPAddress",
                        "Show IP config",
                        "Shows IPv4/IPv6 addresses bound to interfaces — similar to ipconfig but object-based.\n\n"
                                + "Pipe to Where-Object to filter."),
            };

    public static final DexDoc[] ADB =
            new DexDoc[] {
                new DexDoc(
                        "adb devices",
                        "List devices",
                        "Lists attached USB devices and emulators authorized for debugging.\n\n"
                                + "If unauthorized, accept RSA prompt on phone."),
                new DexDoc(
                        "adb install app.apk",
                        "Install APK",
                        "Pushes APK to device and installs. Use -r to replace existing app.\n\n"
                                + "Example:\n  adb install -r app-debug.apk"),
            };

    public static final DexDoc[] REGEX =
            new DexDoc[] {
                new DexDoc(
                        "Email Pattern",
                        "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$",
                        "Validates standard email addresses. Supports dots, dashes, and underscores in the local part."),
                new DexDoc(
                        "Strong Password",
                        "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$",
                        "Minimum eight characters, at least one letter and one number."),
                new DexDoc(
                        "URL Validation",
                        "^(https?://)?([\\da-z.-]+)\\.([a-z.]{2,6})([/\\w .-]*)*/?$",
                        "Matches http, https, or domain-only URLs with paths."),
            };

    /** HTTP code, short label, long explanation */
    public static final String[][] HTTP =
            new String[][] {
                {
                    "200",
                    "OK — request succeeded",
                    "The server understood and processed the request. For GET, the body usually carries the resource. If an API returns 200 but wrong data, check query params and auth headers — not the HTTP layer."
                },
                {
                    "404",
                    "Not found",
                    "No resource matches the URL path. Common causes: typo in route, wrong API version prefix, or the server expects POST vs GET. Verify base URL and trailing slashes."
                },
                {
                    "500",
                    "Internal server error",
                    "The server crashed or threw an unhandled exception. Check server logs. Client can retry with backoff; fix is on the backend."
                },
            };

    public static String[][] allHttpRows() {
        return new String[][] {
            {"200", "OK — request succeeded"},
            {"201", "Created — resource created"},
            {"301", "Moved permanently"},
            {"304", "Not modified"},
            {"400", "Bad request"},
            {"401", "Unauthorized"},
            {"403", "Forbidden"},
            {"404", "Not found"},
            {"500", "Internal server error"},
            {"503", "Service unavailable"}
        };
    }

    public static String httpLongExplain(String code) {
        switch (code) {
            case "200":
                return "Success. The request worked and the response has a body (unless HEAD). If you expected JSON but see HTML, you may be hitting the wrong route or a login page.";
            case "201":
                return "Created. A new resource was created (common for POST). The Location header may point to it.";
            case "301":
                return "Moved permanently. The URL changed forever — update bookmarks and clients to the new URL from the Location header.";
            case "304":
                return "Not modified. Cached version is still valid (ETag/If-Modified-Since). Good for bandwidth; no body usually.";
            case "400":
                return "Bad request. Malformed JSON, missing fields, or invalid params. Fix the client payload; check server validation error messages.";
            case "401":
                return "Unauthorized. Authentication missing or token invalid/expired. Refresh tokens or log in again.";
            case "403":
                return "Forbidden. Authenticated but not allowed — roles/permissions. Different from 404 (resource exists but you cannot see it).";
            case "404":
                return "Not found. Wrong path or HTTP method. Confirm API base URL, trailing slashes, and REST verb.";
            case "500":
                return "Server error. Unhandled exception on server. Check server logs and stack traces — not fixable by changing query params.";
            case "503":
                return "Service unavailable. Overload or maintenance. Retry with backoff; if behind a proxy, check health checks.";
            default:
                return "HTTP " + code + ". See RFC 9110 / MDN. Read response body and server logs when debugging.";
        }
    }

    public static final String[][] PORTS =
            new String[][] {
                {
                    ":80",
                    "HTTP",
                    "Classic web traffic (unencrypted). Often redirected to 443. In dev, you may see 8080 instead."
                },
                {
                    ":443",
                    "HTTPS / TLS",
                    "Encrypted HTTP. Certificate errors usually mean wrong hostname, expired cert, or corporate proxy."
                },
                {
                    ":3306",
                    "MySQL",
                    "Default MySQL/MariaDB. Ensure firewall rules and bind-address allow your client."
                },
            };

    public static String[][] allPortRows() {
        return new String[][] {
            {":80", "HTTP"},
            {":443", "HTTPS / TLS"},
            {":3306", "MySQL"},
            {":5432", "PostgreSQL"},
            {":8080", "Alt HTTP / proxies"}
        };
    }

    public static String portLongExplain(String portKey) {
        switch (portKey) {
            case ":80":
                return PORTS[0][2];
            case ":443":
                return PORTS[1][2];
            case ":3306":
                return PORTS[2][2];
            case ":5432":
                return "Default PostgreSQL. Requires pg_hba.conf and listen_addresses for remote access.";
            case ":8080":
                return "Common alternate HTTP (Tomcat, proxies, dev servers). Often combined with reverse proxy to 443.";
            default:
                return "Verify which process listens: ss -lntp (Linux) or Get-NetTCPConnection (PowerShell).";
        }
    }
}
