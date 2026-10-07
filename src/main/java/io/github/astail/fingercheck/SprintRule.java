package io.github.astail.fingercheck;

import org.bukkit.GameMode;
import org.bukkit.Input;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * ルール 1「走っていないと動けない」。
 * 前進キー（W）を押しているのに走っていない状態が一定時間（既定 2 秒）続いたら注意を出す。
 * スニーク（ゆっくり歩き）中や、そもそも走れない状況（飛行・水中・空腹など）は対象外。
 */
final class SprintRule {

    /** プレイヤーごとの「走らずに前進し始めた時刻」（epoch ミリ秒）。条件が崩れたら消す。 */
    private final Map<UUID, Long> walkingSince = new HashMap<>();
    /** プレイヤーごとの「最後に注意を出した時刻」。アクションバーの再表示間隔の管理に使う。 */
    private final Map<UUID, Long> lastWarnedAt = new HashMap<>();

    /** update の結果。FIRST は今回の違反で初めての注意、REPEAT は継続中の再表示。 */
    enum Warning { NONE, FIRST, REPEAT }

    /**
     * 1 プレイヤー分の状態を更新し、注意を出すべきかを返す。
     * 違反が {@code thresholdMillis} 続いたら FIRST、その後も続いていれば {@code repeatMillis} ごとに REPEAT。
     */
    Warning update(Player player, long now, long thresholdMillis, long repeatMillis) {
        UUID id = player.getUniqueId();
        if (!isWalkingWithoutSprint(player)) {
            forget(id);
            return Warning.NONE;
        }
        long since = walkingSince.computeIfAbsent(id, k -> now);
        if (now - since < thresholdMillis) {
            return Warning.NONE;
        }
        Long last = lastWarnedAt.get(id);
        if (last != null && now - last < repeatMillis) {
            return Warning.NONE;
        }
        lastWarnedAt.put(id, now);
        return last == null ? Warning.FIRST : Warning.REPEAT;
    }

    /** 注意済み（＝違反が threshold を超えて継続中）か。移動ロックの対象判定に使う。 */
    boolean isWarned(UUID id) {
        return lastWarnedAt.containsKey(id);
    }

    /** 新しいキー入力が違反の解除条件（W を離す・走るキー・スニーク）を満たすか。 */
    static boolean releases(Input input) {
        return !input.isForward() || input.isSprint() || input.isSneak();
    }

    void forget(UUID id) {
        walkingSince.remove(id);
        lastWarnedAt.remove(id);
    }

    void clear() {
        walkingSince.clear();
        lastWarnedAt.clear();
    }

    /**
     * 「W を押しているのに走っていない」かどうか。
     * 走れない／走る必要がない状況は false（＝注意しない）にして、誤検知を避ける。
     */
    static boolean isWalkingWithoutSprint(Player player) {
        GameMode mode = player.getGameMode();
        if (mode != GameMode.SURVIVAL && mode != GameMode.ADVENTURE) {
            return false;
        }
        Input input = player.getCurrentInput();
        if (!input.isForward()) {
            return false;
        }
        // 走っている、または走るキーを押している（壁に当たってダッシュが切れた等）なら OK。
        if (player.isSprinting() || input.isSprint()) {
            return false;
        }
        // スニークでゆっくり歩いているときは対象外。
        if (input.isSneak() || player.isSneaking()) {
            return false;
        }
        // 飛行・滑空・騎乗・水中/溶岩・はしご等は走る/歩くの区別が無い（または走れない）ので対象外。
        if (player.isFlying() || player.isGliding() || player.isInsideVehicle()
                || player.isSwimming() || player.isInWater() || player.isInLava()
                || player.isClimbing() || player.isSleeping() || player.isDead()) {
            return false;
        }
        // バニラで走れない条件: 満腹度 6 以下、盲目、アイテム使用中（食事・弓を引く・盾構え等）。
        if (player.getFoodLevel() <= 6 || player.hasPotionEffect(PotionEffectType.BLINDNESS)
                || player.isHandRaised()) {
            return false;
        }
        return true;
    }
}
