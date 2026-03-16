package draylar.tiered.block;

import draylar.tiered.reforge.MagicStationScreenHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class MagicReforgeBlock extends Block {

    public MagicReforgeBlock(Settings settings) {
        super(settings);
    }

    // 🌟 O GATILHO: Quando o jogador clica com o botão direito na mesa
    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient()) {
            NamedScreenHandlerFactory screenHandlerFactory = state.createScreenHandlerFactory(world, pos);
            if (screenHandlerFactory != null) {
                // Abre a interface gráfica no lado do servidor!
                player.openHandledScreen(screenHandlerFactory);
            }
        }
        return ActionResult.SUCCESS;
    }

    // 🌟 A FÁBRICA: Conecta o Bloco Físico ao seu MagicStationScreenHandler
    @Nullable
    @Override
    protected NamedScreenHandlerFactory createScreenHandlerFactory(BlockState state, World world, BlockPos pos) {
        return new SimpleNamedScreenHandlerFactory(
                (syncId, inventory, player) -> new MagicStationScreenHandler(syncId, inventory, ScreenHandlerContext.create(world, pos)),
                Text.translatable("container.tiered.magic_reforge")
        );
    }
}