package draylar.tiered.block;

import draylar.tiered.reforge.ReforgeScreenHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class ReforgeBlock extends Block {

    public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;

    // Formato customizado (opcional, mas recomendado para blocos tipo bigorna não serem cubos gigantes)
    private static final VoxelShape SHAPE = Block.createCuboidShape(0.0, 0.0, 2.0, 16.0, 16.0, 14.0);

    public ReforgeBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.NORTH));
    }

    // 🌟 3. REGISTRANDO A PROPRIEDADE NO BLOCO
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    // 🌟 4. GIRANDO O BLOCO QUANDO O JOGADOR COLOCA NO CHÃO
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        // Pega a direção que o jogador está olhando e inverte (para o bloco olhar para o jogador)
        return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    // 🌟 5. APLICANDO O FORMATO (Hitbox)
    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    // Quando o jogador clica com o botão direito na mesa
    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient()) {
            NamedScreenHandlerFactory screenHandlerFactory = state.createScreenHandlerFactory(world, pos);
            if (screenHandlerFactory != null) {
                // Abre a interface gráfica!
                player.openHandledScreen(screenHandlerFactory);
            }
        }
        return ActionResult.SUCCESS;
    }

    // Conecta o Bloco Físico ao seu ReforgeScreenHandler!
    @Nullable
    @Override
    protected NamedScreenHandlerFactory createScreenHandlerFactory(BlockState state, World world, BlockPos pos) {
        return new SimpleNamedScreenHandlerFactory(
                (syncId, inventory, player) -> new ReforgeScreenHandler(syncId, inventory, ScreenHandlerContext.create(world, pos)),
                Text.translatable("container.tiered.reforge")
        );
    }
}