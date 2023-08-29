package dev.ov4client.addon.modules.misc;

import dev.ov4client.addon.ov4client;
import dev.ov4client.addon.ov4Module;
import dev.ov4client.addon.utils.player.InventoryUtils;
import meteordevelopment.meteorclient.events.entity.player.FinishUsingItemEvent;
import meteordevelopment.meteorclient.events.entity.player.StoppedUsingItemEvent;
import meteordevelopment.meteorclient.events.meteor.MouseButtonEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.BowItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_MIDDLE;

public class MidClickExtra extends ov4Module {
    public MidClickExtra() {
        super(ov4client.Combat, "Mid Click Extra", "Lets you use items when you middle click.");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // General
    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("Mode")
        .description("Which item to use when you middle click.")
        .defaultValue(Mode.Pearl)
        .build()
    );
    private final Setting<SwitchMode> switchMode = sgGeneral.add(new EnumSetting.Builder<SwitchMode>()
        .name("Switch Mode")
        .defaultValue(SwitchMode.InvSwitch)
        .build()
    );
    private final Setting<Boolean> noInventory = sgGeneral.add(new BoolSetting.Builder()
        .name("Anti-inventory")
        .description("Not work in inventory.")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> notify = sgGeneral.add(new BoolSetting.Builder()
        .name("notify")
        .description("Notifies you when you do not have the specified item in your hotbar.")
        .defaultValue(true)
        .build()
    );

    private boolean isUsing;

    private enum Type {
        Immediate,
        LongerSingleClick,
        Longer
    }

    public enum Mode {
        Pearl(Items.ENDER_PEARL, Type.Immediate),
        Rocket(Items.FIREWORK_ROCKET, Type.Immediate),

        Rod(Items.FISHING_ROD, Type.LongerSingleClick),

        Bow(Items.BOW, Type.Longer),
        Gap(Items.GOLDEN_APPLE, Type.Longer),
        EGap(Items.ENCHANTED_GOLDEN_APPLE, Type.Longer),
        Chorus(Items.CHORUS_FRUIT, Type.Longer);

        private final Item item;
        private final Type type;

        Mode(Item item, Type type) {
            this.item = item;
            this.type = type;
        }
    }

    public enum SwitchMode {
        Silent,
        PickSilent,
        InvSwitch
    }

    @Override
    public void onDeactivate() {
        stopIfUsing();
    }

    @EventHandler
    private void onMouseButton(MouseButtonEvent event) {
        if (event.action != KeyAction.Press || event.button != GLFW_MOUSE_BUTTON_MIDDLE) return;
        if (noInventory.get() && mc.currentScreen != null) return;

        FindItemResult result = InvUtils.findInHotbar(mode.get().item);
        FindItemResult invResult = InvUtils.find(mode.get().item);

        if (!switchMode.get().equals(SwitchMode.InvSwitch)) {
            if (!result.found()) {
                if (notify.get()) warning("Unable to find specified item.");
                return;
            }
        } else {
            if (!invResult.found()) {
                if (notify.get()) warning("Unable to find specified item.");
                return;
            }
        }

        switch (switchMode.get()) {
            case Silent -> InvUtils.swap(result.slot(), true);
            case InvSwitch -> InventoryUtils.invSwitch(invResult.slot());
            case PickSilent -> InventoryUtils.pickSwitch(result.slot());
        }

        switch (mode.get().type) {
            case Immediate -> {
                if (mc.interactionManager != null){
                    mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                    InvUtils.swapBack();
                }
            }
            case LongerSingleClick ->{
                if (mc.interactionManager != null){
                    mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                }
            }
            case Longer -> {
                mc.options.useKey.setPressed(true);
                isUsing = true;
            }
        }

        switch (switchMode.get()) {
            case Silent -> InvUtils.swapBack();
            case PickSilent -> InventoryUtils.pickSwapBack();
            case InvSwitch -> InventoryUtils.swapBack();
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (isUsing) {
            boolean pressed = true;

            if (mc.player != null && mc.player.getMainHandStack().getItem() instanceof BowItem) {
                pressed = BowItem.getPullProgress(mc.player.getItemUseTime()) < 1;
            }

            mc.options.useKey.setPressed(pressed);
        }
    }

    @EventHandler
    private void onFinishUsingItem(FinishUsingItemEvent event) {
        stopIfUsing();
    }

    @EventHandler
    private void onStoppedUsingItem(StoppedUsingItemEvent event) {
        stopIfUsing();
    }

    private void stopIfUsing() {
        if (isUsing) {
            mc.options.useKey.setPressed(false);
            InvUtils.swapBack();
            isUsing = false;
        }
    }
}
