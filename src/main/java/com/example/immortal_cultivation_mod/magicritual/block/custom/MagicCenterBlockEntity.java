package com.example.immortal_cultivation_mod.magicritual.block.custom;

import com.example.immortal_cultivation_mod.magicritual.block.ModBlocks;
import com.example.immortal_cultivation_mod.magicritual.center.CenterContext;
import com.example.immortal_cultivation_mod.magicritual.center.CenterEffectSupport;
import com.example.immortal_cultivation_mod.magicritual.center.CenterFuelController;
import com.example.immortal_cultivation_mod.magicritual.center.CenterRitualDefinition;
import com.example.immortal_cultivation_mod.magicritual.center.CenterRitualManager;
import com.example.immortal_cultivation_mod.magicritual.center.CenterTargetPolicy;
import com.example.immortal_cultivation_mod.magicritual.center.MagicCenterType;
import com.example.immortal_cultivation_mod.magicritual.center.MagicCenterTypeRegistry;
import com.example.immortal_cultivation_mod.magicritual.center.TeleportCodeRegistry;
import com.example.immortal_cultivation_mod.magicritual.center.TeleportDestinationInfo;
import com.example.immortal_cultivation_mod.magicritual.center.behavior.TeleportCenterBehavior;
import com.example.immortal_cultivation_mod.magicritual.item.ModItems;
import com.example.immortal_cultivation_mod.magicritual.ui.CenterFuelMenu;
import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.Connection;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.joml.Vector3f;
import org.slf4j.Logger;

public class MagicCenterBlockEntity extends BlockEntity implements MenuProvider {
   private static final int DATA_VERSION = 3;
   private static final long LEGACY_ENERGY_PER_STONE = 1200L;
   private static final int LEGACY_COMBO_MIGRATION_VERSION = 1;
   private static final int CACHE_VALIDATION_TICKS = 100;
   public static final int MAX_CONTROLLER_PLAYERS = 16;
   public static final int MAX_BLACKLISTED_PLAYERS = 64;
   public static final int MAX_TELEPORT_DESTINATIONS = 32;
   private static final int MAX_TELEPORT_DESTINATION_LABEL_LENGTH = 180;
   public static final int MIN_RADIUS = 3;
   public static final int MAX_RADIUS = 303;
   public static final int GLYPH_RENDER_MARGIN = 128;
   private static final Logger LOGGER = LogUtils.getLogger();
   public static volatile double[][] FOG_POINTS = new double[0][];
   public static volatile double[][] INVERTED_POINTS = new double[0][];
   public static volatile double[][] FORCEFIELD_POINTS = new double[0][];
   public static volatile double[][] NORMAL_POINTS = new double[0][];
   private ResourceLocation centerTypeId;
   private ResourceLocation unknownRecoveryType;
   private int radius;
   private long energy;
   private long fuelCostRemainder;
   private float barrierHealth;
   private boolean enabled;
   private boolean active;
   private boolean particlesEnabled;
   private int glyphColor;
   private UUID ownerUuid;
   private UUID centerId;
   private ResourceLocation teleportLinkedDimension;
   private BlockPos teleportLinkedPos;
   private UUID teleportLinkedCenterId;
   private UUID teleportSourceCenterId;
   private final List<String> teleportDestinationCodes;
   private String selectedTeleportDestinationCode;
   private UUID teleportChannelPlayer;
   private long teleportChannelStartTime;
   private int teleportChannelDurationTicks;
   private float teleportChannelStartHealth;
   private int teleportChannelObservedHurtTime;
   private final Map<UUID, String> controllerPlayers;
   private final Set<UUID> excludedPlayers;
   private final Set<String> legacyExcludedNames;
   private final Set<UUID> authorizedPlayersInside;
   private boolean authorizedPlayerInside;
   private long authorizedEntryGameTime;
   private boolean allowHostileMobs;
   private boolean allowPassiveMobs;
   private String customName;
   private CircleCounts passiveCircleCounts;
   private boolean passiveCacheDirty;
   private long lastPassiveScan;
   private PendingTransformation pendingTransformation;
   private List<ConsumedCircle> transformationCircles;
   private boolean legacyRadiusMigrated;
   private int legacyComboMigrationVersion;
   private boolean temporary;
   private long temporaryExpiresAt;
   private boolean structureTrap;
   private boolean suppressContentsDrop;
   private boolean suppressOctagonDrop;
   private final ItemStackHandler fuelSlot;
   private final ItemStackHandler reforgeWeaponSlot;
   private final ItemStackHandler octagonGrid;

   public MagicCenterBlockEntity(BlockPos pos, BlockState state) {
      super((BlockEntityType)ModBlockEntities.MAGIC_CENTER_BE.get(), pos, state);
      this.centerTypeId = MagicCenterTypeRegistry.BLANK;
      this.radius = 5;
      this.enabled = true;
      this.particlesEnabled = true;
      this.glyphColor = 16777215;
      this.centerId = UUID.randomUUID();
      this.teleportDestinationCodes = new ArrayList();
      this.selectedTeleportDestinationCode = "";
      this.teleportChannelStartTime = -1L;
      this.controllerPlayers = new HashMap();
      this.excludedPlayers = new HashSet();
      this.legacyExcludedNames = new HashSet();
      this.authorizedPlayersInside = new HashSet();
      this.authorizedEntryGameTime = -1L;
      this.allowHostileMobs = true;
      this.allowPassiveMobs = true;
      this.customName = "";
      this.passiveCircleCounts = MagicCenterBlockEntity.CircleCounts.ZERO;
      this.passiveCacheDirty = true;
      this.lastPassiveScan = Long.MIN_VALUE;
      this.transformationCircles = List.of();
      this.temporaryExpiresAt = -1L;
      this.fuelSlot = new ItemStackHandler(1) {
         protected void onContentsChanged(int slot) {
            MagicCenterBlockEntity.this.setChangedAndSync();
         }

         public boolean isItemValid(int slot, ItemStack stack) {
            return ModItems.isSpiritStone(stack.getItem());
         }
      };
      this.reforgeWeaponSlot = new ItemStackHandler(1) {
         protected void onContentsChanged(int slot) {
            MagicCenterBlockEntity.this.setChangedAndSync();
         }

         public boolean isItemValid(int slot, ItemStack stack) {
            return MagicCenterTypeRegistry.REFORGE_BLADE.equals(MagicCenterBlockEntity.this.centerTypeId) && stack.isDamageableItem();
         }

         public int getSlotLimit(int slot) {
            return 1;
         }
      };
      this.octagonGrid = new ItemStackHandler(24) {
         protected void onContentsChanged(int slot) {
            MagicCenterBlockEntity.this.setChangedAndSync();
         }
      };
      if (state.hasProperty(MagicCenterBlock.RADIUS)) {
         this.radius = normalizeRadius((Integer)state.getValue(MagicCenterBlock.RADIUS));
      }

   }

   public static void loadPatterns(ResourceManager manager) {
      FOG_POINTS = MagicPatternLoader.load(manager, ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "pattern/fog.json"));
      INVERTED_POINTS = MagicPatternLoader.load(manager, ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "pattern/inverted.json"));
      FORCEFIELD_POINTS = MagicPatternLoader.load(manager, ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "pattern/forcefield.json"));
      NORMAL_POINTS = MagicPatternLoader.load(manager, ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "pattern/normal.json"));
   }

   public static void spawnParticles(Level level, BlockPos pos, BlockState state, MagicCenterBlockEntity center, double[][] points, Vector3f color) {
   }

   public static void tick(Level level, BlockPos pos, BlockState state, MagicCenterBlockEntity center) {
      if (level instanceof ServerLevel serverLevel) {
         if (center.temporary && serverLevel.getGameTime() >= center.temporaryExpiresAt) {
            center.removeWithoutDrops();
         } else {
            center.recoverPendingTransformation();
            center.migrateLegacyRadius(state);
            center.migrateLegacyComboIfNeeded(serverLevel);
            center.updateAuthorizedPlayerPresence(serverLevel);
            MagicCenterType type = center.getCenterType();
            CenterContext context = new CenterContext(serverLevel, pos, state, center);
            if (!center.temporary && serverLevel.getGameTime() % 20L == 0L) {
               CenterFuelController.pullFromAdjacentInventories(context);
            }

            if (MagicCenterTypeRegistry.BLANK.equals(type.id())) {
               center.refreshPassiveCircleCacheIfNeeded(false);
            }

            int baseCostPerMinute = Math.max(0, type.behavior().getFuelCostPerMinute(context));
            int costPerMinute = center.applyFormationFlagFuelMultiplier(baseCostPerMinute);
            boolean wasActive = center.active;
            boolean teleportCenter = MagicCenterTypeRegistry.TELEPORT.equals(type.id());
            if (teleportCenter && serverLevel.getGameTime() % 20L == 0L) {
               TeleportCodeRegistry.register(center);
            }

            boolean teleportChannelPending;
            boolean automaticTemporaryTeleport;
            boolean var10001;
            label78: {
               label77: {
                  teleportChannelPending = teleportCenter && center.hasTeleportChannel();
                  automaticTemporaryTeleport = teleportCenter && center.temporary && center.hasTeleportLink();
                  boolean proximityReady = !center.structureTrap || center.hasPlayerInsideRadius(serverLevel);
                  if (proximityReady) {
                     if (teleportCenter) {
                        if (center.enabled && center.hasFuel() && center.hasPlayerInsideRadius(serverLevel)) {
                           break label77;
                        }
                     } else if (center.temporary) {
                        if (center.enabled) {
                           break label77;
                        }
                     } else if (CenterFuelController.consumeActiveTick(context, costPerMinute)) {
                        break label77;
                     }
                  }

                  var10001 = false;
                  break label78;
               }

               var10001 = true;
            }

            center.active = var10001;
            if (center.active || teleportChannelPending || automaticTemporaryTeleport) {
               type.behavior().serverTick(context);
            }

            CenterEffectSupport.track(center);
            if (wasActive != center.active) {
               center.setChangedAndSync();
            } else if (serverLevel.getGameTime() % 20L == 0L) {
               center.setChanged();
            }

         }
      }
   }

   private void updateAuthorizedPlayerPresence(ServerLevel serverLevel) {
      Set<UUID> currentPlayers = new HashSet();
      Vec3 center = this.centerPoint();
      double radiusSquared = (double)(this.radius * this.radius);

      for(ServerPlayer player : serverLevel.players()) {
         if (player.isAlive() && !(player.getY() < (double)this.worldPosition.getY()) && CenterTargetPolicy.isAuthorizedPlayer(this, player)) {
            double dx = player.getX() - center.x;
            double dz = player.getZ() - center.z;
            if (dx * dx + dz * dz <= radiusSquared) {
               currentPlayers.add(player.getUUID());
            }
         }
      }

      boolean playerEntered = currentPlayers.stream().anyMatch((uuid) -> !this.authorizedPlayersInside.contains(uuid));
      boolean playerInside = !currentPlayers.isEmpty();
      boolean presenceChanged = playerInside != this.authorizedPlayerInside;
      this.authorizedPlayersInside.clear();
      this.authorizedPlayersInside.addAll(currentPlayers);
      this.authorizedPlayerInside = playerInside;
      if (playerEntered) {
         this.authorizedEntryGameTime = serverLevel.getGameTime();
      }

      if (playerEntered || presenceChanged) {
         this.setChangedAndSync();
      }

   }

   private boolean hasPlayerInsideRadius(ServerLevel serverLevel) {
      for(ServerPlayer player : serverLevel.players()) {
         if (player.isAlive() && !player.isSpectator() && this.isInsideRadius(player)) {
            return true;
         }
      }

      return false;
   }

   private void migrateLegacyRadius(BlockState state) {
      if (!this.legacyRadiusMigrated) {
         this.legacyRadiusMigrated = true;
         if (state.hasProperty(MagicCenterBlock.RADIUS) && this.radius == 5) {
            this.radius = normalizeRadius((Integer)state.getValue(MagicCenterBlock.RADIUS));
         }

         this.passiveCacheDirty = true;
         this.setChanged();
      }

   }

   private void migrateLegacyComboIfNeeded(ServerLevel serverLevel) {
      if (this.legacyComboMigrationVersion < 1 && this.isBlank()) {
         int minChunkX = Math.floorDiv(this.worldPosition.getX() - this.radius, 16);
         int maxChunkX = Math.floorDiv(this.worldPosition.getX() + this.radius, 16);
         int minChunkZ = Math.floorDiv(this.worldPosition.getZ() - this.radius, 16);
         int maxChunkZ = Math.floorDiv(this.worldPosition.getZ() + this.radius, 16);

         for(int chunkX = minChunkX; chunkX <= maxChunkX; ++chunkX) {
            for(int chunkZ = minChunkZ; chunkZ <= maxChunkZ; ++chunkZ) {
               if (!serverLevel.hasChunk(chunkX, chunkZ)) {
                  return;
               }
            }
         }

         List<LegacyComboMatch> matches = new ArrayList();

         for(BlockEntity blockEntity : getLoadedBlockEntitiesNear(serverLevel, this.worldPosition, this.radius)) {
            if (blockEntity instanceof ComboBlockEntity && Math.abs(blockEntity.getBlockPos().getY() - this.worldPosition.getY()) <= 3) {
               BlockPos comboPos = blockEntity.getBlockPos();
               BlockState comboState = blockEntity.getBlockState();
               if (comboState.is((Block)ModBlocks.COMBO_BLOCK.get())) {
                  LegacyComboMatch match = findLegacyComboMatch(serverLevel, comboPos, (ComboType)comboState.getValue(ComboBlock.COMBO_TYPE));
                  if (match != null) {
                     matches.add(match);
                  }
               }
            }
         }

         this.legacyComboMigrationVersion = 1;
         if (matches.size() > 1) {
            LOGGER.warn("Magic Center at {} has {} old combo structures; leaving it blank for manual repair", this.worldPosition, matches.size());
            this.setChanged();
         } else if (matches.isEmpty()) {
            this.setChanged();
         } else {
            LegacyComboMatch match = (LegacyComboMatch)matches.getFirst();
            this.beginPendingTransformation(match.outputType(), match.circlePositions());
            serverLevel.setBlock(match.comboPos(), Blocks.AIR.defaultBlockState(), 3);

            for(BlockPos circlePos : match.circlePositions()) {
               if (serverLevel.getBlockState(circlePos).is((Block)ModBlocks.MAGIC_CIRCLE_BLOCK.get())) {
                  serverLevel.setBlock(circlePos, Blocks.AIR.defaultBlockState(), 3);
               }
            }

            this.completeTransformation(match.outputType());
            LOGGER.info("Migrated old {} combo at {} into Magic Center at {}", new Object[]{match.outputType(), match.comboPos(), this.worldPosition});
         }
      }
   }

   private static LegacyComboMatch findLegacyComboMatch(ServerLevel level, BlockPos comboPos, ComboType comboType) {
      ComboBlockEntity.PatternEntry[] entries = ComboBlockEntity.getPattern(comboType);

      for(int rotation = 0; rotation < 4; ++rotation) {
         List<BlockPos> circles = new ArrayList();
         boolean valid = true;

         for(ComboBlockEntity.PatternEntry entry : entries) {
            int var10000;
            switch (rotation) {
               case 1 -> var10000 = -entry.dz();
               case 2 -> var10000 = -entry.dx();
               case 3 -> var10000 = entry.dz();
               default -> var10000 = entry.dx();
            }

            int dx = var10000;
            switch (rotation) {
               case 1 -> var10000 = entry.dx();
               case 2 -> var10000 = -entry.dz();
               case 3 -> var10000 = -entry.dx();
               default -> var10000 = entry.dz();
            }

            int dz = var10000;
            BlockPos found = null;

            for(int y = -1; y <= 1; ++y) {
               BlockPos candidate = comboPos.offset(dx, y, dz);
               BlockState state = level.getBlockState(candidate);
               if (state.is((Block)ModBlocks.MAGIC_CIRCLE_BLOCK.get()) && state.getValue(MagicCircleBlock.CIRCLE_TYPE) == entry.type()) {
                  found = candidate.immutable();
                  break;
               }
            }

            if (found == null) {
               valid = false;
               break;
            }

            circles.add(found);
         }

         if (valid) {
            ResourceLocation var19;
            switch (comboType) {
               case FOG -> var19 = MagicCenterTypeRegistry.FOG;
               case INVERTED -> var19 = MagicCenterTypeRegistry.INVERTED;
               case FORCEFIELD -> var19 = MagicCenterTypeRegistry.FORCEFIELD;
               default -> throw new MatchException((String)null, (Throwable)null);
            }

            ResourceLocation output = var19;
            return new LegacyComboMatch(comboPos.immutable(), output, List.copyOf(circles));
         }
      }

      return null;
   }

   private void recoverPendingTransformation() {
      if (this.pendingTransformation != null && this.level != null && !this.level.isClientSide) {
         if (!MagicCenterTypeRegistry.contains(this.pendingTransformation.outputType())) {
            this.unknownRecoveryType = this.pendingTransformation.outputType();
            this.pendingTransformation = null;
            this.setChangedAndSync();
         } else {
            if (this.transformationCircles.isEmpty()) {
               this.transformationCircles = this.snapshotCircles(this.pendingTransformation.consumedPositions());
            }

            for(BlockPos consumed : this.pendingTransformation.consumedPositions()) {
               if (this.level.getBlockState(consumed).is((Block)ModBlocks.MAGIC_CIRCLE_BLOCK.get())) {
                  this.level.setBlock(consumed, Blocks.AIR.defaultBlockState(), 3);
               }
            }

            this.centerTypeId = this.pendingTransformation.outputType();
            this.pendingTransformation = null;
            this.fuelCostRemainder = 0L;
            this.passiveCircleCounts = MagicCenterBlockEntity.CircleCounts.ZERO;
            this.passiveCacheDirty = true;
            this.setChangedAndSync();
         }
      }
   }

   public void beginPendingTransformation(ResourceLocation outputType, List<BlockPos> consumedPositions) {
      this.transformationCircles = this.snapshotCircles(consumedPositions);
      this.pendingTransformation = new PendingTransformation(outputType, consumedPositions.stream().map(BlockPos::immutable).toList());
      this.setChangedAndSync();
   }

   public void completeTransformation(ResourceLocation outputType) {
      if (MagicCenterTypeRegistry.contains(outputType) && !MagicCenterTypeRegistry.BLANK.equals(outputType)) {
         this.centerTypeId = outputType;
         this.pendingTransformation = null;
         this.fuelCostRemainder = 0L;
         this.passiveCircleCounts = MagicCenterBlockEntity.CircleCounts.ZERO;
         this.passiveCacheDirty = true;
         this.setChangedAndSync();
      }
   }

   private List<ConsumedCircle> snapshotCircles(List<BlockPos> positions) {
      if (this.level != null && !positions.isEmpty()) {
         List<ConsumedCircle> snapshots = new ArrayList();

         for(BlockPos position : positions) {
            BlockState state = this.level.getBlockState(position);
            if (state.is((Block)ModBlocks.MAGIC_CIRCLE_BLOCK.get())) {
               snapshots.add(new ConsumedCircle(position.immutable(), (CircleType)state.getValue(MagicCircleBlock.CIRCLE_TYPE)));
            }
         }

         return List.copyOf(snapshots);
      } else {
         return List.of();
      }
   }

   public boolean breakTransformation(ServerLevel serverLevel) {
      if (this.isBlank()) {
         return false;
      } else {
         if (MagicCenterTypeRegistry.TELEPORT.equals(this.centerTypeId)) {
            this.unlinkTeleportPair();
            TeleportCodeRegistry.unregister(this);
         }

         List<ConsumedCircle> circles = this.transformationCircles.isEmpty() ? this.recoverCanonicalPattern() : this.transformationCircles;
         int brokenCount = circles.isEmpty() ? 0 : Math.max(1, Math.round((float)circles.size() * 0.25F));
         Set<Integer> brokenIndices = new HashSet();

         while(brokenIndices.size() < brokenCount) {
            brokenIndices.add(serverLevel.random.nextInt(circles.size()));
         }

         this.centerTypeId = MagicCenterTypeRegistry.BLANK;
         this.pendingTransformation = null;
         this.transformationCircles = List.of();
         this.fuelCostRemainder = 0L;
         this.active = false;
         this.barrierHealth = 0.0F;
         this.passiveCircleCounts = MagicCenterBlockEntity.CircleCounts.ZERO;
         this.passiveCacheDirty = true;
         this.lastPassiveScan = Long.MIN_VALUE;

         for(int index = 0; index < circles.size(); ++index) {
            ConsumedCircle circle = (ConsumedCircle)circles.get(index);
            BlockState circleState = (BlockState)((Block)ModBlocks.MAGIC_CIRCLE_BLOCK.get()).defaultBlockState().setValue(MagicCircleBlock.CIRCLE_TYPE, circle.type());
            BlockState currentState = serverLevel.getBlockState(circle.pos());
            boolean broken = brokenIndices.contains(index) || !currentState.isAir();
            if (!broken) {
               serverLevel.setBlock(circle.pos(), circleState, 3);
               serverLevel.sendParticles(ParticleTypes.ENCHANT, (double)circle.pos().getX() + (double)0.5F, (double)circle.pos().getY() + 0.2, (double)circle.pos().getZ() + (double)0.5F, 8, 0.18, 0.08, 0.18, 0.02);
            } else {
               if (currentState.is((Block)ModBlocks.MAGIC_CIRCLE_BLOCK.get())) {
                  serverLevel.setBlock(circle.pos(), Blocks.AIR.defaultBlockState(), 3);
               }

               serverLevel.sendParticles(ParticleTypes.ENCHANT, (double)circle.pos().getX() + (double)0.5F, (double)circle.pos().getY() + 0.15, (double)circle.pos().getZ() + (double)0.5F, 20, 0.32, 0.12, 0.32, 0.04);
               serverLevel.sendParticles(ParticleTypes.SMOKE, (double)circle.pos().getX() + (double)0.5F, (double)circle.pos().getY() + 0.18, (double)circle.pos().getZ() + (double)0.5F, 8, 0.22, 0.08, 0.22, 0.015);
            }
         }

         serverLevel.playSound((Player)null, this.worldPosition, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1.2F, 0.65F);
         serverLevel.sendParticles(ParticleTypes.POOF, (double)this.worldPosition.getX() + (double)0.5F, (double)this.worldPosition.getY() + 0.35, (double)this.worldPosition.getZ() + (double)0.5F, 28, 0.55, (double)0.25F, 0.55, 0.04);
         this.setChangedAndSync();
         serverLevel.getChunkSource().blockChanged(this.worldPosition);
         return true;
      }
   }

   private List<ConsumedCircle> recoverCanonicalPattern() {
      for(CenterRitualDefinition definition : CenterRitualManager.INSTANCE.definitions()) {
         if (definition.output().equals(this.centerTypeId)) {
            List<ConsumedCircle> recovered = new ArrayList();

            for(CenterRitualDefinition.Requirement requirement : definition.pattern()) {
               CircleType type = parseCircleType((String)requirement.properties().get("circle_type"));
               recovered.add(new ConsumedCircle(this.worldPosition.offset(requirement.offset()).immutable(), type));
            }

            return List.copyOf(recovered);
         }
      }

      return List.of();
   }

   private static CircleType parseCircleType(String name) {
      for(CircleType type : CircleType.values()) {
         if (type.getSerializedName().equals(name)) {
            return type;
         }
      }

      return CircleType.ATTACK;
   }

   public MagicCenterType getCenterType() {
      return MagicCenterTypeRegistry.getOrBlank(this.centerTypeId);
   }

   public ResourceLocation getCenterTypeId() {
      return this.centerTypeId;
   }

   public void configureStructureTrap(ResourceLocation typeId, int requestedRadius, int amplifierFlags) {
      if (MagicCenterTypeRegistry.contains(typeId) && !MagicCenterTypeRegistry.BLANK.equals(typeId)) {
         this.centerTypeId = typeId;
         this.unknownRecoveryType = null;
         this.radius = normalizeRadius(requestedRadius);
         this.energy = 0L;
         this.fuelCostRemainder = 0L;
         this.enabled = true;
         this.active = false;
         this.structureTrap = true;
         this.ownerUuid = null;
         this.controllerPlayers.clear();
         this.excludedPlayers.clear();
         this.legacyExcludedNames.clear();
         this.pendingTransformation = null;
         this.transformationCircles = List.of();
         this.passiveCircleCounts = MagicCenterBlockEntity.CircleCounts.ZERO;
         this.passiveCacheDirty = true;
         this.fuelSlot.setStackInSlot(0, new ItemStack((ItemLike)ModItems.INFINITE_SPIRIT_STONE.get()));
         this.clearOctagonGrid();
         int flagCount = Math.max(0, Math.min(this.octagonGrid.getSlots(), amplifierFlags));

         for(int slot = 0; slot < flagCount; ++slot) {
            this.octagonGrid.setStackInSlot(slot, new ItemStack((ItemLike)ModItems.AMPLIFIER_FLAG.get()));
         }

         this.setChangedAndSync();
      }
   }

   public boolean isStructureTrap() {
      return this.structureTrap;
   }

   public UUID getCenterId() {
      return this.centerId;
   }

   public String getTeleportCode() {
      return TeleportCodeRegistry.codeFor(this.centerId);
   }

   public boolean hasTeleportLink() {
      return !this.selectedTeleportDestinationCode.isEmpty() && this.teleportDestinationCodes.contains(this.selectedTeleportDestinationCode);
   }

   public boolean canTeleportOut() {
      return this.hasTeleportLink();
   }

   public int getTeleportLinkMode() {
      return this.hasTeleportLink() ? 0 : -1;
   }

   public String getTeleportDestinationLabel() {
      if (!this.hasTeleportLink()) {
         return "-";
      } else {
         Level var2 = this.level;
         if (var2 instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)var2;
            TeleportCodeRegistry.TargetRef ref = TeleportCodeRegistry.lookup(serverLevel.getServer(), this.selectedTeleportDestinationCode);
            if (ref != null) {
               return destinationLabel(ref);
            }
         }

         return this.selectedTeleportDestinationCode;
      }
   }

   public String getSelectedTeleportDestinationCode() {
      return this.selectedTeleportDestinationCode;
   }

   public List<String> getTeleportDestinationCodes() {
      return List.copyOf(this.teleportDestinationCodes);
   }

   public TeleportLinkResult addTeleportDestination(String rawCode) {
      Level var3 = this.level;
      if (var3 instanceof ServerLevel serverLevel) {
         if (MagicCenterTypeRegistry.TELEPORT.equals(this.centerTypeId) && !this.temporary) {
            String code = TeleportCodeRegistry.normalize(rawCode);
            if (code.isEmpty()) {
               return MagicCenterBlockEntity.TeleportLinkResult.INVALID_CODE;
            }

            if (code.equals(this.getTeleportCode())) {
               return MagicCenterBlockEntity.TeleportLinkResult.SELF;
            }

            if (this.teleportDestinationCodes.contains(code)) {
               this.selectedTeleportDestinationCode = code;
               this.clearTeleportChannel();
               this.setChangedAndSync();
               return MagicCenterBlockEntity.TeleportLinkResult.ALREADY_LINKED;
            }

            if (this.teleportDestinationCodes.size() >= 32) {
               return MagicCenterBlockEntity.TeleportLinkResult.FULL;
            }

            TeleportCodeRegistry.ResolvedTarget target = TeleportCodeRegistry.resolve(serverLevel.getServer(), code);
            if (target != null && target.center() != this && !target.center().isTemporary()) {
               this.teleportDestinationCodes.add(code);
               this.selectedTeleportDestinationCode = code;
               this.clearTeleportChannel();
               this.setChangedAndSync();
               return MagicCenterBlockEntity.TeleportLinkResult.ADDED;
            }

            return MagicCenterBlockEntity.TeleportLinkResult.NOT_FOUND;
         }
      }

      return MagicCenterBlockEntity.TeleportLinkResult.NOT_FOUND;
   }

   public boolean selectTeleportDestination(String rawCode) {
      String code = TeleportCodeRegistry.normalize(rawCode);
      if (!code.isEmpty() && this.teleportDestinationCodes.contains(code)) {
         if (!code.equals(this.selectedTeleportDestinationCode)) {
            this.selectedTeleportDestinationCode = code;
            this.clearTeleportChannel();
            this.setChangedAndSync();
         }

         return true;
      } else {
         return false;
      }
   }

   public boolean removeTeleportDestination(String rawCode) {
      String code = TeleportCodeRegistry.normalize(rawCode);
      if (!code.isEmpty() && this.teleportDestinationCodes.remove(code)) {
         if (this.teleportLinkedCenterId != null && code.equals(TeleportCodeRegistry.codeFor(this.teleportLinkedCenterId))) {
            this.clearLegacyTeleportLink();
         }

         if (code.equals(this.selectedTeleportDestinationCode)) {
            this.selectedTeleportDestinationCode = this.teleportDestinationCodes.isEmpty() ? "" : (String)this.teleportDestinationCodes.getFirst();
         }

         this.clearTeleportChannel();
         this.setChangedAndSync();
         return true;
      } else {
         return false;
      }
   }

   public List<TeleportDestinationInfo> getTeleportDestinationInfo() {
      Level var2 = this.level;
      if (var2 instanceof ServerLevel serverLevel) {
         if (MagicCenterTypeRegistry.TELEPORT.equals(this.centerTypeId)) {
            TeleportCodeRegistry.register(this);
            List<TeleportDestinationInfo> destinations = new ArrayList();

            for(String code : this.teleportDestinationCodes) {
               TeleportCodeRegistry.TargetRef ref = TeleportCodeRegistry.lookup(serverLevel.getServer(), code);
               if (ref == null) {
                  destinations.add(new TeleportDestinationInfo(code, code, 100, 0L, false));
               } else {
                  destinations.add(new TeleportDestinationInfo(code, destinationLabel(ref), 100, TeleportCenterBehavior.energyCostPerEntity(this, ref.pos()), true));
               }
            }

            return List.copyOf(destinations);
         }
      }

      return List.of();
   }

   private static String destinationLabel(TeleportCodeRegistry.TargetRef ref) {
      String name = ref.label() != null && !ref.label().isBlank() ? ref.label() : ref.dimension().toString();
      String label = name + " \u00b7 " + String.valueOf(ref.dimension()) + " @ " + ref.pos().getX() + ", " + ref.pos().getY() + ", " + ref.pos().getZ();
      return label.length() <= 180 ? label : label.substring(0, 177) + "...";
   }

   public void linkTeleportPair(MagicCenterBlockEntity other) {
      if (other != null && other != this && this.level != null && other.level != null && MagicCenterTypeRegistry.TELEPORT.equals(this.centerTypeId) && MagicCenterTypeRegistry.TELEPORT.equals(other.centerTypeId)) {
         TeleportCodeRegistry.register(this);
         TeleportCodeRegistry.register(other);
         this.clearTeleportLinkLocal();
         other.clearTeleportLinkLocal();
         this.teleportDestinationCodes.add(other.getTeleportCode());
         this.selectedTeleportDestinationCode = other.getTeleportCode();
         other.teleportDestinationCodes.add(this.getTeleportCode());
         other.selectedTeleportDestinationCode = this.getTeleportCode();
         this.clearTeleportChannel();
         other.clearTeleportChannel();
         this.setChangedAndSync();
         other.setChangedAndSync();
      }
   }

   public void cycleTeleportLinkMode() {
   }

   public ResolvedTeleportTarget resolveTeleportTarget() {
      Level var2 = this.level;
      if (var2 instanceof ServerLevel sourceLevel) {
         if (this.hasTeleportLink()) {
            TeleportCodeRegistry.ResolvedTarget resolved = TeleportCodeRegistry.resolve(sourceLevel.getServer(), this.selectedTeleportDestinationCode);
            if (resolved != null && resolved.center() != this) {
               return new ResolvedTeleportTarget(resolved.level(), resolved.center());
            }

            if (this.teleportLinkedDimension != null && this.teleportLinkedPos != null && this.teleportLinkedCenterId != null && this.selectedTeleportDestinationCode.equals(TeleportCodeRegistry.codeFor(this.teleportLinkedCenterId))) {
               ServerLevel targetLevel = sourceLevel.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, this.teleportLinkedDimension));
               if (targetLevel == null) {
                  return null;
               }

               targetLevel.getChunkAt(this.teleportLinkedPos);
               BlockEntity var5 = targetLevel.getBlockEntity(this.teleportLinkedPos);
               if (var5 instanceof MagicCenterBlockEntity) {
                  MagicCenterBlockEntity target = (MagicCenterBlockEntity)var5;
                  if (MagicCenterTypeRegistry.TELEPORT.equals(target.centerTypeId) && this.teleportLinkedCenterId.equals(target.centerId)) {
                     TeleportCodeRegistry.register(target);
                     return new ResolvedTeleportTarget(targetLevel, target);
                  }
               }

               return null;
            }

            return null;
         }
      }

      return null;
   }

   public void reconcileTeleportLinkAfterPlacement() {
      TeleportCodeRegistry.register(this);
   }

   public void unlinkTeleportPair() {
      Level var2 = this.level;
      if (var2 instanceof ServerLevel serverLevel) {
         String ownCode = this.getTeleportCode();

         for(String code : List.copyOf(this.teleportDestinationCodes)) {
            TeleportCodeRegistry.ResolvedTarget target = TeleportCodeRegistry.resolve(serverLevel.getServer(), code);
            if (target != null) {
               target.center().removeTeleportDestination(ownCode);
            }
         }
      }

      this.clearTeleportLinkLocal();
      this.setChangedAndSync();
   }

   private void clearTeleportLinkLocal() {
      this.clearLegacyTeleportLink();
      this.teleportDestinationCodes.clear();
      this.selectedTeleportDestinationCode = "";
      this.clearTeleportChannel();
   }

   private void clearLegacyTeleportLink() {
      this.teleportLinkedDimension = null;
      this.teleportLinkedPos = null;
      this.teleportLinkedCenterId = null;
      this.teleportSourceCenterId = null;
   }

   public boolean isInsideRadius(Entity entity) {
      return entity != null && entity.position().distanceToSqr(this.centerPoint()) <= (double)this.radius * (double)this.radius;
   }

   public boolean hasTeleportChannel() {
      return this.teleportChannelPlayer != null;
   }

   public UUID getTeleportChannelPlayer() {
      return this.teleportChannelPlayer;
   }

   public long getTeleportChannelStartTime() {
      return this.teleportChannelStartTime;
   }

   public int getTeleportChannelDurationTicks() {
      return this.teleportChannelDurationTicks;
   }

   public float getTeleportChannelStartHealth() {
      return this.teleportChannelStartHealth;
   }

   public int getTeleportChannelObservedHurtTime() {
      return this.teleportChannelObservedHurtTime;
   }

   public void observeTeleportHurtTime(int hurtTime) {
      this.teleportChannelObservedHurtTime = Math.max(0, hurtTime);
   }

   public void beginTeleportChannel(UUID playerId, long startTime, int durationTicks, float startHealth, int observedHurtTime) {
      this.teleportChannelPlayer = playerId;
      this.teleportChannelStartTime = startTime;
      this.teleportChannelDurationTicks = Math.max(1, durationTicks);
      this.teleportChannelStartHealth = startHealth;
      this.teleportChannelObservedHurtTime = Math.max(0, observedHurtTime);
      this.setChangedAndSync();
   }

   public void clearTeleportChannel() {
      if (this.teleportChannelPlayer != null || this.teleportChannelStartTime >= 0L || this.teleportChannelDurationTicks != 0) {
         this.teleportChannelPlayer = null;
         this.teleportChannelStartTime = -1L;
         this.teleportChannelDurationTicks = 0;
         this.teleportChannelStartHealth = 0.0F;
         this.teleportChannelObservedHurtTime = 0;
         this.setChangedAndSync();
      }
   }

   public boolean hasTeleportEnergy(long requestedEnergy) {
      long cost = Math.max(0L, requestedEnergy);
      if (cost != 0L && !this.hasInfiniteFuel()) {
         ItemStack storedFuel = this.fuelSlot.getStackInSlot(0);
         long fuelValue = (long)CenterFuelController.fuelValue(storedFuel);
         long itemEnergy = fuelValue <= 0L ? 0L : (fuelValue > Long.MAX_VALUE / (long)storedFuel.getCount() ? Long.MAX_VALUE : fuelValue * (long)storedFuel.getCount());
         return this.energy >= cost || itemEnergy >= cost - this.energy;
      } else {
         return true;
      }
   }

   public boolean consumeTeleportEnergy(long requestedEnergy) {
      long cost = Math.max(0L, requestedEnergy);
      if (cost != 0L && !this.hasInfiniteFuel()) {
         if (!this.hasTeleportEnergy(cost)) {
            return false;
         } else {
            while(this.energy < cost) {
               ItemStack fuel = this.fuelSlot.getStackInSlot(0);
               int value = CenterFuelController.fuelValue(fuel);
               if (value <= 0) {
                  return false;
               }

               long missing = cost - this.energy;
               int needed = (int)Math.min(2147483647L, Math.max(1L, (missing + (long)value - 1L) / (long)value));
               ItemStack consumed = this.fuelSlot.extractItem(0, needed, false);
               if (consumed.isEmpty()) {
                  return false;
               }

               long added = (long)value * (long)consumed.getCount();
               this.energy = added > Long.MAX_VALUE - this.energy ? Long.MAX_VALUE : this.energy + added;
            }

            this.energy -= cost;
            this.setChangedAndSync();
            return true;
         }
      } else {
         return true;
      }
   }

   public boolean isBlank() {
      return MagicCenterTypeRegistry.BLANK.equals(this.centerTypeId);
   }

   public int getCurrentFuelCostPerMinute() {
      Level var2 = this.level;
      if (var2 instanceof ServerLevel serverLevel) {
         CenterContext var4 = new CenterContext(serverLevel, this.worldPosition, this.getBlockState(), this);
         int baseCost = Math.max(0, this.getCenterType().behavior().getFuelCostPerMinute(var4));
         return this.applyFormationFlagFuelMultiplier(baseCost);
      } else {
         return this.applyFormationFlagFuelMultiplier(CenterFuelController.baseRadiusCost(this.radius));
      }
   }

   public int getRadius() {
      return this.radius;
   }

   public void setRadius(int requestedRadius) {
      int normalized = normalizeRadius(requestedRadius);
      if (this.radius != normalized) {
         this.radius = normalized;
         this.invalidatePassiveCache();
         this.setChangedAndSync();
      }

   }

   public static int normalizeRadius(int requestedRadius) {
      int clamped = Math.max(3, Math.min(303, requestedRadius));
      return (clamped & 1) == 0 ? clamped - 1 : clamped;
   }

   public static List<BlockEntity> getLoadedBlockEntitiesNear(Level level, BlockPos origin, int horizontalRadius) {
      int radius = Math.max(0, horizontalRadius);
      int minChunkX = Math.floorDiv(origin.getX() - radius, 16);
      int maxChunkX = Math.floorDiv(origin.getX() + radius, 16);
      int minChunkZ = Math.floorDiv(origin.getZ() - radius, 16);
      int maxChunkZ = Math.floorDiv(origin.getZ() + radius, 16);
      List<BlockEntity> blockEntities = new ArrayList();

      for(int chunkX = minChunkX; chunkX <= maxChunkX; ++chunkX) {
         for(int chunkZ = minChunkZ; chunkZ <= maxChunkZ; ++chunkZ) {
            var chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
            if (chunk != null) {
               for(BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                  if (blockEntity.isRemoved()) continue;
                  BlockPos blockPos = blockEntity.getBlockPos();
                  if (Math.abs(blockPos.getX() - origin.getX()) <= radius && Math.abs(blockPos.getZ() - origin.getZ()) <= radius) {
                     blockEntities.add(blockEntity);
                  }
               }
            }
         }
      }

      return blockEntities;
   }

   public static List<MagicCenterBlockEntity> getLoadedCentersNear(Level level, BlockPos origin, int horizontalRadius) {
      List<MagicCenterBlockEntity> centers = new ArrayList();

      for(BlockEntity blockEntity : getLoadedBlockEntitiesNear(level, origin, horizontalRadius)) {
         if (blockEntity instanceof MagicCenterBlockEntity center) {
            centers.add(center);
         }
      }

      return centers;
   }

   public Vec3 centerPoint() {
      return Vec3.atCenterOf(this.worldPosition);
   }

   public long getEnergy() {
      return this.energy;
   }

   public void setEnergy(long energy) {
      this.energy = Math.max(0L, energy);
   }

   public long getFuelCostRemainder() {
      return this.fuelCostRemainder;
   }

   public void setFuelCostRemainder(long fuelCostRemainder) {
      this.fuelCostRemainder = Math.max(0L, fuelCostRemainder);
   }

   public void addEnergy(long energy) {
      if (energy > 0L) {
         this.energy = Math.min(Long.MAX_VALUE, this.energy + Math.min(energy, Long.MAX_VALUE - this.energy));
         this.setChanged();
      }

   }

   public float getBarrierHealth() {
      return this.barrierHealth;
   }

   public void addBarrierHealth(float amount) {
      if (amount > 0.0F) {
         this.barrierHealth = Math.min(Float.MAX_VALUE, this.barrierHealth + amount);
         this.setChangedAndSync();
      }

   }

   public boolean absorbBarrierDamage(float amount) {
      Level var3 = this.level;
      if (var3 instanceof ServerLevel serverLevel) {
         if (MagicCenterTypeRegistry.FORCEFIELD.equals(this.centerTypeId)) {
            if (this.temporary) {
               return this.active;
            }

            if (this.hasInfiniteFuel()) {
               return true;
            }

            float remainingDamage = Math.max(1.0F, amount);
            while(this.barrierHealth < remainingDamage) {
               ItemStack reserveStone = this.fuelSlot.extractItem(0, 1, false);
               if (reserveStone.isEmpty()) {
                  this.collapseBarrier(serverLevel);
                  return false;
               }
               this.barrierHealth += (float)CenterFuelController.barrierHealthFor(reserveStone);
            }

            this.barrierHealth = Math.max(0.0F, this.barrierHealth - remainingDamage);
            this.setChangedAndSync();
            return true;
         }
      }

      return false;
   }

   private void collapseBarrier(ServerLevel serverLevel) {
      this.active = false;
      this.energy = 0L;
      this.barrierHealth = 0.0F;
      Vec3 center = this.centerPoint();
      serverLevel.explode((Entity)null, center.x, center.y, center.z, 3.0F, ExplosionInteraction.NONE);
      this.breakTransformation(serverLevel);
   }

   public boolean hasFuel() {
      return this.energy > 0L || !this.fuelSlot.getStackInSlot(0).isEmpty();
   }

   public boolean hasInfiniteFuel() {
      return this.fuelSlot.getStackInSlot(0).is((Item)ModItems.INFINITE_SPIRIT_STONE.get());
   }

   public boolean isEnabled() {
      return this.enabled;
   }

   public void setEnabled(boolean enabled) {
      if (this.enabled != enabled) {
         this.enabled = enabled;
         if (!enabled) {
            this.active = false;
         }

         this.setChangedAndSync();
      }

   }

   public boolean isActive() {
      return this.active;
   }

   public boolean isParticlesEnabled() {
      return this.particlesEnabled;
   }

   public boolean hasAuthorizedPlayerInside() {
      return this.authorizedPlayerInside;
   }

   public long getAuthorizedEntryGameTime() {
      return this.authorizedEntryGameTime;
   }

   public void setParticlesEnabled(boolean particlesEnabled) {
      if (this.particlesEnabled != particlesEnabled) {
         this.particlesEnabled = particlesEnabled;
         this.setChangedAndSync();
      }

   }

   public int getGlyphColor() {
      return this.glyphColor;
   }

   public void setGlyphColor(int glyphColor) {
      int normalized = glyphColor & 16777215;
      if (this.glyphColor != normalized) {
         this.glyphColor = normalized;
         this.setChangedAndSync();
      }

   }

   public UUID getOwnerUuid() {
      return this.ownerUuid;
   }

   public String getOwnerDisplayLabel() {
      if (this.ownerUuid == null) {
         return "-";
      } else {
         Level var2 = this.level;
         if (var2 instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)var2;
            ServerPlayer owner = serverLevel.getServer().getPlayerList().getPlayer(this.ownerUuid);
            if (owner != null) {
               return owner.getGameProfile().getName();
            }
         }

         return this.ownerUuid.toString();
      }
   }

   public void setOwnerUuid(UUID ownerUuid) {
      if (this.ownerUuid == null && ownerUuid != null) {
         this.ownerUuid = ownerUuid;
         this.controllerPlayers.clear();
         this.excludedPlayers.clear();
         this.legacyExcludedNames.clear();
         this.setChangedAndSync();
      }

   }

   public boolean isOwner(Player player) {
      return this.ownerUuid != null && this.ownerUuid.equals(player.getUUID());
   }

   public boolean isController(Player player) {
      UUID playerUuid = player.getUUID();
      if (this.isOwner(player)) {
         if (this.controllerPlayers.remove(playerUuid) != null) {
            this.setChanged();
         }

         return false;
      } else {
         String savedName = (String)this.controllerPlayers.get(playerUuid);
         if (savedName == null) {
            return false;
         } else {
            String currentName = player.getGameProfile().getName();
            if (!currentName.equals(savedName)) {
               this.controllerPlayers.entrySet().stream().filter((entry) -> !((UUID)entry.getKey()).equals(playerUuid) && ((String)entry.getValue()).equalsIgnoreCase(currentName)).forEach((entry) -> entry.setValue(((UUID)entry.getKey()).toString()));
               this.controllerPlayers.put(playerUuid, currentName);
               this.setChanged();
            }

            return true;
         }
      }
   }

   public Set<UUID> getControllerPlayers() {
      return Set.copyOf(this.controllerPlayers.keySet());
   }

   public Set<String> getControllerWhitelist() {
      Level var2 = this.level;
      if (var2 instanceof ServerLevel serverLevel) {
         this.refreshControllerNames(serverLevel);
      }

      Set<String> labels = new HashSet();
      this.controllerPlayers.forEach((uuid, name) -> labels.add(name != null && !name.isBlank() ? name : uuid.toString()));
      return labels;
   }

   private void refreshControllerNames(ServerLevel serverLevel) {
      boolean changed = false;

      for(Map.Entry<UUID, String> entry : this.controllerPlayers.entrySet()) {
         ServerPlayer online = serverLevel.getServer().getPlayerList().getPlayer((UUID)entry.getKey());
         String currentName = online == null ? (String)serverLevel.getServer().getProfileCache().get((UUID)entry.getKey()).map(GameProfile::getName).orElse((String)entry.getValue()) : online.getGameProfile().getName();
         if (isValidPlayerName(currentName) && !currentName.equals(entry.getValue()) && this.controllerPlayers.entrySet().stream().noneMatch((other) -> !((UUID)other.getKey()).equals(entry.getKey()) && ((String)other.getValue()).equalsIgnoreCase(currentName))) {
            entry.setValue(currentName);
            changed = true;
         }
      }

      if (changed) {
         this.setChanged();
      }

   }

   public boolean addController(String name) {
      if (!isValidPlayerName(name)) {
         return false;
      } else {
         String normalizedName = name.trim();
         Level var4 = this.level;
         if (var4 instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)var4;
            this.refreshControllerNames(serverLevel);
            ServerPlayer online = serverLevel.getServer().getPlayerList().getPlayerByName(normalizedName);
            GameProfile profile = online == null ? (GameProfile)serverLevel.getServer().getProfileCache().get(normalizedName).orElse(null) : online.getGameProfile();
            if (profile != null && profile.getId() != null && !profile.getId().equals(this.ownerUuid) && !this.controllerPlayers.containsKey(profile.getId()) && !this.controllerPlayers.values().stream().anyMatch((savedName) -> savedName.equalsIgnoreCase(normalizedName)) && this.controllerPlayers.size() < 16) {
               String profileName = profile.getName();
               this.controllerPlayers.put(profile.getId(), profileName != null && !profileName.isBlank() ? profileName : normalizedName);
               this.setChangedAndSync();
               return true;
            } else {
               return false;
            }
         } else {
            return false;
         }
      }
   }

   public boolean removeController(String name) {
      if (name != null && !name.isBlank() && name.length() <= 64) {
         String normalizedName = name.trim();
         UUID controllerUuid = null;

         try {
            controllerUuid = UUID.fromString(normalizedName);
         } catch (IllegalArgumentException var7) {
         }

         if (controllerUuid == null) {
            Level var5 = this.level;
            if (var5 instanceof ServerLevel) {
               ServerLevel serverLevel = (ServerLevel)var5;
               this.refreshControllerNames(serverLevel);
               ServerPlayer online = serverLevel.getServer().getPlayerList().getPlayerByName(normalizedName);
               GameProfile profile = online == null ? (GameProfile)serverLevel.getServer().getProfileCache().get(normalizedName).orElse(null) : online.getGameProfile();
               if (profile != null && this.controllerPlayers.containsKey(profile.getId())) {
                  controllerUuid = profile.getId();
               }
            }
         }

         if (controllerUuid == null) {
            controllerUuid = (UUID)this.controllerPlayers.entrySet().stream().filter((entry) -> ((String)entry.getValue()).equalsIgnoreCase(normalizedName)).map(Map.Entry::getKey).findFirst().orElse(null);
         }

         boolean changed = controllerUuid != null && this.controllerPlayers.remove(controllerUuid) != null;
         if (changed) {
            this.setChangedAndSync();
         }

         return changed;
      } else {
         return false;
      }
   }

   private static boolean isValidPlayerName(String name) {
      if (name == null) {
         return false;
      } else {
         String trimmed = name.trim();
         return trimmed.length() >= 1 && trimmed.length() <= 16 && trimmed.matches("[A-Za-z0-9_]+");
      }
   }

   private static String findIgnoreCase(Set<String> values, String target) {
      return target == null ? null : (String)values.stream().filter((value) -> value.equalsIgnoreCase(target)).findFirst().orElse(null);
   }

   private String resolvePlayerLabel(UUID uuid) {
      Level var3 = this.level;
      if (var3 instanceof ServerLevel serverLevel) {
         ServerPlayer online = serverLevel.getServer().getPlayerList().getPlayer(uuid);
         return online != null ? online.getGameProfile().getName() : (String)serverLevel.getServer().getProfileCache().get(uuid).map(GameProfile::getName).filter(MagicCenterBlockEntity::isValidPlayerName).orElse(uuid.toString());
      } else {
         return uuid.toString();
      }
   }

   private void saveAccessLists(CompoundTag tag) {
      ListTag controllers = new ListTag();
      ListTag controllerNames = new ListTag();

      for(Map.Entry<UUID, String> controller : this.controllerPlayers.entrySet()) {
         CompoundTag entry = new CompoundTag();
         entry.putUUID("UUID", (UUID)controller.getKey());
         String name = (String)controller.getValue();
         if (!isValidPlayerName(name)) {
            name = this.resolvePlayerLabel((UUID)controller.getKey());
         }

         if (isValidPlayerName(name)) {
            entry.putString("Name", name);
            controllerNames.add(StringTag.valueOf(name));
         }

         controllers.add(entry);
      }

      tag.put("ControllerPlayers", controllers);
      tag.put("ControllerWhitelist", controllerNames);
      ListTag excluded = new ListTag();
      ListTag blacklistedNames = new ListTag();
      Set<String> savedNames = new HashSet();

      for(UUID uuid : this.excludedPlayers) {
         CompoundTag entry = new CompoundTag();
         entry.putUUID("UUID", uuid);
         String name = this.resolvePlayerLabel(uuid);
         if (isValidPlayerName(name)) {
            entry.putString("Name", name);
            if (findIgnoreCase(savedNames, name) == null) {
               savedNames.add(name);
               blacklistedNames.add(StringTag.valueOf(name));
            }
         }

         excluded.add(entry);
      }

      tag.put("ExcludedPlayers", excluded);
      ListTag legacy = new ListTag();

      for(String name : this.legacyExcludedNames) {
         legacy.add(StringTag.valueOf(name));
         if (findIgnoreCase(savedNames, name) == null) {
            savedNames.add(name);
            blacklistedNames.add(StringTag.valueOf(name));
         }
      }

      tag.put("LegacyExcludedNames", legacy);
      tag.put("BlacklistedPlayerNames", blacklistedNames);
   }

   private void loadAccessLists(CompoundTag tag) {
      this.controllerPlayers.clear();
      ListTag controllers = tag.getList("ControllerPlayers", 10);

      for(int i = 0; i < controllers.size() && this.controllerPlayers.size() < 16; ++i) {
         CompoundTag entry = controllers.getCompound(i);
         if (entry.hasUUID("UUID")) {
            UUID uuid = entry.getUUID("UUID");
            if (!uuid.equals(this.ownerUuid) && !this.controllerPlayers.containsKey(uuid)) {
               String name = entry.getString("Name");
               if (!isValidPlayerName(name)) {
                  name = this.resolvePlayerLabel(uuid);
               }

               String proposedLabel = name;
               String label = this.controllerPlayers.values().stream().anyMatch((existing) -> existing.equalsIgnoreCase(proposedLabel)) ? uuid.toString() : proposedLabel;
               this.controllerPlayers.put(uuid, label);
            }
         }
      }

      this.excludedPlayers.clear();
      this.legacyExcludedNames.clear();
      Set<String> loadedNames = new HashSet();
      ListTag excluded = tag.getList("ExcludedPlayers", 10);

      for(int i = 0; i < excluded.size() && this.excludedPlayers.size() < 64; ++i) {
         CompoundTag entry = excluded.getCompound(i);
         if (entry.hasUUID("UUID")) {
            this.excludedPlayers.add(entry.getUUID("UUID"));
         }

         String name = entry.getString("Name");
         if (isValidPlayerName(name)) {
            loadedNames.add(name);
         }
      }

      readPlayerNameSet(tag.getList("LegacyExcludedNames", 8), this.legacyExcludedNames, 64 - this.excludedPlayers.size(), loadedNames);
      readPlayerNameSet(tag.getList("blacklistedPlayers", 8), this.legacyExcludedNames, 64 - this.excludedPlayers.size(), loadedNames);
      if (excluded.isEmpty()) {
         readPlayerNameSet(tag.getList("BlacklistedPlayerNames", 8), this.legacyExcludedNames, 64, Set.of());
      }

   }

   public boolean isPlayerExcluded(Player player) {
      if (this.excludedPlayers.contains(player.getUUID())) {
         return true;
      } else {
         String legacyName = findIgnoreCase(this.legacyExcludedNames, player.getGameProfile().getName());
         if (legacyName != null) {
            this.legacyExcludedNames.remove(legacyName);
            this.excludedPlayers.add(player.getUUID());
            this.setChangedAndSync();
            return true;
         } else {
            return false;
         }
      }
   }

   public Set<UUID> getExcludedPlayers() {
      return Set.copyOf(this.excludedPlayers);
   }

   public Set<String> getBlacklistedPlayers() {
      Set<String> labels = new HashSet(this.legacyExcludedNames);

      for(UUID uuid : this.excludedPlayers) {
         labels.add(this.resolvePlayerLabel(uuid));
      }

      return labels;
   }

   public boolean addExcludedPlayer(UUID uuid) {
      boolean changed = uuid != null && this.excludedPlayers.size() + this.legacyExcludedNames.size() < 64 && this.excludedPlayers.add(uuid);
      if (changed) {
         this.setChangedAndSync();
      }

      return changed;
   }

   public boolean removeExcludedPlayer(UUID uuid) {
      boolean changed = uuid != null && this.excludedPlayers.remove(uuid);
      if (changed) {
         this.setChangedAndSync();
      }

      return changed;
   }

   public boolean addBlacklistedPlayer(String name) {
      if (isValidPlayerName(name) && this.excludedPlayers.size() + this.legacyExcludedNames.size() < 64) {
         Level var3 = this.level;
         if (var3 instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)var3;
            String var7 = name.trim();
            ServerPlayer online = serverLevel.getServer().getPlayerList().getPlayerByName(var7);
            GameProfile profile = online == null ? (GameProfile)serverLevel.getServer().getProfileCache().get(var7).orElse(null) : online.getGameProfile();
            if (profile != null && profile.getId() != null && !this.excludedPlayers.contains(profile.getId()) && findIgnoreCase(this.legacyExcludedNames, var7) == null) {
               boolean changed = this.excludedPlayers.add(profile.getId());
               if (changed) {
                  this.setChangedAndSync();
               }

               return changed;
            } else {
               return false;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public boolean removeBlacklistedPlayer(String name) {
      String normalizedName = name == null ? "" : name.trim();
      boolean changed = false;

      try {
         changed = this.excludedPlayers.remove(UUID.fromString(normalizedName));
      } catch (IllegalArgumentException var7) {
      }

      Level var5 = this.level;
      if (var5 instanceof ServerLevel serverLevel) {
         ServerPlayer online = serverLevel.getServer().getPlayerList().getPlayerByName(normalizedName);
         GameProfile profile = online == null && isValidPlayerName(normalizedName) ? (GameProfile)serverLevel.getServer().getProfileCache().get(normalizedName).orElse(null) : (online == null ? null : online.getGameProfile());
         if (profile != null) {
            changed |= this.excludedPlayers.remove(profile.getId());
         }
      }

      String legacyName = findIgnoreCase(this.legacyExcludedNames, normalizedName);
      if (legacyName != null) {
         changed |= this.legacyExcludedNames.remove(legacyName);
      }

      if (changed) {
         this.setChangedAndSync();
      }

      return changed;
   }

   public boolean isAllowHostileMobs() {
      return this.allowHostileMobs;
   }

   public void setAllowHostileMobs(boolean allowHostileMobs) {
      if (this.allowHostileMobs != allowHostileMobs) {
         this.allowHostileMobs = allowHostileMobs;
         this.setChanged();
      }

   }

   public boolean isAllowPassiveMobs() {
      return this.allowPassiveMobs;
   }

   public void setAllowPassiveMobs(boolean allowPassiveMobs) {
      if (this.allowPassiveMobs != allowPassiveMobs) {
         this.allowPassiveMobs = allowPassiveMobs;
         this.setChanged();
      }

   }

   public boolean isAffectedByCircle(LivingEntity entity) {
      if (entity instanceof Player player) {
         return !this.isPlayerExcluded(player);
      } else {
         return entity instanceof Enemy ? this.allowHostileMobs : this.allowPassiveMobs;
      }
   }

   public String getCustomNameText() {
      return this.customName;
   }

   public void setCustomNameText(String customName) {
      this.customName = customName == null ? "" : customName.strip();
      this.setChanged();
   }

   public CircleCounts getPassiveCircleCounts() {
      return this.passiveCircleCounts;
   }

   public void invalidatePassiveCache() {
      this.passiveCacheDirty = true;
   }

   public void refreshPassiveCircleCacheIfNeeded(boolean force) {
      if (this.isBlank() && this.level != null) {
         long gameTime = this.level.getGameTime();
         if (force || this.passiveCacheDirty || gameTime - this.lastPassiveScan >= 100L) {
            int attack = 0;
            int defense = 0;
            int regeneration = 0;
            double radiusSquared = (double)(this.radius * this.radius);

            for(BlockEntity blockEntity : getLoadedBlockEntitiesNear(this.level, this.worldPosition, this.radius)) {
               BlockPos scanPos = blockEntity.getBlockPos();
               if (Math.abs(scanPos.getY() - this.worldPosition.getY()) <= 3 && !(Vec3.atCenterOf(scanPos).distanceToSqr(this.centerPoint()) > radiusSquared)) {
                  BlockState state = blockEntity.getBlockState();
                  if (state.is((Block)ModBlocks.MAGIC_CIRCLE_BLOCK.get())) {
                     switch ((CircleType)state.getValue(MagicCircleBlock.CIRCLE_TYPE)) {
                        case ATTACK:
                           ++attack;
                           break;
                        case DEFENSE:
                           ++defense;
                           break;
                        case REGENERATION:
                           ++regeneration;
                     }
                  }
               }
            }

            this.passiveCircleCounts = new CircleCounts(attack, defense, regeneration);
            this.passiveCacheDirty = false;
            this.lastPassiveScan = gameTime;
            this.setChanged();
         }
      } else {
         this.passiveCircleCounts = MagicCenterBlockEntity.CircleCounts.ZERO;
      }
   }

   public static void invalidateNearbyBlankCenters(Level level, BlockPos circlePos) {
      if (!level.isClientSide) {
         for(MagicCenterBlockEntity center : getLoadedCentersNear(level, circlePos, 303)) {
            if (Math.abs(center.worldPosition.getY() - circlePos.getY()) <= 3 && center.isBlank() && Vec3.atCenterOf(circlePos).distanceToSqr(center.centerPoint()) <= (double)(center.radius * center.radius)) {
               center.invalidatePassiveCache();
            }
         }

      }
   }

   public ItemStackHandler getFuelSlot() {
      return this.fuelSlot;
   }

   public ItemStackHandler getReforgeWeaponSlot() {
      return this.reforgeWeaponSlot;
   }

   public ItemStackHandler getOctagonGrid() {
      return this.octagonGrid;
   }

   public int countDurationFlags() {
      return this.countFlags((Item)ModItems.DURATION_FLAG.get());
   }

   public int countAmplifierFlags() {
      return this.countFlags((Item)ModItems.AMPLIFIER_FLAG.get());
   }

   public int countSpiritGatheringFlags() {
      return this.countFlags((Item)ModItems.SPIRIT_GATHERING_FLAG.get());
   }

   public int countHasteFlags() {
      return this.countFlags((Item)ModItems.HASTE_FLAG.get());
   }

   public int countQiReturnFlags() {
      return this.countFlags((Item)ModItems.QI_RETURN_FLAG.get());
   }

   public int countFormationFlags() {
      int count = 0;

      for(int slot = 0; slot < this.octagonGrid.getSlots(); ++slot) {
         ItemStack stack = this.octagonGrid.getStackInSlot(slot);
         if (ModItems.isFormationFlag(stack.getItem())) {
            count += stack.getCount();
         }
      }

      return count;
   }

   private int applyFormationFlagFuelMultiplier(int baseCost) {
      long adjustedCost = (long)Math.max(0, baseCost);
      if (adjustedCost == 0L) {
         return 0;
      } else {
         int gatheringFlags = this.countSpiritGatheringFlags();
         int normalCostFlags = Math.max(0, this.countFormationFlags() - gatheringFlags);

         for(int i = 0; i < normalCostFlags; ++i) {
            adjustedCost = (adjustedCost * 3L + 1L) / 2L;
            if (adjustedCost >= 2147483647L) {
               return Integer.MAX_VALUE;
            }
         }

         for(int i = 0; i < this.countHasteFlags(); ++i) {
            adjustedCost = (adjustedCost * 13L + 9L) / 10L;
            if (adjustedCost >= 2147483647L) {
               return Integer.MAX_VALUE;
            }
         }

         for(int i = 0; i < gatheringFlags; ++i) {
            adjustedCost = Math.max(1L, adjustedCost * 3L / 4L);
         }

         return (int)Math.min(2147483647L, adjustedCost);
      }
   }

   public int adjustedTriggerInterval(int baseTicks) {
      int interval = Math.max(1, baseTicks);

      for(int i = 0; i < this.countHasteFlags(); ++i) {
         interval = Math.max(1, (int)Math.round((double)interval / 1.3));
      }

      return interval;
   }

   public boolean shouldTriggerEvery(long gameTime, int baseTicks) {
      return gameTime % (long)this.adjustedTriggerInterval(baseTicks) == 0L;
   }

   public int getQiReturnAmount() {
      return this.countQiReturnFlags() * 10;
   }

   public int getAttackCount() {
      return 1 + this.countAmplifierFlags();
   }

   public boolean hasFormationFlags() {
      return this.countFormationFlags() > 0;
   }

   public ItemStack getFormationFlagForRender(int slot) {
      if (slot >= 0 && slot < this.octagonGrid.getSlots()) {
         ItemStack stack = this.octagonGrid.getStackInSlot(slot);
         return ModItems.isFormationFlag(stack.getItem()) ? stack : ItemStack.EMPTY;
      } else {
         return ItemStack.EMPTY;
      }
   }

   private int countFlags(Item flagItem) {
      int count = 0;

      for(int slot = 0; slot < this.octagonGrid.getSlots(); ++slot) {
         ItemStack stack = this.octagonGrid.getStackInSlot(slot);
         if (stack.is(flagItem)) {
            count += stack.getCount();
         }
      }

      return count;
   }

   public ItemStack getReforgeWeapon() {
      return this.reforgeWeaponSlot.getStackInSlot(0);
   }

   public void repairReforgeWeapon(int amount) {
      ItemStack weapon = this.reforgeWeaponSlot.getStackInSlot(0);
      if (amount > 0 && !weapon.isEmpty() && weapon.isDamageableItem() && weapon.isDamaged()) {
         ItemStack repaired = weapon.copy();
         repaired.setDamageValue(Math.max(0, repaired.getDamageValue() - amount));
         this.reforgeWeaponSlot.setStackInSlot(0, repaired);
      }
   }

   public Component getDisplayName() {
      return this.customName.isBlank() ? Component.translatable(this.getCenterType().translationKey()) : Component.literal(this.customName);
   }

   private void saveCenterIdentityAndTeleportLink(CompoundTag tag) {
      tag.putUUID("CenterId", this.centerId);
      ListTag destinations = new ListTag();

      for(String code : this.teleportDestinationCodes) {
         destinations.add(StringTag.valueOf(code));
      }

      tag.put("TeleportDestinations", destinations);
      if (this.hasTeleportLink()) {
         tag.putString("SelectedTeleportDestination", this.selectedTeleportDestinationCode);
      }

      if (this.teleportLinkedDimension != null && this.teleportLinkedPos != null && this.teleportLinkedCenterId != null) {
         CompoundTag link = new CompoundTag();
         link.putString("Dimension", this.teleportLinkedDimension.toString());
         link.putLong("Pos", this.teleportLinkedPos.asLong());
         link.putUUID("CenterId", this.teleportLinkedCenterId);
         if (this.teleportSourceCenterId != null) {
            link.putUUID("SourceCenterId", this.teleportSourceCenterId);
         }

         tag.put("TeleportLink", link);
      }
   }

   private void loadCenterIdentityAndTeleportLink(CompoundTag tag) {
      this.centerId = tag.hasUUID("CenterId") ? tag.getUUID("CenterId") : UUID.randomUUID();
      this.teleportLinkedDimension = null;
      this.teleportLinkedPos = null;
      this.teleportLinkedCenterId = null;
      this.teleportSourceCenterId = null;
      this.teleportDestinationCodes.clear();
      this.selectedTeleportDestinationCode = "";
      ListTag destinations = tag.getList("TeleportDestinations", 8);

      for(int index = 0; index < destinations.size() && this.teleportDestinationCodes.size() < 32; ++index) {
         String code = TeleportCodeRegistry.normalize(destinations.getString(index));
         if (!code.isEmpty() && !code.equals(this.getTeleportCode()) && !this.teleportDestinationCodes.contains(code)) {
            this.teleportDestinationCodes.add(code);
         }
      }

      String selected = TeleportCodeRegistry.normalize(tag.getString("SelectedTeleportDestination"));
      if (this.teleportDestinationCodes.contains(selected)) {
         this.selectedTeleportDestinationCode = selected;
      }

      if (!tag.contains("TeleportLink", 10)) {
         this.normalizeSelectedTeleportDestination();
      } else {
         CompoundTag link = tag.getCompound("TeleportLink");
         ResourceLocation dimension = ResourceLocation.tryParse(link.getString("Dimension"));
         if (dimension != null && link.contains("Pos", 4) && link.hasUUID("CenterId")) {
            this.teleportLinkedDimension = dimension;
            this.teleportLinkedPos = BlockPos.of(link.getLong("Pos"));
            this.teleportLinkedCenterId = link.getUUID("CenterId");
            this.teleportSourceCenterId = link.hasUUID("SourceCenterId") ? link.getUUID("SourceCenterId") : null;
            if (this.teleportDestinationCodes.isEmpty() && (this.teleportSourceCenterId == null || this.centerId.equals(this.teleportSourceCenterId))) {
               this.teleportDestinationCodes.add(TeleportCodeRegistry.codeFor(this.teleportLinkedCenterId));
            }

            this.normalizeSelectedTeleportDestination();
         } else {
            this.normalizeSelectedTeleportDestination();
         }
      }
   }

   private void normalizeSelectedTeleportDestination() {
      if (!this.teleportDestinationCodes.contains(this.selectedTeleportDestinationCode)) {
         this.selectedTeleportDestinationCode = this.teleportDestinationCodes.isEmpty() ? "" : (String)this.teleportDestinationCodes.getFirst();
      }

   }

   public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
      buffer.writeBlockPos(this.worldPosition);
      buffer.writeBoolean(this.allowHostileMobs);
      buffer.writeBoolean(this.allowPassiveMobs);
      buffer.writeCollection(this.getBlacklistedPlayers(), (buf, value) -> buf.writeUtf(value));
      buffer.writeBoolean(this.particlesEnabled);
      buffer.writeInt(this.glyphColor);
      buffer.writeResourceLocation(this.centerTypeId);
      buffer.writeVarInt(this.radius);
      buffer.writeVarLong(this.energy);
      buffer.writeFloat(this.barrierHealth);
      buffer.writeBoolean(this.enabled);
      buffer.writeBoolean(this.active);
      buffer.writeVarInt(this.getCurrentFuelCostPerMinute());
      buffer.writeVarInt(this.passiveCircleCounts.attack());
      buffer.writeVarInt(this.passiveCircleCounts.defense());
      buffer.writeVarInt(this.passiveCircleCounts.regeneration());
      buffer.writeBoolean(this.ownerUuid != null);
      if (this.ownerUuid != null) {
         buffer.writeUUID(this.ownerUuid);
      }

      boolean var10000;
      label38: {
         buffer.writeUtf(this.getOwnerDisplayLabel());
         buffer.writeCollection(this.excludedPlayers, (buf, value) -> buf.writeUUID(value));
         buffer.writeCollection(this.getControllerWhitelist(), (buf, value) -> buf.writeUtf(value));
         if (menu instanceof CenterFuelMenu fuelMenu) {
            if (fuelMenu.canControlCenter()) {
               var10000 = true;
               break label38;
            }
         }

         var10000 = false;
      }

      boolean canControl;
      label33: {
         canControl = var10000;
         if (menu instanceof CenterFuelMenu fuelMenu) {
            if (fuelMenu.canManageControllers()) {
               var10000 = true;
               break label33;
            }
         }

         var10000 = false;
      }

      boolean canManageControllers = var10000;
      buffer.writeBoolean(canControl);
      buffer.writeBoolean(canManageControllers);
      buffer.writeVarInt(this.getTeleportLinkMode());
      buffer.writeUtf(this.getTeleportDestinationLabel());
      buffer.writeUtf(this.getTeleportCode());
      buffer.writeUtf(this.getSelectedTeleportDestinationCode());
      List<TeleportDestinationInfo> destinations = this.getTeleportDestinationInfo();
      buffer.writeVarInt(destinations.size());

      for(TeleportDestinationInfo destination : destinations) {
         buffer.writeUtf(destination.code());
         buffer.writeUtf(destination.label());
         buffer.writeVarInt(destination.qiCost());
         buffer.writeVarLong(destination.energyCost());
         buffer.writeBoolean(destination.available());
      }

   }

   public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
      CenterFuelMenu menu = new CenterFuelMenu(id, inventory, this.fuelSlot, this.reforgeWeaponSlot, this.octagonGrid, ContainerLevelAccess.create(this.level, this.worldPosition), this.worldPosition, this.allowHostileMobs, this.allowPassiveMobs, this.getBlacklistedPlayers(), this.glyphColor, this.centerTypeId, this.radius, this.energy, this.barrierHealth, this.enabled, this.active, this.getCurrentFuelCostPerMinute(), this.passiveCircleCounts, this.ownerUuid, this.excludedPlayers, this.getControllerWhitelist(), CenterTargetPolicy.canEdit(this, player), CenterTargetPolicy.canManageControllers(this, player), this.getTeleportLinkMode(), this.getTeleportDestinationLabel(), this.getTeleportCode(), this.getSelectedTeleportDestinationCode(), this.getTeleportDestinationInfo());
      menu.bindCenter(this);
      return menu;
   }

   protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
      super.saveAdditional(tag, registries);
      tag.putInt("DataVersion", 3);
      tag.putString("CenterType", this.centerTypeId.toString());
      if (this.unknownRecoveryType != null) {
         tag.putString("UnknownRecoveryType", this.unknownRecoveryType.toString());
      }

      tag.putInt("Radius", this.radius);
      tag.putLong("Energy", this.energy);
      tag.putLong("FuelCostRemainder", this.fuelCostRemainder);
      tag.putFloat("BarrierHealth", this.barrierHealth);
      tag.putBoolean("Enabled", this.enabled);
      tag.putBoolean("Active", this.active);
      tag.putBoolean("ParticlesEnabled", this.particlesEnabled);
      tag.putInt("GlyphColor", this.glyphColor);
      tag.putBoolean("AllowHostileMobs", this.allowHostileMobs);
      tag.putBoolean("AllowPassiveMobs", this.allowPassiveMobs);
      tag.putString("CustomNameText", this.customName);
      tag.putInt("LegacyComboMigrationVersion", this.legacyComboMigrationVersion);
      tag.putBoolean("Temporary", this.temporary);
      tag.putLong("TemporaryExpiresAt", this.temporaryExpiresAt);
      tag.putBoolean("StructureTrap", this.structureTrap);
      if (this.ownerUuid != null) {
         tag.putUUID("Owner", this.ownerUuid);
      }

      this.saveCenterIdentityAndTeleportLink(tag);
      this.saveAccessLists(tag);
      tag.put("FuelInventory", this.fuelSlot.serializeNBT(registries));
      tag.put("ReforgeInventory", this.reforgeWeaponSlot.serializeNBT(registries));
      tag.put("OctagonGridInventory", this.octagonGrid.serializeNBT(registries));
      ListTag savedCircles = new ListTag();

      for(ConsumedCircle circle : this.transformationCircles) {
         CompoundTag entry = new CompoundTag();
         entry.putLong("Pos", circle.pos().asLong());
         entry.putString("Type", circle.type().getSerializedName());
         savedCircles.add(entry);
      }

      tag.put("TransformationCircles", savedCircles);
      if (this.pendingTransformation != null) {
         CompoundTag pending = new CompoundTag();
         pending.putString("OutputType", this.pendingTransformation.outputType().toString());
         ListTag positions = new ListTag();

         for(BlockPos pos : this.pendingTransformation.consumedPositions()) {
            positions.add(LongTag.valueOf(pos.asLong()));
         }

         pending.put("ConsumedPositions", positions);
         tag.put("PendingTransformation", pending);
      }

   }

   protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
      super.loadAdditional(tag, registries);
      int dataVersion = tag.getInt("DataVersion");
      ResourceLocation loadedType = ResourceLocation.tryParse(tag.getString("CenterType"));
      if (loadedType != null && MagicCenterTypeRegistry.contains(loadedType)) {
         this.centerTypeId = loadedType;
      } else if (loadedType != null) {
         this.unknownRecoveryType = loadedType;
         this.centerTypeId = MagicCenterTypeRegistry.BLANK;
      }

      if (tag.contains("UnknownRecoveryType", 8)) {
         this.unknownRecoveryType = ResourceLocation.tryParse(tag.getString("UnknownRecoveryType"));
      }

      if (tag.contains("Radius", 3)) {
         this.radius = normalizeRadius(tag.getInt("Radius"));
      }

      if (tag.contains("Energy", 4)) {
         long storedEnergy = Math.max(0L, tag.getLong("Energy"));
         this.energy = dataVersion < 3 && storedEnergy > 0L ? 1L + (storedEnergy - 1L) / 1200L : storedEnergy;
      } else if (tag.contains("fuel", 5)) {
         this.energy = Math.max(0L, (long)Math.ceil((double)tag.getFloat("fuel")));
      }

      this.fuelCostRemainder = Math.max(0L, tag.getLong("FuelCostRemainder")) % 1200L;
      this.barrierHealth = Math.max(0.0F, tag.getFloat("BarrierHealth"));
      this.enabled = !tag.contains("Enabled") || tag.getBoolean("Enabled");
      this.active = tag.getBoolean("Active");
      this.particlesEnabled = tag.contains("ParticlesEnabled") ? tag.getBoolean("ParticlesEnabled") : !tag.contains("particlesEnabled") || tag.getBoolean("particlesEnabled");
      this.glyphColor = tag.contains("GlyphColor") ? tag.getInt("GlyphColor") & 16777215 : (tag.contains("glyphColor") ? tag.getInt("glyphColor") & 16777215 : 16777215);
      this.allowHostileMobs = tag.contains("AllowHostileMobs") ? tag.getBoolean("AllowHostileMobs") : !tag.contains("allowHostileMobs") || tag.getBoolean("allowHostileMobs");
      this.allowPassiveMobs = tag.contains("AllowPassiveMobs") ? tag.getBoolean("AllowPassiveMobs") : !tag.contains("allowPassiveMobs") || tag.getBoolean("allowPassiveMobs");
      this.customName = tag.getString("CustomNameText");
      this.legacyComboMigrationVersion = tag.getInt("LegacyComboMigrationVersion");
      this.temporary = tag.getBoolean("Temporary");
      this.temporaryExpiresAt = this.temporary ? tag.getLong("TemporaryExpiresAt") : -1L;
      this.structureTrap = tag.getBoolean("StructureTrap");
      this.ownerUuid = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
      this.loadCenterIdentityAndTeleportLink(tag);
      this.loadAccessLists(tag);
      List<ConsumedCircle> loadedCircles = new ArrayList();
      ListTag savedCircles = tag.getList("TransformationCircles", 10);

      for(int i = 0; i < savedCircles.size(); ++i) {
         CompoundTag entry = savedCircles.getCompound(i);
         if (entry.contains("Pos", 4)) {
            loadedCircles.add(new ConsumedCircle(BlockPos.of(entry.getLong("Pos")), parseCircleType(entry.getString("Type"))));
         }
      }

      this.transformationCircles = List.copyOf(loadedCircles);
      if (tag.contains("FuelInventory", 10)) {
         this.fuelSlot.deserializeNBT(registries, tag.getCompound("FuelInventory"));
      } else if (tag.contains("inventory", 10)) {
         this.fuelSlot.deserializeNBT(registries, tag.getCompound("inventory"));
      }

      if (tag.contains("ReforgeInventory", 10)) {
         this.reforgeWeaponSlot.deserializeNBT(registries, tag.getCompound("ReforgeInventory"));
      }

      if (tag.contains("OctagonGridInventory", 10)) {
         this.octagonGrid.deserializeNBT(registries, tag.getCompound("OctagonGridInventory"));
      }

      this.pendingTransformation = null;
      if (tag.contains("PendingTransformation", 10)) {
         CompoundTag pending = tag.getCompound("PendingTransformation");
         ResourceLocation output = ResourceLocation.tryParse(pending.getString("OutputType"));
         if (output != null) {
            List<BlockPos> positions = new ArrayList();
            ListTag savedPositions = pending.getList("ConsumedPositions", 4);

            for(int i = 0; i < savedPositions.size(); ++i) {
               positions.add(BlockPos.of(((LongTag)savedPositions.get(i)).getAsLong()));
            }

            this.pendingTransformation = new PendingTransformation(output, List.copyOf(positions));
         }
      }

      this.passiveCircleCounts = MagicCenterBlockEntity.CircleCounts.ZERO;
      this.passiveCacheDirty = true;
      this.lastPassiveScan = Long.MIN_VALUE;
   }

   private static void readPlayerNameSet(ListTag list, Set<String> target, int maxSize, Set<String> ignoredNames) {
      for(int i = 0; i < list.size() && target.size() < maxSize; ++i) {
         String value = list.getString(i);
         if (isValidPlayerName(value) && findIgnoreCase(target, value) == null && findIgnoreCase(ignoredNames, value) == null) {
            target.add(value);
         }
      }

   }

   public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
      CompoundTag tag = new CompoundTag();
      tag.putString("CenterType", this.centerTypeId.toString());
      tag.putInt("Radius", this.radius);
      tag.putLong("Energy", this.energy);
      tag.putFloat("BarrierHealth", this.barrierHealth);
      tag.putBoolean("Enabled", this.enabled);
      tag.putBoolean("Active", this.active);
      tag.putBoolean("ParticlesEnabled", this.particlesEnabled);
      tag.putInt("GlyphColor", this.glyphColor);
      tag.putBoolean("AuthorizedPlayerInside", this.authorizedPlayerInside);
      tag.putLong("AuthorizedEntryGameTime", this.authorizedEntryGameTime);
      tag.putBoolean("Temporary", this.temporary);
      tag.putBoolean("StructureTrap", this.structureTrap);
      if (this.temporary) {
         tag.putLong("TemporaryExpiresAt", this.temporaryExpiresAt);
      }

      if (this.teleportChannelPlayer != null) {
         tag.putUUID("TeleportChannelPlayer", this.teleportChannelPlayer);
         tag.putLong("TeleportChannelStartTime", this.teleportChannelStartTime);
         tag.putInt("TeleportChannelDuration", this.teleportChannelDurationTicks);
      }

      tag.put("ReforgeInventory", this.reforgeWeaponSlot.serializeNBT(registries));
      tag.put("OctagonGridInventory", this.octagonGrid.serializeNBT(registries));
      return tag;
   }

   public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
      this.readClientTag(tag, registries);
   }

   public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
      this.readClientTag(packet.getTag(), registries);
   }

   private void readClientTag(CompoundTag tag, HolderLookup.Provider registries) {
      ResourceLocation type = ResourceLocation.tryParse(tag.getString("CenterType"));
      this.centerTypeId = type != null && MagicCenterTypeRegistry.contains(type) ? type : MagicCenterTypeRegistry.BLANK;
      this.radius = normalizeRadius(tag.getInt("Radius"));
      this.energy = Math.max(0L, tag.getLong("Energy"));
      this.barrierHealth = Math.max(0.0F, tag.getFloat("BarrierHealth"));
      this.enabled = tag.getBoolean("Enabled");
      this.active = tag.getBoolean("Active");
      this.particlesEnabled = tag.getBoolean("ParticlesEnabled");
      this.glyphColor = tag.getInt("GlyphColor") & 16777215;
      this.authorizedPlayerInside = tag.getBoolean("AuthorizedPlayerInside");
      this.authorizedEntryGameTime = tag.contains("AuthorizedEntryGameTime", 4) ? tag.getLong("AuthorizedEntryGameTime") : -1L;
      this.temporary = tag.getBoolean("Temporary");
      this.temporaryExpiresAt = this.temporary && tag.contains("TemporaryExpiresAt", 4) ? tag.getLong("TemporaryExpiresAt") : -1L;
      this.structureTrap = tag.getBoolean("StructureTrap");
      this.teleportChannelPlayer = tag.hasUUID("TeleportChannelPlayer") ? tag.getUUID("TeleportChannelPlayer") : null;
      this.teleportChannelStartTime = this.teleportChannelPlayer == null ? -1L : tag.getLong("TeleportChannelStartTime");
      this.teleportChannelDurationTicks = this.teleportChannelPlayer == null ? 0 : Math.max(1, tag.getInt("TeleportChannelDuration"));
      if (tag.contains("ReforgeInventory", 10)) {
         this.reforgeWeaponSlot.deserializeNBT(registries, tag.getCompound("ReforgeInventory"));
      } else {
         this.reforgeWeaponSlot.setStackInSlot(0, ItemStack.EMPTY);
      }

      if (tag.contains("OctagonGridInventory", 10)) {
         this.octagonGrid.deserializeNBT(registries, tag.getCompound("OctagonGridInventory"));
      } else {
         this.clearOctagonGrid();
      }

   }

   public Packet<ClientGamePacketListener> getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   public void dropContents() {
      if (this.level != null) {
         if (!this.suppressContentsDrop && !this.temporary && !this.structureTrap) {
            for(int slot = 0; slot < this.fuelSlot.getSlots(); ++slot) {
               ItemStack stack = this.fuelSlot.extractItem(slot, Integer.MAX_VALUE, false);
               if (!stack.isEmpty()) {
                  Containers.dropItemStack(this.level, (double)this.worldPosition.getX(), (double)this.worldPosition.getY(), (double)this.worldPosition.getZ(), stack);
               }
            }

            for(int slot = 0; slot < this.reforgeWeaponSlot.getSlots(); ++slot) {
               ItemStack stack = this.reforgeWeaponSlot.extractItem(slot, Integer.MAX_VALUE, false);
               if (!stack.isEmpty()) {
                  Containers.dropItemStack(this.level, (double)this.worldPosition.getX(), (double)this.worldPosition.getY(), (double)this.worldPosition.getZ(), stack);
               }
            }

            if (this.suppressOctagonDrop) {
               this.clearOctagonGrid();
            } else {
               for(int slot = 0; slot < this.octagonGrid.getSlots(); ++slot) {
                  ItemStack stack = this.octagonGrid.extractItem(slot, Integer.MAX_VALUE, false);
                  if (!stack.isEmpty()) {
                     Containers.dropItemStack(this.level, (double)this.worldPosition.getX(), (double)this.worldPosition.getY(), (double)this.worldPosition.getZ(), stack);
                  }
               }
            }

            long wholeStones = this.energy / 1L;

            int count;
            for(this.energy = 0L; wholeStones > 0L; wholeStones -= (long)count) {
               count = (int)Math.min(64L, wholeStones);
               Containers.dropItemStack(this.level, (double)this.worldPosition.getX(), (double)this.worldPosition.getY(), (double)this.worldPosition.getZ(), new ItemStack((ItemLike)ModItems.SPIRIT_STONE.get(), count));
            }

         } else {
            this.fuelSlot.setStackInSlot(0, ItemStack.EMPTY);
            this.reforgeWeaponSlot.setStackInSlot(0, ItemStack.EMPTY);
            this.clearOctagonGrid();
            this.energy = 0L;
         }
      }
   }

   public CompoundTag createPortableConfigurationTag(HolderLookup.Provider registries) {
      CompoundTag tag = new CompoundTag();
      tag.putInt("DataVersion", 3);
      tag.putString("CenterType", this.centerTypeId.toString());
      tag.putInt("Radius", this.radius);
      tag.putBoolean("Enabled", this.enabled);
      tag.putBoolean("ParticlesEnabled", this.particlesEnabled);
      tag.putInt("GlyphColor", this.glyphColor);
      tag.putBoolean("AllowHostileMobs", this.allowHostileMobs);
      tag.putBoolean("AllowPassiveMobs", this.allowPassiveMobs);
      tag.putString("CustomNameText", this.customName);
      if (this.ownerUuid != null) {
         tag.putUUID("Owner", this.ownerUuid);
      }

      this.saveCenterIdentityAndTeleportLink(tag);
      this.saveAccessLists(tag);
      tag.put("OctagonGridInventory", this.octagonGrid.serializeNBT(registries));
      return tag;
   }

   public void applyPortableConfigurationTag(CompoundTag tag, HolderLookup.Provider registries) {
      this.loadAdditional(tag.copy(), registries);
      this.radius = normalizeRadius(Math.min(5, this.radius));
      this.energy = 0L;
      this.fuelCostRemainder = 0L;
      this.barrierHealth = 0.0F;
      this.active = false;
      this.temporary = false;
      this.temporaryExpiresAt = -1L;
      this.structureTrap = false;
      this.suppressContentsDrop = false;
      this.suppressOctagonDrop = false;
      this.pendingTransformation = null;
      this.transformationCircles = List.of();
      this.fuelSlot.setStackInSlot(0, ItemStack.EMPTY);
      this.reforgeWeaponSlot.setStackInSlot(0, ItemStack.EMPTY);
      this.setChangedAndSync();
   }

   public boolean isTemporary() {
      return this.temporary;
   }

   public long getTemporaryExpiresAt() {
      return this.temporaryExpiresAt;
   }

   public void markTemporary(long expiresAt) {
      this.centerId = UUID.randomUUID();
      this.teleportLinkedDimension = null;
      this.teleportLinkedPos = null;
      this.teleportLinkedCenterId = null;
      this.teleportSourceCenterId = null;
      this.teleportDestinationCodes.clear();
      this.selectedTeleportDestinationCode = "";
      this.teleportChannelPlayer = null;
      this.teleportChannelStartTime = -1L;
      this.teleportChannelDurationTicks = 0;
      this.teleportChannelStartHealth = 0.0F;
      this.teleportChannelObservedHurtTime = 0;
      this.temporary = true;
      this.temporaryExpiresAt = Math.max(0L, expiresAt);
      this.active = this.enabled;
      this.energy = 0L;
      this.fuelCostRemainder = 0L;
      this.barrierHealth = 0.0F;
      this.pendingTransformation = null;
      this.transformationCircles = List.of();
      this.fuelSlot.setStackInSlot(0, ItemStack.EMPTY);
      this.reforgeWeaponSlot.setStackInSlot(0, ItemStack.EMPTY);
      this.setChangedAndSync();
   }

   private void clearOctagonGrid() {
      for(int slot = 0; slot < this.octagonGrid.getSlots(); ++slot) {
         if (!this.octagonGrid.getStackInSlot(slot).isEmpty()) {
            this.octagonGrid.setStackInSlot(slot, ItemStack.EMPTY);
         }
      }

   }

   public void removeForPacking() {
      this.suppressOctagonDrop = true;
      if (this.level != null && !this.level.isClientSide) {
         this.level.removeBlock(this.worldPosition, false);
      }

   }

   public void removeWithoutDrops() {
      this.suppressContentsDrop = true;
      if (this.level != null && !this.level.isClientSide) {
         if (this.temporary && MagicCenterTypeRegistry.TELEPORT.equals(this.centerTypeId)) {
            TeleportCenterBehavior.forgetAutomaticCenter(this);
            this.unlinkTeleportPair();
         }

         this.level.removeBlock(this.worldPosition, false);
      }

   }

   private void setChangedAndSync() {
      this.setChanged();
      if (this.level != null && !this.level.isClientSide) {
         this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 2);
      }

   }

   public static enum TeleportLinkResult {
      ADDED,
      ALREADY_LINKED,
      INVALID_CODE,
      SELF,
      NOT_FOUND,
      FULL;

      // $FF: synthetic method
      private static TeleportLinkResult[] $values() {
         return new TeleportLinkResult[]{ADDED, ALREADY_LINKED, INVALID_CODE, SELF, NOT_FOUND, FULL};
      }
   }

   public static record CircleCounts(int attack, int defense, int regeneration) {
      public static final CircleCounts ZERO = new CircleCounts(0, 0, 0);
   }

   public static record PendingTransformation(ResourceLocation outputType, List<BlockPos> consumedPositions) {
   }

   public static record ConsumedCircle(BlockPos pos, CircleType type) {
   }

   public static record ResolvedTeleportTarget(ServerLevel level, MagicCenterBlockEntity center) {
   }

   private static record LegacyComboMatch(BlockPos comboPos, ResourceLocation outputType, List<BlockPos> circlePositions) {
   }
}
