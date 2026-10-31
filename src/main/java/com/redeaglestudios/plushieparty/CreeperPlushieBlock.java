package com.redeaglestudios.plushieparty;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.RenderShape;

import java.util.List;

/**
 * The invisible hitbox block that hosts the plushie.
 *
 * Faithful port of re_plushie_party:creeper_plushie_hitbox:
 * - placement sets minecraft:cardinal_direction (faces the placer)
 * - after placement the script spawns the doll entity at the block center,
 *   with YAW_BY_DIRECTION { south:180, north:0, east:90, west:270 }
 * - placed state gives a 10x10x10 (pixel) collision + selection box centered on the block
 * - 0.5s to mine with a pickaxe/axe, not destructible by explosions, immovable
 * - breaking the block removes the doll; interacting squeezes it and toggles the party
 * - the block itself is invisible (blank texture on Bedrock) and shows the model as item
 */
public class CreeperPlushieBlock extends Block {
    /** Bedrock permutation placed==1: collision/selection box origin [-5,0,-5] size [10,10,10]. */
    private static final VoxelShape HITBOX_SHAPE = Shapes.box(
        3.0 / 16.0, 0.0, 3.0 / 16.0,
        13.0 / 16.0, 10.0 / 16.0, 13.0 / 16.0
    );

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public CreeperPlushieBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Bedrock placement_direction: the block faces the placer.
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    // ------------------------------------------------------------- visuals
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;   // blank texture on Bedrock -> invisible in world
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return HITBOX_SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // Entities (the doll itself) must not collide with their own hitbox.
        if (context instanceof EntityCollisionContext entityContext
                && entityContext.getEntity() instanceof CreeperPlushieEntity) {
            return Shapes.empty();
        }
        return HITBOX_SHAPE;
    }

    @Override
    protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0f;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }

    // -------------------------------------------------------- entity sync
    /** Bedrock findPlushie(): entity of the right type whose block position matches the block. */
    private CreeperPlushieEntity findPlushie(Level level, BlockPos pos) {
        AABB box = new AABB(pos).inflate(0.5);
        List<Entity> found = level.getEntitiesOfClass(Entity.class, box, e -> e instanceof CreeperPlushieEntity);
        for (Entity e : found) {
            BlockPos ep = e.blockPosition();
            if (ep.getX() == pos.getX() && ep.getY() == pos.getY() && ep.getZ() == pos.getZ()) {
                return (CreeperPlushieEntity) e;
            }
        }
        return null;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level instanceof ServerLevel server && !movedByPiston) {
            // Only spawn if there is no doll yet (prevents duplicates on chunk reload).
            if (findPlushie(level, pos) == null) {
                CreeperPlushieEntity plushie = ModRegistry.CREEPER_PLUSHIE.create(server,
                    EntitySpawnReason.EVENT);
                if (plushie != null) {
                    // Bedrock: spawn at block center (x+0.5, y, z+0.5) with YAW_BY_DIRECTION.
                    plushie.absSnapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                        yawFor(state.getValue(FACING)), 0.0f);
                    plushie.setPersistenceRequired();
                    server.addFreshEntity(plushie);
                    // Bedrock entity_spawned adds re_event:variant0 -> the doll squeezes once on spawn.
                    plushie.squeeze();
                }
            }
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        CreeperPlushieEntity plushie = findPlushie(level, pos);
        if (plushie != null) {
            PartyManager.get().onPlushieRemoved(plushie);
            plushie.discard();
        }
    }

    // ---------------------------------------------------------- interaction
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server && player instanceof ServerPlayer serverPlayer) {
            CreeperPlushieEntity plushie = findPlushie(level, pos);
            if (plushie != null) {
                // Bedrock: every click squeezes (re_event:variant0)...
                plushie.squeeze();
                // ...and toggles the party (stick / any music disc / active party stops it).
                PartyManager.get().toggleParty(serverPlayer, plushie, server);
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** Bedrock YAW_BY_DIRECTION {south:180, north:0, east:90, west:270} converted to Java yaw. */
    public static float yawFor(Direction facing) {
        return switch (facing) {
            case SOUTH -> 0.0f;    // bedrock 180
            case NORTH -> 180.0f;  // bedrock 0
            case EAST -> 270.0f;   // bedrock 90
            case WEST -> 90.0f;    // bedrock 270
            default -> 0.0f;
        };
    }
}
