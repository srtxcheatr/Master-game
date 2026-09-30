package com.boost.your.srt.shizuku;

// Runs inside the privileged Shizuku process (shell/root uid).
interface IShellService {
    // Transaction id reserved by Shizuku for destroying the user service.
    void destroy() = 16777114;

    // Returns [exitCode, stdout, stderr]
    String[] exec(String command) = 1;
}
