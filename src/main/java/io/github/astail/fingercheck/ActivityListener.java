package io.github.astail.fingercheck;

import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

import java.util.function.Consumer;

/**
 * 「歩く以外の行動」を検知してルール 1 のタイマーをリセットする。
 * ルールは「歩くだけを 2 秒以上続けたら」なので、ブロックを置く・殴る・アイテムを使う等をしたら 0 秒からやり直し
 * （ロック中なら解除）。ジャンプは入力状態なので {@link SprintRule} 側で見る。
 * キャンセルされた行動（保護区域での設置など）も、プレイヤーが操作したことには変わりないので数える。
 */
final class ActivityListener implements Listener {

    private final Consumer<Player> onActivity;

    ActivityListener(Consumer<Player> onActivity) {
        this.onActivity = onActivity;
    }

    /** 腕を振る（左クリック全般: 殴る・掘り始め・空振り）。 */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onSwing(PlayerAnimationEvent event) {
        onActivity.accept(event.getPlayer());
    }

    /** ブロック・空中へのクリック（右クリックでの使用・設置など）。感圧板を踏む PHYSICAL は歩行なので除外。 */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.PHYSICAL) {
            onActivity.accept(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlace(BlockPlaceEvent event) {
        onActivity.accept(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        onActivity.accept(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAttack(PrePlayerAttackEntityEvent event) {
        onActivity.accept(event.getPlayer());
    }

    /** ホットバーの持ち替え。 */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onHeld(PlayerItemHeldEvent event) {
        onActivity.accept(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDrop(PlayerDropItemEvent event) {
        onActivity.accept(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSwap(PlayerSwapHandItemsEvent event) {
        onActivity.accept(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryOpen(InventoryOpenEvent event) {
        HumanEntity human = event.getPlayer();
        if (human instanceof Player player) {
            onActivity.accept(player);
        }
    }
}
