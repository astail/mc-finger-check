package io.github.astail.fingercheck;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 違反中のプレイヤーをその場から動けなくする。
 * 移動速度 attribute を 0 倍にしてクライアント側でも止める（カクつかない）。
 * ジャンプ中の空中移動は attribute では止まらないので、{@link #clamp} で水平移動を打ち消す。
 */
final class MovementLock {

    private final NamespacedKey key;
    private final Set<UUID> locked = new HashSet<>();

    MovementLock(Plugin plugin) {
        this.key = new NamespacedKey(plugin, "sprint_lock");
    }

    boolean isLocked(UUID id) {
        return locked.contains(id);
    }

    void lock(Player player) {
        if (!locked.add(player.getUniqueId())) {
            return;
        }
        AttributeInstance speed = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speed != null && speed.getModifier(key) == null) {
            // MULTIPLY_SCALAR_1: 値 ×(1 + amount)。-1 で 0 倍。transient なのでプレイヤーデータには保存されない。
            speed.addTransientModifier(new AttributeModifier(key, -1.0, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
        }
    }

    void release(Player player) {
        locked.remove(player.getUniqueId());
        AttributeInstance speed = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speed != null && speed.getModifier(key) != null) {
            speed.removeModifier(key);
        }
    }

    /** ロック中なら水平移動だけ打ち消す（落下・視点移動はそのまま）。 */
    void clamp(PlayerMoveEvent event) {
        if (!locked.contains(event.getPlayer().getUniqueId())) {
            return;
        }
        Location from = event.getFrom();
        Location to = event.getTo();
        if (from.getX() == to.getX() && from.getZ() == to.getZ()) {
            return;
        }
        Location fixed = to.clone();
        fixed.setX(from.getX());
        fixed.setZ(from.getZ());
        event.setTo(fixed);
    }

    void forget(UUID id) {
        locked.remove(id);
    }
}
