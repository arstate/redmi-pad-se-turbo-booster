use std::collections::HashSet;
use std::env;
use std::fs::File;
use std::io::{BufRead, BufReader, Read, Write};
use std::os::unix::io::FromRawFd;
use std::os::unix::net::UnixListener;
use std::process::Command;
use std::sync::atomic::{AtomicBool, Ordering};
use std::sync::{Arc, Mutex};
use std::thread;
use std::time::Duration;

use rsbinder::{hub, ProxyHandle};

#[derive(Clone, Debug, PartialEq)]
enum Profile {
    Stock,
    FlagshipBalanced,
    ExtremeGamingRender,
    DebloatRestore,
}

impl Profile {
    fn from_str(s: &str) -> Self {
        match s {
            "FLAGSHIP_BALANCED" => Profile::FlagshipBalanced,
            "EXTREME_GAMING_RENDER" => Profile::ExtremeGamingRender,
            "DEBLOAT_RESTORE" => Profile::DebloatRestore,
            _ => Profile::Stock,
        }
    }

    fn as_str(&self) -> &'static str {
        match self {
            Profile::Stock => "STOCK",
            Profile::FlagshipBalanced => "FLAGSHIP_BALANCED",
            Profile::ExtremeGamingRender => "EXTREME_GAMING_RENDER",
            Profile::DebloatRestore => "DEBLOAT_RESTORE",
        }
    }
}

struct DaemonState {
    profile: Profile,
    auth_token: String,
    protected_apps: HashSet<String>,
    active_game: Option<String>,
    suspended_hogs: HashSet<String>,
    screen_is_on: bool,
}

impl DaemonState {
    fn new(token: String) -> Self {
        let mut protected = HashSet::new();
        protected.insert("com.whatsapp".to_string());
        protected.insert("com.whatsapp.w4b".to_string());
        protected.insert("org.telegram.messenger".to_string());
        protected.insert("com.discord".to_string());
        protected.insert("com.tailscale.ipn".to_string());
        protected.insert("com.google.android.deskclock".to_string());
        protected.insert("com.android.deskclock".to_string());

        DaemonState {
            profile: Profile::FlagshipBalanced,
            auth_token: token,
            protected_apps: protected,
            active_game: None,
            suspended_hogs: HashSet::new(),
            screen_is_on: true,
        }
    }
}

// Low-overhead execution helper
fn run_cmd(cmd: &str, args: &[&str]) -> (bool, String) {
    match Command::new(cmd).args(args).output() {
        Ok(output) => {
            let out_str = String::from_utf8_lossy(&output.stdout).trim().to_string();
            let err_str = String::from_utf8_lossy(&output.stderr).trim().to_string();
            let res = if out_str.is_empty() { err_str } else { out_str };
            (output.status.success(), res)
        }
        Err(e) => (false, e.to_string()),
    }
}

// MQSAS Call 21 Persistent Property Setter via direct Binder IPC (rsbinder) with fallback
fn mqsas_setprop(key: &str, val: &str) -> bool {
    let prop_arg = format!("{} {}", key, val);

    // 1. Direct Binder IPC via rsbinder (0-fork fastpath)
    if let Some(service) = hub::check_service("miui.mqsas.IMQSNative") {
        if let Some(proxy) = service.as_any().downcast_ref::<ProxyHandle>() {
            if let Ok(mut data) = proxy.prepare_request("miui.mqsas.IMQSNative") {
                let _ = data.write_i32(1);
                let _ = data.write(&"setprop");
                let _ = data.write_i32(1);
                let _ = data.write(&prop_arg.as_str());
                let _ = data.write(&"/dev/null");
                let _ = data.write_i32(600);
                if proxy.submit_transact(21, &data, 0).is_ok() {
                    return true;
                }
            }
        }
    }

    // 2. Fallback via /system/bin/service call
    let (ok, _) = run_cmd(
        "/system/bin/service",
        &[
            "call", "miui.mqsas.IMQSNative", "21",
            "i32", "1",
            "s16", "setprop",
            "i32", "1",
            "s16", &prop_arg,
            "s16", "/dev/null",
            "i32", "600",
        ],
    );
    if !ok {
        run_cmd("/system/bin/setprop", &[key, val]);
    }
    true
}

// Fetch process resident set size (RSS) in KB
fn get_current_rss_kb() -> usize {
    if let Ok(mut file) = File::open("/proc/self/statm") {
        let mut s = String::new();
        if file.read_to_string(&mut s).is_ok() {
            let parts: Vec<&str> = s.split_whitespace().collect();
            if parts.len() >= 2 {
                if let Ok(pages) = parts[1].parse::<usize>() {
                    return pages * 4; // 4KB pages
                }
            }
        }
    }
    0
}

// Low-overhead screen state detection using sysfs (0 forks) with dumpsys fallback
fn is_screen_awake() -> bool {
    for path in &[
        "/sys/class/backlight/panel0-backlight/brightness",
        "/sys/class/leds/lcd-backlight/brightness",
        "/sys/class/backlight/lcd-backlight/brightness",
    ] {
        if let Ok(mut file) = File::open(path) {
            let mut s = String::new();
            if file.read_to_string(&mut s).is_ok() {
                if let Ok(val) = s.trim().parse::<u32>() {
                    return val > 0;
                }
            }
        }
    }

    // Fallback to dumpsys power
    let (_, out) = run_cmd("/system/bin/dumpsys", &["power"]);
    for line in out.lines() {
        if line.contains("mWakefulness=Awake") || line.contains("Display Power: state=ON") {
            return true;
        }
    }
    false
}

// Check current foreground package from dumpsys window
fn get_foreground_package() -> Option<String> {
    let (_, out) = run_cmd("/system/bin/dumpsys", &["window", "visible-apps"]);
    if out.is_empty() {
        let (_, out_win) = run_cmd("/system/bin/dumpsys", &["window", "windows"]);
        return parse_focused_pkg(&out_win);
    }
    parse_focused_pkg(&out)
}

fn parse_focused_pkg(out: &str) -> Option<String> {
    for line in out.lines() {
        if line.contains("mCurrentFocus") || line.contains("mFocusedApp") || line.contains("top-activity") {
            if let Some(start) = line.find('{') {
                if let Some(slash) = line[start..].find('/') {
                    let seg = &line[start + 1..start + slash];
                    let parts: Vec<&str> = seg.split_whitespace().collect();
                    if let Some(pkg) = parts.last() {
                        let clean = pkg.trim();
                        if !clean.is_empty() && clean.contains('.') {
                            return Some(clean.to_string());
                        }
                    }
                }
            }
        }
    }
    None
}

fn main() {
    let args: Vec<String> = env::args().collect();
    let mut token = String::new();
    let mut socket_name = "turbodaemon_ipc".to_string();
    let mut is_daemon = false;

    let mut i = 1;
    while i < args.len() {
        match args[i].as_str() {
            "--token" => {
                if i + 1 < args.len() {
                    token = args[i + 1].clone();
                    i += 1;
                }
            }
            "--socket" => {
                if i + 1 < args.len() {
                    socket_name = args[i + 1].clone();
                    i += 1;
                }
            }
            "--daemon" => {
                is_daemon = true;
            }
            _ => {}
        }
        i += 1;
    }

    // When running as a daemon, redirect standard I/O to /dev/null to prevent pipe deadlocks
    if is_daemon {
        unsafe {
            let devnull = libc::open(b"/dev/null\0".as_ptr() as *const libc::c_char, libc::O_RDWR);
            if devnull >= 0 {
                libc::dup2(devnull, libc::STDIN_FILENO);
                libc::dup2(devnull, libc::STDOUT_FILENO);
                libc::dup2(devnull, libc::STDERR_FILENO);
                if devnull > libc::STDERR_FILENO {
                    libc::close(devnull);
                }
            }
        }
    }

    let state = Arc::new(Mutex::new(DaemonState::new(token)));
    let running = Arc::new(AtomicBool::new(true));

    // 1. Bind Linux Abstract Unix Domain Socket (\0turbodaemon_ipc)
    let listener = match bind_abstract_socket(&socket_name) {
        Ok(l) => l,
        Err(e) => {
            eprintln!("[-] Failed to bind abstract socket: {}", e);
            std::process::exit(1);
        }
    };

    // 2. Start Worker Thread: 90Hz & Device Config Guard
    let state_guard = Arc::clone(&state);
    let running_guard = Arc::clone(&running);
    thread::spawn(move || {
        while running_guard.load(Ordering::Relaxed) {
            let prof = {
                let st = state_guard.lock().unwrap();
                st.profile.clone()
            };

            if prof == Profile::FlagshipBalanced || prof == Profile::ExtremeGamingRender {
                // Check 90Hz and lock via MQSAS Call 21
                let (_, peak) = run_cmd("/system/bin/settings", &["get", "system", "peak_refresh_rate"]);
                if peak.trim() != "90.0" && peak.trim() != "90" {
                    mqsas_setprop("persist.sys.miui.sf_fps", "90");
                    run_cmd("/system/bin/settings", &["put", "system", "peak_refresh_rate", "90.0"]);
                    run_cmd("/system/bin/settings", &["put", "system", "min_refresh_rate", "90.0"]);
                    run_cmd("/system/bin/settings", &["put", "secure", "user_refresh_rate", "90"]);
                }

                // Device level left untouched to avoid triggering com.miui.home launcher crash

                // Check Cgroup v2 Freezer
                let (_, freezer) = run_cmd("/system/bin/device_config", &["get", "activity_manager_native_boot", "use_freezer"]);
                if freezer.trim() != "true" {
                    run_cmd("/system/bin/device_config", &["put", "activity_manager_native_boot", "use_freezer", "true"]);
                }
            }

            thread::sleep(Duration::from_secs(8));
        }
    });

    // 3. Start Worker Thread: Screen-Off Gentle Memory Reclaimer & Game Isolation
    let state_reclaim = Arc::clone(&state);
    let running_reclaim = Arc::clone(&running);
    thread::spawn(move || {
        let heavy_hogs = [
            "com.ss.android.ugc.trill",
            "com.shopee.id",
            "com.tokopedia.tkpd",
            "com.instagram.android",
            "com.facebook.katana",
            "com.netflix.mediaclient",
            "com.maxstream",
        ];

        let target_games = [
            "com.tencent.ig",
            "com.mobile.legends",
            "com.roblox.client",
            "com.mojang.minecraftpe",
            "com.lemon.lvoverseas",
            "com.lemon.ccpadpro",
        ];

        while running_reclaim.load(Ordering::Relaxed) {
            let awake = is_screen_awake();
            let mut st = state_reclaim.lock().unwrap();

            // Detect screen-off transition: gentle compaction without thrashing active Page Cache
            if st.screen_is_on && !awake {
                st.screen_is_on = false;
                if st.profile != Profile::Stock {
                    run_cmd("/system/bin/cmd", &["activity", "compact", "system"]);
                }
            } else if !st.screen_is_on && awake {
                st.screen_is_on = true;
            }

            // Game / CapCut Session Isolation: only check foreground while screen is on
            if awake && (st.profile == Profile::ExtremeGamingRender || st.profile == Profile::FlagshipBalanced) {
                if let Some(fg) = get_foreground_package() {
                    let is_game = target_games.iter().any(|&g| g == fg);

                    if is_game && st.active_game.as_ref() != Some(&fg) {
                        st.active_game = Some(fg.clone());
                        run_cmd("/system/bin/cmd", &["tare", "set-vip", "0", &fg, "true"]);

                        for &hog in &heavy_hogs {
                            if !st.protected_apps.contains(hog) {
                                run_cmd("/system/bin/cmd", &["package", "suspend", hog]);
                                st.suspended_hogs.insert(hog.to_string());
                            }
                        }
                    } else if !is_game && st.active_game.is_some() {
                        if let Some(ref prev_game) = st.active_game {
                            run_cmd("/system/bin/cmd", &["tare", "set-vip", "0", prev_game, "false"]);
                        }
                        st.active_game = None;

                        for hog in st.suspended_hogs.drain() {
                            run_cmd("/system/bin/cmd", &["package", "unsuspend", &hog]);
                        }
                    }
                }
            }

            drop(st);
            thread::sleep(Duration::from_millis(4000));
        }
    });

    // 4. Main Event Loop: Handle Socket IPC Commands
    for stream in listener.incoming() {
        if !running.load(Ordering::Relaxed) {
            break;
        }

        match stream {
            Ok(mut s) => {
                let state_clone = Arc::clone(&state);
                let running_clone = Arc::clone(&running);

                thread::spawn(move || {
                    let mut reader = BufReader::new(s.try_clone().unwrap());
                    let mut is_authenticated = false;
                    let mut line = String::new();

                    while reader.read_line(&mut line).unwrap_or(0) > 0 {
                        let trimmed = line.trim();
                        let parts: Vec<&str> = trimmed.split_whitespace().collect();

                        if parts.is_empty() {
                            line.clear();
                            continue;
                        }

                        let cmd = parts[0];
                        let token_required = {
                            let st = state_clone.lock().unwrap();
                            !st.auth_token.is_empty()
                        };

                        if token_required && !is_authenticated && cmd != "AUTH" {
                            let _ = s.write_all(b"ERR_UNAUTHENTICATED\n");
                            line.clear();
                            continue;
                        }

                        let response = match cmd {
                            "AUTH" => {
                                let st = state_clone.lock().unwrap();
                                if parts.len() > 1 && (st.auth_token.is_empty() || parts[1] == st.auth_token) {
                                    is_authenticated = true;
                                    "OK\n".to_string()
                                } else {
                                    "ERR_AUTH\n".to_string()
                                }
                            }
                            "PING" => {
                                let st = state_clone.lock().unwrap();
                                let rss = get_current_rss_kb();
                                format!(
                                    "PONG version=2.0 profile={} rss_kb={} screen={} active_game={}\n",
                                    st.profile.as_str(),
                                    rss,
                                    if st.screen_is_on { "on" } else { "off" },
                                    st.active_game.as_deref().unwrap_or("none")
                                )
                            }
                            "SET_PROFILE" => {
                                if parts.len() > 1 {
                                    let mut st = state_clone.lock().unwrap();
                                    st.profile = Profile::from_str(parts[1]);
                                    format!("OK profile={}\n", st.profile.as_str())
                                } else {
                                    "ERR_ARGS\n".to_string()
                                }
                            }
                            "GET_STATUS" => {
                                let st = state_clone.lock().unwrap();
                                let rss = get_current_rss_kb();
                                format!(
                                    "STATUS profile={} rss_kb={} suspended_count={} protected_count={}\n",
                                    st.profile.as_str(),
                                    rss,
                                    st.suspended_hogs.len(),
                                    st.protected_apps.len()
                                )
                            }
                            "TRIGGER_RECLAIM" => {
                                run_cmd("/system/bin/cmd", &["activity", "compact", "system"]);
                                run_cmd("/system/bin/sm", &["fstrim"]);
                                "OK reclaimed\n".to_string()
                            }
                            "ADD_PROTECTED_APP" => {
                                if parts.len() > 1 {
                                    let mut st = state_clone.lock().unwrap();
                                    st.protected_apps.insert(parts[1].to_string());
                                    "OK\n".to_string()
                                } else {
                                    "ERR_ARGS\n".to_string()
                                }
                            }
                            "REMOVE_PROTECTED_APP" => {
                                if parts.len() > 1 {
                                    let mut st = state_clone.lock().unwrap();
                                    st.protected_apps.remove(parts[1]);
                                    "OK\n".to_string()
                                } else {
                                    "ERR_ARGS\n".to_string()
                                }
                            }
                            "STOP" => {
                                running_clone.store(false, Ordering::Relaxed);
                                let _ = s.write_all(b"OK stopping\n");
                                std::process::exit(0);
                            }
                            _ => "ERR_UNKNOWN_CMD\n".to_string(),
                        };

                        let _ = s.write_all(response.as_bytes());
                        line.clear();
                    }
                });
            }
            Err(e) => {
                eprintln!("Socket accept error: {}", e);
            }
        }
    }
}

// Binds an abstract Unix domain socket on Linux/Android (\0<name>)
fn bind_abstract_socket(name: &str) -> std::io::Result<UnixListener> {
    unsafe {
        let fd = libc::socket(libc::AF_UNIX, libc::SOCK_STREAM, 0);
        if fd < 0 {
            return Err(std::io::Error::last_os_error());
        }

        let mut addr: libc::sockaddr_un = std::mem::zeroed();
        addr.sun_family = libc::AF_UNIX as libc::sa_family_t;

        // Abstract socket address starts with null byte '\0'
        let bytes = name.as_bytes();
        if bytes.len() + 1 >= addr.sun_path.len() {
            libc::close(fd);
            return Err(std::io::Error::new(
                std::io::ErrorKind::InvalidInput,
                "Socket name too long",
            ));
        }

        addr.sun_path[0] = 0;
        for (i, &b) in bytes.iter().enumerate() {
            addr.sun_path[i + 1] = b as libc::c_char;
        }

        let len = (std::mem::size_of::<libc::sa_family_t>() + 1 + bytes.len()) as libc::socklen_t;

        if libc::bind(fd, &addr as *const _ as *const libc::sockaddr, len) < 0 {
            let err = std::io::Error::last_os_error();
            libc::close(fd);
            return Err(err);
        }

        if libc::listen(fd, 8) < 0 {
            let err = std::io::Error::last_os_error();
            libc::close(fd);
            return Err(err);
        }

        Ok(UnixListener::from_raw_fd(fd))
    }
}
