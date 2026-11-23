package com.velorise.simplemap;

import com.velorise.simplemap.item.EmptyMapBookItem;
import com.velorise.simplemap.item.MapBookItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

import java.util.function.Supplier;

public final class ModItems {
    private static final ResourceKey<Item> EMPTY_MAP_BOOK_KEY = ResourceKey.create(
            Registries.ITEM, Identifier.fromNamespaceAndPath(SimpleMap.MODID, "empty_book_map"));
    private static final ResourceKey<Item> MAP_BOOK_KEY = ResourceKey.create(
            Registries.ITEM, Identifier.fromNamespaceAndPath(SimpleMap.MODID, "book_map"));

    private static final EmptyMapBookItem EMPTY_MAP_BOOK_INSTANCE = Registry.register(
            BuiltInRegistries.ITEM,
            EMPTY_MAP_BOOK_KEY,
            new EmptyMapBookItem(new Item.Properties().setId(EMPTY_MAP_BOOK_KEY).stacksTo(1)));

    private static final MapBookItem MAP_BOOK_INSTANCE = Registry.register(
            BuiltInRegistries.ITEM,
            MAP_BOOK_KEY,
            new MapBookItem(new Item.Properties().setId(MAP_BOOK_KEY).stacksTo(1)));

    public static final Supplier<EmptyMapBookItem> EMPTY_MAP_BOOK = () -> EMPTY_MAP_BOOK_INSTANCE;
    public static final Supplier<MapBookItem> MAP_BOOK = () -> MAP_BOOK_INSTANCE;

    private ModItems() {}

    public static void register() {}
}
