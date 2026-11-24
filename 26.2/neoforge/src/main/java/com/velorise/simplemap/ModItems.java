package com.velorise.simplemap;

import com.velorise.simplemap.item.EmptyMapBookItem;
import com.velorise.simplemap.item.MapBookItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SimpleMap.MODID);

    public static final DeferredItem<EmptyMapBookItem> EMPTY_MAP_BOOK = ITEMS.registerItem(
            "empty_book_map", props -> new EmptyMapBookItem(props.stacksTo(1)));

    public static final DeferredItem<MapBookItem> MAP_BOOK = ITEMS.registerItem(
            "book_map", props -> new MapBookItem(props.stacksTo(1)));
}
