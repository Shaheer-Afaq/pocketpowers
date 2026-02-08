package pocketutils;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import java.util.function.Function;

public class ModItems {

     public static final Item HEALTH_GEM = register(
            "health_gem",properties -> new GemItem(properties, MobEffects.HEALTH_BOOST, 1, "health", 500), new Item.Properties().stacksTo(1).fireResistant());

     public static final Item MINING_GEM = register(
            "mining_gem",properties -> new GemItem(properties, MobEffects.HASTE,1, "mining", 400), new Item.Properties().stacksTo(1).fireResistant());

    public static final Item GRAVITY_GEM = register(
            "gravity_gem",properties -> new GemItem(properties, null,0, "gravity", 500), new Item.Properties().stacksTo(1).fireResistant());

    public static final Item MAGIC_GEM = register(
            "magic_gem",properties -> new GemItem(properties, null,0, "magic", 500), new Item.Properties().stacksTo(1).fireResistant().useCooldown(100));

    public static final Item FIRE_GEM = register(
            "fire_gem",properties -> new GemItem(properties, MobEffects.FIRE_RESISTANCE,0, "fire", 300), new Item.Properties().stacksTo(1).fireResistant().useCooldown(160));

    public static void initialize() {
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
                .register(itemGroup -> {
                    itemGroup.accept(ModItems.HEALTH_GEM);
                    itemGroup.accept(ModItems.MINING_GEM);
                    itemGroup.accept(ModItems.GRAVITY_GEM);
                    itemGroup.accept(ModItems.MAGIC_GEM);
                    itemGroup.accept(ModItems.FIRE_GEM);
                });
        GemEvents.initialize();

    }
    public static <GenericItem extends Item> GenericItem register(String name, Function<Item.Properties, GenericItem> itemFactory, Item.Properties settings) {
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(PocketUtils.MOD_ID, name));
        GenericItem item = itemFactory.apply(settings.setId(itemKey));
        Registry.register(BuiltInRegistries.ITEM, itemKey, item);
        return item;
    }
}
