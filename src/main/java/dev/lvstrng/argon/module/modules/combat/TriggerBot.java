package dev.lvstrng.argon.module.modules.combat;

import dev.lvstrng.argon.Argon;
import dev.lvstrng.argon.event.events.AttackListener;
import dev.lvstrng.argon.event.events.TickListener;
import dev.lvstrng.argon.module.Category;
import dev.lvstrng.argon.module.Module;
import dev.lvstrng.argon.module.setting.BooleanSetting;
import dev.lvstrng.argon.module.setting.MinMaxSetting;
import dev.lvstrng.argon.module.setting.NumberSetting;
import dev.lvstrng.argon.utils.EncryptedString;
import dev.lvstrng.argon.utils.MouseSimulation;
import dev.lvstrng.argon.utils.TimerUtils;
import dev.lvstrng.argon.utils.WorldUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.lwjgl.glfw.GLFW;

public final class TriggerBot extends Module implements TickListener, AttackListener {
    private final BooleanSetting inScreen = new BooleanSetting(EncryptedString.of("Work In Screen"), false)
            .setDescription(EncryptedString.of("Will trigger even if youre inside a screen"));

    private final BooleanSetting whileUse = new BooleanSetting(EncryptedString.of("While Use"), false)
            .setDescription(EncryptedString.of("Will hit the player no matter if you're eating or blocking with a shield"));

    private final BooleanSetting onLeftClick = new BooleanSetting(EncryptedString.of("On Left Click"), false)
            .setDescription(EncryptedString.of("Only gets triggered if holding down left click"));

    private final BooleanSetting allItems = new BooleanSetting(EncryptedString.of("All Items"), false)
            .setDescription(EncryptedString.of("Works with all Items /THIS USES SWORD DELAY AS THE DELAY/"));

    private final MinMaxSetting swordDelay = new MinMaxSetting(EncryptedString.of("Sword Delay"), 0.0, 1000.0, 250.0, 1.0, 50.0)
            .setDescription(EncryptedString.of("Delay for swords"));

    private final MinMaxSetting axeDelay = new MinMaxSetting(EncryptedString.of("Axe Delay"), 0.0, 1000.0, 250.0, 1.0, 50.0)
            .setDescription(EncryptedString.of("Delay for axes"));

    private final BooleanSetting checkShield = new BooleanSetting(EncryptedString.of("Check Shield"), true)
            .setDescription(EncryptedString.of("Checks if the player is blocking your hits with a shield (Recommended with Shield Disabler)"));

    private final BooleanSetting onlyCritSword = new BooleanSetting(EncryptedString.of("Only Crit Sword"), false)
            .setDescription(EncryptedString.of("Only does critical hits with a sword"));

    private final BooleanSetting onlyCritAxe = new BooleanSetting(EncryptedString.of("Only Crit Axe"), false)
            .setDescription(EncryptedString.of("Only does critical hits with an axe"));

    private final BooleanSetting swing = new BooleanSetting(EncryptedString.of("Swing Hand"), true)
            .setDescription(EncryptedString.of("Whether to swing the hand or not"));

    private final BooleanSetting whileAscend = new BooleanSetting(EncryptedString.of("While Ascending"), true)
            .setDescription(EncryptedString.of("Wont hit if you're ascending from a jump, only if on ground or falling"));

    private final BooleanSetting clickSimulation = new BooleanSetting(EncryptedString.of("Click Simulation"), false)
            .setDescription(EncryptedString.of("Makes the CPS hud think you're legit"));

    private final BooleanSetting strayBypass = new BooleanSetting(EncryptedString.of("Stray Bypass"), false)
            .setDescription(EncryptedString.of("Bypasses stray's Anti-TriggerBot"));

    private final BooleanSetting allEntities = new BooleanSetting(EncryptedString.of("All Entities"), false)
            .setDescription(EncryptedString.of("Will attack all entities"));

    private final BooleanSetting useShield = new BooleanSetting(EncryptedString.of("Use Shield"), false)
            .setDescription(EncryptedString.of("Uses shield if it's in your offhand"));

    private final NumberSetting shieldTime = new NumberSetting(EncryptedString.of("Shield Time"), 0.0, 1000.0, 250.0, 1.0);

    private final BooleanSetting sticky = new BooleanSetting(EncryptedString.of("Same Player"), false)
            .setDescription(EncryptedString.of("Hits the player that was recently attacked, good for FFA"));

    private final TimerUtils timer = new TimerUtils();

    private int currentSwordDelay;
    private int currentAxeDelay;
    private Entity targetEntity;

    public TriggerBot() {
        super(EncryptedString.of("Trigger Bot"),
                EncryptedString.of("Automatically hits players for you"),
                -1,
                Category.COMBAT);

        addSettings(inScreen, whileUse, onLeftClick, allItems, swordDelay, axeDelay, checkShield, onlyCritSword, onlyCritAxe, swing, whileAscend, clickSimulation, strayBypass, allEntities, useShield, shieldTime, sticky);
    }

    @Override
    public void onEnable() {
        currentSwordDelay = (int) swordDelay.getRandomValueFloat();
        currentAxeDelay = (int) axeDelay.getRandomValueFloat();
        timer.reset();
        eventManager.add(TickListener.class, this);
        eventManager.add(AttackListener.class, this);
        super.onEnable();
    }

    @Override
    public void onDisable() {
        eventManager.remove(TickListener.class, this);
        eventManager.remove(AttackListener.class, this);
        super.onDisable();
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null)
            return;

        if (!inScreen.getValue() && mc.currentScreen != null)
            return;

        if (onLeftClick.getValue() && GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) != GLFW.GLFW_PRESS)
            return;

        if (!whileUse.getValue() && mc.player.isUsingItem())
            return;

        if (!whileAscend.getValue() && mc.player.getVelocity().y > 0.0 && !mc.player.isOnGround())
            return;

        boolean isSword = WorldUtils.isSword(mc.player.getMainHandStack());
        boolean isAxe = WorldUtils.isAxe(mc.player.getMainHandStack());

        if (!allItems.getValue() && !isSword && !isAxe)
            return;

        HitResult hitResult = mc.crosshairTarget;
        if (hitResult == null || hitResult.getType() != HitResult.Type.ENTITY)
            return;

        Entity entity = ((EntityHitResult) hitResult).getEntity();
        if (entity == null || !entity.isAlive())
            return;

        if (!allEntities.getValue() && !(entity instanceof PlayerEntity))
            return;

        if (entity instanceof PlayerEntity player && Argon.INSTANCE.getModuleManager().getModule(dev.lvstrng.argon.module.modules.client.Friends.class).isEnabled() && Argon.INSTANCE.getFriendManager().isAimingOverFriend())
            return;

        if (checkShield.getValue() && entity instanceof PlayerEntity playerEntity && WorldUtils.isShieldFacingAway(playerEntity))
            return;

        if (sticky.getValue() && targetEntity != null && targetEntity != entity)
            return;

        int delay = isSword ? currentSwordDelay : currentAxeDelay;
        if (timer.delay(delay)) {
            if (clickSimulation.getValue()) {
                MouseSimulation.mouseClick(GLFW.GLFW_MOUSE_BUTTON_LEFT);
            }

            if (onlyCritSword.getValue() && isSword && (mc.player.fallDistance <= 0.0 || mc.player.isOnGround()))
                return;

            if (onlyCritAxe.getValue() && isAxe && (mc.player.fallDistance <= 0.0 || mc.player.isOnGround()))
                return;

            mc.interactionManager.attackEntity(mc.player, entity);
            if (swing.getValue()) {
                mc.player.swingHand(mc.player.getActiveHand());
            }

            if (clickSimulation.getValue()) {
                MouseSimulation.mouseRelease(GLFW.GLFW_MOUSE_BUTTON_LEFT);
            }

            currentSwordDelay = (int) swordDelay.getRandomValueFloat();
            currentAxeDelay = (int) axeDelay.getRandomValueFloat();
            timer.reset();
        }
    }

    @Override
    public void onAttack(AttackEvent event) {
        if (event.target != null) {
            targetEntity = event.target;
        }
    }
}
