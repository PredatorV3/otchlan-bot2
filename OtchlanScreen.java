package pl.otchlanbot;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class OtchlanScreen extends Screen {

	private final OtchlanBot bot;

	public OtchlanScreen(OtchlanBot bot) {
		super(Text.literal("OtchlanBot"));
		this.bot = bot;
	}

	private Text label() {
		return Text.literal(bot.isEnabled() ? "Mod: WŁĄCZONY" : "Mod: WYŁĄCZONY");
	}

	@Override
	protected void init() {
		addDrawableChild(ButtonWidget.builder(label(), b -> {
			bot.setEnabled(!bot.isEnabled());
			b.setMessage(label());
		}).dimensions(width / 2 - 70, height / 2 - 10, 140, 20).build());

		addDrawableChild(ButtonWidget.builder(Text.literal("Zamknij"), b -> close())
				.dimensions(width / 2 - 70, height / 2 + 16, 140, 20).build());
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		context.drawCenteredTextWithShadow(textRenderer, Text.literal("OtchlanBot"),
				width / 2, height / 2 - 35, 0xFFFFFFFF);
	}
}
