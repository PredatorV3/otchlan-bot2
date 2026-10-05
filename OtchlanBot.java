package pl.otchlanbot;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.LinkedHashMap;
import java.util.Map;

public class OtchlanBot implements ClientModInitializer {

	static final int CLICK_DELAY_TICKS = 2;
	static final int REFRESH_DELAY_TICKS = 10;
	static final int OPEN_TIMEOUT_TICKS = 100;
	static final int MAX_EMPTY_ROUNDS = 3;
	static final boolean THROW_WHOLE_STACK = true;

	private boolean enabled = true;
	private boolean armed = false;
	private boolean seenGui = false;
	private int delay = 0;
	private int waitedForOpen = 0;
	private int emptyRounds = 0;
	private boolean threwSinceDye = false;

	private KeyBinding openGuiKey;

	public boolean isEnabled() { return enabled; }

	public void setEnabled(boolean value) {
		enabled = value;
		if (!value) armed = false;
		chat(MinecraftClient.getInstance(), value ? "mod wlaczony" : "mod wylaczony");
	}

	@Override
	public void onInitializeClient() {
		KeyBinding.Category category = KeyBinding.Category.create(Identifier.of("otchlanbot", "main"));
		openGuiKey = KeyBindingHelper.registerKeyBinding(
				new KeyBinding("key.otchlanbot.gui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_O, category));

		ClientSendMessageEvents.COMMAND.register(cmd -> {
			String c = cmd.trim().toLowerCase();
			if (c.equals("otchlan") || c.startsWith("otchlan ")) {
				if (!enabled) {
					chat(MinecraftClient.getInstance(), "mod jest wylaczony (wlacz go pod klawiszem O)");
					return;
				}
				armed = true;
				seenGui = false;
				delay = 0;
				waitedForOpen = 0;
				emptyRounds = 0;
				threwSinceDye = false;
				chat(MinecraftClient.getInstance(), "komenda wykryta, czekam na menu");
			}
		});
		ClientTickEvents.END_CLIENT_TICK.register(this::tick);
	}

	private void tick(MinecraftClient mc) {
		while (openGuiKey.wasPressed()) {
			if (mc.currentScreen == null) mc.setScreen(new OtchlanScreen(this));
		}

		if (!enabled || !armed || mc.player == null || mc.interactionManager == null) return;

		boolean isMenu = mc.currentScreen instanceof GenericContainerScreen;

		if (!isMenu) {
			if (seenGui) { stop(mc, "menu zamkniete, stop"); return; }
			if (++waitedForOpen > OPEN_TIMEOUT_TICKS) stop(mc, "menu sie nie otworzylo (to nie jest zwykly chest GUI?)");
			return;
		}

		GenericContainerScreen screen = (GenericContainerScreen) mc.currentScreen;
		GenericContainerScreenHandler handler = screen.getScreenHandler();
		int chestSlots = handler.getRows() * 9;

		if (!seenGui) {
			seenGui = true;
			delay = 5;
			chat(mc, "menu: " + screen.getTitle().getString() + " | rzedy: " + handler.getRows());
			Map<String, Integer> seen = new LinkedHashMap<>();
			for (int i = 0; i < chestSlots; i++) {
				ItemStack st = handler.getSlot(i).getStack();
				if (st.isEmpty() || isDecoration(st)) continue;
				seen.putIfAbsent(id(st), i);
			}
			StringBuilder sb = new StringBuilder("itemy (id@slot): ");
			seen.forEach((k, v) -> sb.append(k).append("@").append(v).append(", "));
			chat(mc, sb.toString());
			return;
		}

		if (delay > 0) { delay--; return; }

		for (int i = 0; i < chestSlots; i++) {
			ItemStack st = handler.getSlot(i).getStack();
			if (st.isEmpty() || isDecoration(st) || isDye(st)) continue;
			mc.interactionManager.clickSlot(handler.syncId, i, THROW_WHOLE_STACK ? 1 : 0,
					SlotActionType.THROW, mc.player);
			threwSinceDye = true;
			delay = CLICK_DELAY_TICKS;
			return;
		}

		for (int i = 0; i < chestSlots; i++) {
			ItemStack st = handler.getSlot(i).getStack();
			if (!st.isEmpty() && isDye(st)) {
				if (!threwSinceDye && ++emptyRounds >= MAX_EMPTY_ROUNDS) {
					stop(mc, "nic wiecej do wyrzucenia, koniec");
					return;
				}
				chat(mc, "klikam barwnik w slocie " + i);
				mc.interactionManager.clickSlot(handler.syncId, i, 0, SlotActionType.PICKUP, mc.player);
				threwSinceDye = false;
				delay = REFRESH_DELAY_TICKS;
				return;
			}
		}

		stop(mc, "nie znalazlem barwnika (sprawdz liste itemow wyzej)");
	}

	private static String id(ItemStack st) {
		return Registries.ITEM.getId(st.getItem()).getPath();
	}

	private static boolean isDecoration(ItemStack st) {
		return id(st).endsWith("glass_pane");
	}

	private static boolean isDye(ItemStack st) {
		String p = id(st);
		return p.equals("lime_dye") || p.equals("green_dye");
	}

	private void chat(MinecraftClient mc, String msg) {
		if (mc != null && mc.player != null) mc.player.sendMessage(Text.literal("[OB] " + msg), false);
	}

	private void stop(MinecraftClient mc, String msg) {
		armed = false;
		chat(mc, msg);
	}
}
