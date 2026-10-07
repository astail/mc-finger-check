package io.github.astail.fingercheck;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerInputEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Locale;

/**
 * FingerCheck 本体。
 * プレイヤーに「指のトレーニング」的なルールを課す。現在のルールは 1 つ:
 * 「走っていないと動けない」— W で走らずに歩き続けると注意され、W を押し直すか走るまで動けなくなる。
 * サーバー側のみで動作（クライアント MOD 不要）。
 */
public final class FingerCheckPlugin extends JavaPlugin implements Listener {

    /** 判定タスクの実行間隔（tick）。2 tick = 0.1 秒。 */
    private static final long CHECK_INTERVAL_TICKS = 2L;
    /** 違反が続いている間、アクションバーを再表示する間隔。アクションバーは数秒で消えるため。 */
    private static final long ACTIONBAR_REPEAT_MILLIS = 1_000L;

    /** 注意の表示先。 */
    enum Display { ACTIONBAR, CHAT, BOTH }

    private final SprintRule sprintRule = new SprintRule();
    private final MovementLock movementLock = new MovementLock(this);

    private boolean active;
    private long thresholdMillis;
    private Display display;
    private boolean blockMovement;
    private String message;

    private BukkitTask task;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadSettings();
        if (!register()) {
            return;
        }
        startTask();
        getLogger().info("FingerCheck を有効化しました（判定時間: " + thresholdMillis + " ms / 状態: "
                + (active ? "ON" : "OFF") + "）。");
    }

    @Override
    public void onDisable() {
        stopTask();
        releaseAll();
    }

    private boolean register() {
        PluginCommand command = getCommand("fingercheck");
        if (command == null) {
            getLogger().severe("plugin.yml に fingercheck コマンドが定義されていません。プラグインを無効化します。");
            getServer().getPluginManager().disablePlugin(this);
            return false;
        }
        FingerCheckCommand handler = new FingerCheckCommand(this);
        command.setExecutor(handler);
        command.setTabCompleter(handler);
        getServer().getPluginManager().registerEvents(this, this);
        return true;
    }

    private void loadSettings() {
        reloadConfig();
        FileConfiguration config = getConfig();
        active = config.getBoolean("enabled", true);
        double seconds = Math.max(0.5, Math.min(60.0, config.getDouble("threshold-seconds", 2.0)));
        thresholdMillis = Math.round(seconds * 1000.0);
        String rawDisplay = config.getString("display", "actionbar");
        try {
            display = Display.valueOf(rawDisplay.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            getLogger().warning("display の値が不正です（actionbar / chat / both）: " + rawDisplay + " → actionbar を使います。");
            display = Display.ACTIONBAR;
        }
        blockMovement = config.getBoolean("block-movement", true);
        message = config.getString("message", "Shift も押してね！");
    }

    // ───────────────────────── スケジューラ ─────────────────────────

    private void startTask() {
        stopTask();
        task = getServer().getScheduler().runTaskTimer(this, this::tick, CHECK_INTERVAL_TICKS, CHECK_INTERVAL_TICKS);
    }

    private void stopTask() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void tick() {
        if (!active) {
            return;
        }
        long now = System.currentTimeMillis();
        for (Player player : getServer().getOnlinePlayers()) {
            if (player.hasPermission("fingercheck.bypass")) {
                release(player);
                continue;
            }
            SprintRule.Warning warning = sprintRule.update(player, now, thresholdMillis, ACTIONBAR_REPEAT_MILLIS);
            if (warning != SprintRule.Warning.NONE) {
                warn(player, warning == SprintRule.Warning.FIRST);
            }
            if (blockMovement && sprintRule.isWarned(player.getUniqueId())) {
                movementLock.lock(player);
            } else {
                movementLock.release(player);
            }
        }
    }

    // ───────────────────────── 通知 ─────────────────────────

    /** 注意を出す。チャットは違反 1 回につき最初の 1 回だけ、アクションバーは継続中ずっと出す。 */
    private void warn(Player player, boolean first) {
        Component text = Component.text(message, NamedTextColor.YELLOW);
        if (display != Display.CHAT) {
            player.sendActionBar(text);
        }
        if (first && display != Display.ACTIONBAR) {
            player.sendMessage(prefix().append(text));
        }
    }

    /** 違反状態とロックを解除する。 */
    private void release(Player player) {
        sprintRule.forget(player.getUniqueId());
        movementLock.release(player);
    }

    private void releaseAll() {
        for (Player player : getServer().getOnlinePlayers()) {
            release(player);
        }
        sprintRule.clear();
    }

    Component prefix() {
        return Component.text("[FingerCheck] ", NamedTextColor.AQUA);
    }

    /** W を離した・走るキーを押した瞬間に解除する（2 tick 待たずに動けるように）。 */
    @EventHandler
    public void onInput(PlayerInputEvent event) {
        Player player = event.getPlayer();
        if (sprintRule.isWarned(player.getUniqueId()) && SprintRule.releases(event.getInput())) {
            release(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        movementLock.clamp(event);
    }

    /** /reload 等で残った速度 modifier を念のため外す。 */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        release(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        release(event.getPlayer());
        movementLock.forget(event.getPlayer().getUniqueId());
    }

    // ───────────────────────── 公開ヘルパー（コマンドから利用） ─────────────────────────

    public void reloadAll() {
        loadSettings();
        releaseAll();
        startTask();
    }

    public boolean isActive() {
        return active;
    }

    public long getThresholdMillis() {
        return thresholdMillis;
    }

    public Display getDisplay() {
        return display;
    }

    public boolean isBlockMovement() {
        return blockMovement;
    }
}
