package io.github.astail.fingercheck;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** /fingercheck コマンドの実処理とタブ補完。 */
public final class FingerCheckCommand implements CommandExecutor, TabCompleter {

    private final FingerCheckPlugin plugin;

    public FingerCheckCommand(FingerCheckPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendStatus(sender);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> { if (requireManage(sender)) doReload(sender); }
            case "status" -> sendStatus(sender);
            default -> sendUsage(sender);
        }
        return true;
    }

    private void doReload(CommandSender sender) {
        plugin.reloadAll();
        sender.sendMessage(ok("設定を再読み込みしました。"));
    }

    private void sendStatus(CommandSender sender) {
        sender.sendMessage(info("FingerCheck: " + (plugin.isActive() ? "ON" : "OFF")
                + " / 走らず前進 " + (plugin.getThresholdMillis() / 1000.0) + " 秒で注意"
                + " / 表示 " + plugin.getDisplay().name().toLowerCase(Locale.ROOT)
                + " / 移動ロック " + (plugin.isBlockMovement() ? "あり" : "なし")));
        if (sender.hasPermission("fingercheck.bypass")) {
            sender.sendMessage(info("あなたは fingercheck.bypass を持っているため対象外です。"));
        }
        sendUsage(sender);
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(info("/fingercheck status   - 現在の設定を表示"));
        if (sender.hasPermission("fingercheck.manage")) {
            sender.sendMessage(info("/fingercheck reload   - 設定を再読み込み"));
        }
    }

    private boolean requireManage(CommandSender sender) {
        if (sender.hasPermission("fingercheck.manage")) {
            return true;
        }
        sender.sendMessage(error("この操作（reload）はサーバー管理者のみ実行できます。"));
        return false;
    }

    private static Component ok(String text) {
        return Component.text(text, NamedTextColor.GREEN);
    }

    private static Component error(String text) {
        return Component.text(text, NamedTextColor.RED);
    }

    private static Component info(String text) {
        return Component.text(text, NamedTextColor.GRAY);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(List.of("status"));
            if (sender.hasPermission("fingercheck.manage")) {
                subs.add("reload");
            }
            String lower = args[0].toLowerCase(Locale.ROOT);
            subs.removeIf(s -> !s.startsWith(lower));
            return subs;
        }
        return List.of();
    }
}
