package pocketutils;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.network.chat.Component;

@Environment(EnvType.CLIENT)
public class PocketutilsClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		ItemTooltipCallback.EVENT.register((itemStack, tooltipContext, tooltipType, list) -> {
			if (itemStack.getItem() instanceof GemItem item) {
				String lines = Component.translatable("item.pocketutils." + item.name + ".tooltip").getString();
				for (String line : lines.split("\n")) {
					list.add(Component.literal(line));
				}
			}
		});
	}
}