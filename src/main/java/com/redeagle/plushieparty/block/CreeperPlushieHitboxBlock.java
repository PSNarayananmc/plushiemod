package com.redeagle.plushieparty.block;

import com.redeagle.plushieparty.entity.CreeperPlushieEntity;
import com.redeagle.plushieparty.party.PartyManager;
import com.redeagle.plushieparty.registry.PlushieEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/**
 * The invisible hitbox block ({@code re_plushie_party:creeper_plushie_hitbox}).
 *
 * <p>Faithful port of the Bedrock block:</p>
 * <ul>
 *   <li>renders nothing in the world (Bedrock geometry is a 1/16 cube with a
 *       fully transparent texture); break particles use the plushie texture</li>
 *   <li>no collision or selection box except for the 10/16 cube
 *       ({@code placed} state), which is set immediately on placement</li>
 *   <li>immovable by pistons, explosion-proof, 0.5 s to mine, cloth sounds,
 *       white map colour</li>
 *   <li>on placement, spawns the plushie entity at the block centre, rotated
 *       to face the placing player (same yaw table as the Bedrock script:
 *       south 180, north 0, east 90, west 270)</li>
 *   <li>on break, removes the associated plushie entity (Bedrock
 *       {@code playerBreakBlock} handler)</li>
 *   <li>right-clicking (with any item, like Bedrock
 *       {@code playerInteractWithBlock}) squishes the plushie; the party
 *       toggle itself only reacts to a stick or a music disc</li>
 * </ul>
 */
public class CreeperPlushieHitboxBlock extends Block {
        public static final BooleanProperty PLACED = BooleanProperty.create("placed");
        public static final EnumProperty<Direction> FACING = net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;
        /** Bedrock permutation box: origin [-5,0,-5], size [10,10,10] (centre-relative) -> Java coords. */
        private static final VoxelShape SHAPE_PLACED = net.minecraft.world.phys.shapes.Shapes.box(
                        (8 - 5) / 16.0, 0.0, (8 - 5) / 16.0, (8 + 5) / 16.0, 10 / 16.0, (8 + 5) / 16.0);

        public CreeperPlushieHitboxBlock(Properties properties) {
                super(properties);
                this.registerDefaultState(this.stateDefinition.any()
                                .setValue(PLACED, false)
                                .setValue(FACING, Direction.NORTH));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
                builder.add(PLACED, FACING);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
                // The Bedrock script sets placed=1 immediately after placement.
                // FACING = horizontal direction back towards the placing player
                // (equivalent of the Bedrock cardinal_direction trait).
                return this.defaultBlockState()
                                .setValue(PLACED, true)
                                .setValue(FACING, context.getHorizontalDirection().getOpposite());
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
                return state.getValue(PLACED) ? SHAPE_PLACED : net.minecraft.world.phys.shapes.Shapes.empty();
        }

        @Override
        protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
                return state.getValue(PLACED) ? SHAPE_PLACED : net.minecraft.world.phys.shapes.Shapes.empty();
        }

        @Override
        public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
                super.setPlacedBy(level, pos, state, placer, stack);
                if (!level.isClientSide() && state.getValue(PLACED)) {
                        // Bedrock: spawnEntity(CREEPER_ENTITY, block.center(), initialRotation: YAW_BY_DIRECTION[cardinal])
                        float yaw = yawForFacing(state.getValue(FACING));
                        CreeperPlushieEntity plushie = PlushieEntities.CREEPER_PLUSHIE.create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
                        if (plushie != null) {
                                plushie.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                                plushie.setYRot(yaw);
                                plushie.setXRot(0.0F);
                                level.addFreshEntity(plushie);
                        } else {
                                com.redeagle.plushieparty.PlushiePartyMod.LOGGER.warn(
                                                "Creeper Plushie entity could not be created at {}", pos);
                        }
                }
        }

        /**
         * Same yaw table as the original script ({@code YAW_BY_DIRECTION}).
         * Java {@code Direction#toYRot} of the facing equals the Bedrock table
         * value for the matching cardinal direction, so both engines produce
         * identical entity rotations.
         */
        public static float yawForFacing(Direction facing) {
                return facing.toYRot();
        }

        @Override
        public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
                if (!level.isClientSide()) {
                        removePlushie(level, pos);
                }
                return super.playerWillDestroy(level, pos, state, player);
        }

        /** Bedrock {@code findPlushie}: the entity standing inside this exact block cell. */
        public static CreeperPlushieEntity findPlushie(Level level, BlockPos pos) {
                List<CreeperPlushieEntity> nearby = level.getEntitiesOfClass(
                                CreeperPlushieEntity.class,
                                new AABB(pos),
                                e -> BlockPos.containing(e.getX(), e.getY(), e.getZ()).equals(pos));
                return nearby.isEmpty() ? null : nearby.getFirst();
        }

        private static void removePlushie(Level level, BlockPos pos) {
                List<CreeperPlushieEntity> nearby = level.getEntitiesOfClass(
                                CreeperPlushieEntity.class,
                                new AABB(pos),
                                e -> BlockPos.containing(e.getX(), e.getY(), e.getZ()).equals(pos));
                for (CreeperPlushieEntity plushie : nearby) {
                        PartyManager.onEntityRemoved(plushie);
                        plushie.discard();
                }
        }

        /** Bedrock {@code playerInteractWithBlock}: squish on every click, party toggle needs stick/disc. */
        private InteractionResult interact(Level level, BlockPos pos, Player player) {
                if (level.isClientSide()) {
                        return InteractionResult.SUCCESS;
                }
                CreeperPlushieEntity plushie = findPlushie(level, pos);
                if (plushie == null) {
                        return InteractionResult.CONSUME;
                }
                plushie.squish();
                PartyManager.toggleParty(player, plushie);
                return InteractionResult.CONSUME;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
                return this.interact(level, pos, player);
        }

        @Override
        protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
                // Bedrock fires playerInteractWithBlock for any held item; the normal
                // item behaviour still runs afterwards (event is not cancelled).
                InteractionResult result = this.interact(level, pos, player);
                return result == InteractionResult.SUCCESS ? InteractionResult.PASS : result;
        }
}
