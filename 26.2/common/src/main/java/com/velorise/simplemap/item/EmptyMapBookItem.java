package com.velorise.simplemap.item;

import com.velorise.simplemap.MapBookHooks;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class EmptyMapBookItem extends Item {
    public EmptyMapBookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            MapBookHooks.useEmptyBook(player, hand, this);
        }
        return InteractionResult.SUCCESS;
    }
}
