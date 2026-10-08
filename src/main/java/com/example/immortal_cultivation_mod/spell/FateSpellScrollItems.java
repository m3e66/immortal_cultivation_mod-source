package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.attachment.SpiritRoots;
import com.example.immortal_cultivation_mod.spell.ModSpells;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = ImmortalCultivationMod.MODID)
public final class FateSpellScrollItems {

    private FateSpellScrollItems() {
    }

    public static final String JIETIAN_DUOYUN_SHU =
            "jietian_duoyun_shu";

    public static final String BIJIE_SHU =
            "bijie_shu";

    public static final String GUANYUN_SHU =
            "guanyun_shu";

    public static final String YINFU_SHU =
            "yinfu_shu";

    public static final String BIHUO_JUE =
            "bihuo_jue";

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(
                    ImmortalCultivationMod.MODID
            );

    public static final DeferredItem<Item>
            JIETIAN_DUOYUN_SHU_SCROLL =
            ITEMS.register(
                    "jietian_duoyun_shu_scroll",
                    () -> new FateSpellScrollItem(
                            new Item.Properties()
                                    .stacksTo(1),
                            JIETIAN_DUOYUN_SHU
                    )
            );

    public static final DeferredItem<Item>
            BIJIE_SHU_SCROLL =
            ITEMS.register(
                    "bijie_shu_scroll",
                    () -> new FateSpellScrollItem(
                            new Item.Properties()
                                    .stacksTo(1),
                            BIJIE_SHU
                    )
            );

    public static final DeferredItem<Item>
            GUANYUN_SHU_SCROLL =
            ITEMS.register(
                    "guanyun_shu_scroll",
                    () -> new FateSpellScrollItem(
                            new Item.Properties()
                                    .stacksTo(1),
                            GUANYUN_SHU
                    )
            );

    public static final DeferredItem<Item>
            YINFU_SHU_SCROLL =
            ITEMS.register(
                    "yinfu_shu_scroll",
                    () -> new FateSpellScrollItem(
                            new Item.Properties()
                                    .stacksTo(1),
                            YINFU_SHU
                    )
            );

    public static final DeferredItem<Item>
            BIHUO_JUE_SCROLL =
            ITEMS.register(
                    "bihuo_jue_scroll",
                    () -> new FateSpellScrollItem(
                            new Item.Properties()
                                    .stacksTo(1),
                            BIHUO_JUE
                    )
            );

    private static final long LEARNING_TIMEOUT_TICKS =
            20L * 60L * 15L;


    private static final Map<UUID, PendingFateScroll>
            PENDING_SCROLLS =
            new HashMap<>();

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }

    private record PendingFateScroll(
            String spellId,
            long expiresAt
    ) {
    }

    public static class FateSpellScrollItem
            extends ModItems.SpellScrollItem {

        public FateSpellScrollItem(
                Properties properties,
                String spellId
        ) {

            super(
                    properties,
                    spellId
            );
        }


        @Override
        public InteractionResultHolder<ItemStack> use(
                Level level,
                Player player,
                InteractionHand hand
        ) {

            if (!level.isClientSide
                    && player instanceof ServerPlayer serverPlayer) {

                armPendingScroll(
                        serverPlayer,
                        spellId()
                );
            }

            return super.use(
                    level,
                    player,
                    hand
            );
        }

        @Override
        public Component getName(ItemStack stack) {

            return Component.translatable(
                            "spell."
                                    + ImmortalCultivationMod.MODID
                                    + "."
                                    + spellId()
                    )
                    .withStyle(
                            elementNameColor(
                                    spellId()
                            )
                    );
        }
    }


    private static void armPendingScroll(
            ServerPlayer player,
            String spellId
    ) {

        String normalized =
                ModSpells.normalizeId(
                        spellId
                );

        ModSpells.SpellDef spell =
                ModSpells.get(
                        normalized
                );

        if (spell == null) {

            PENDING_SCROLLS.remove(
                    player.getUUID()
            );

            return;
        }


        ModAttachments.CultivationData data =
                ModAttachments.getData(
                        player
                );

        if (data.knownSpells()
                .contains(normalized)) {

            PENDING_SCROLLS.remove(
                    player.getUUID()
            );

            return;
        }

        if (ModSpells.isInnateKnown(
                normalized,
                data
        )) {

            PENDING_SCROLLS.remove(
                    player.getUUID()
            );

            return;
        }

        if (!ModSpells.meetsRequirement(
                data.cultivationLevel(),
                spell
        )) {

            PENDING_SCROLLS.remove(
                    player.getUUID()
            );

            return;
        }

        PENDING_SCROLLS.put(
                player.getUUID(),
                new PendingFateScroll(
                        normalized,
                        player.level()
                                .getGameTime()
                                + LEARNING_TIMEOUT_TICKS
                )
        );
    }


    @SubscribeEvent
    public static void onPlayerTick(
            PlayerTickEvent.Post event
    ) {

        if (!(event.getEntity()
                instanceof ServerPlayer player)) {

            return;
        }


        UUID playerId =
                player.getUUID();


        PendingFateScroll pending =
                PENDING_SCROLLS.get(
                        playerId
                );


        if (pending == null) {
            return;
        }

        if (player.level()
                .getGameTime()
                > pending.expiresAt()) {

            PENDING_SCROLLS.remove(
                    playerId
            );

            return;
        }


        ModAttachments.CultivationData data =
                ModAttachments.getData(
                        player
                );

        if (!data.knownSpells()
                .contains(
                        pending.spellId()
                )) {

            return;
        }

        if (!player.getAbilities()
                .instabuild) {

            consumeFateScroll(
                    player,
                    pending.spellId()
            );
        }

        PENDING_SCROLLS.remove(
                playerId
        );
    }

    @SubscribeEvent
    public static void onPlayerLogout(
            PlayerEvent.PlayerLoggedOutEvent event
    ) {

        PENDING_SCROLLS.remove(
                event.getEntity()
                        .getUUID()
        );
    }

    private static void consumeFateScroll(
            ServerPlayer player,
            String spellId
    ) {

        Item target =
                scrollItemForSpell(
                        spellId
                );


        if (target == null) {
            return;
        }

        ItemStack mainHand =
                player.getMainHandItem();


        if (mainHand.is(target)) {

            mainHand.shrink(1);

            return;
        }

        ItemStack offHand =
                player.getOffhandItem();


        if (offHand.is(target)) {

            offHand.shrink(1);

            return;
        }

        for (ItemStack stack
                : player.getInventory().items) {

            if (stack.is(target)) {

                stack.shrink(1);

                return;
            }
        }
    }

    private static Item scrollItemForSpell(
            String spellId
    ) {

        return switch (
                ModSpells.normalizeId(
                        spellId
                )
                ) {

            case JIETIAN_DUOYUN_SHU ->
                    JIETIAN_DUOYUN_SHU_SCROLL.get();

            case BIJIE_SHU ->
                    BIJIE_SHU_SCROLL.get();

            case GUANYUN_SHU ->
                    GUANYUN_SHU_SCROLL.get();

            case YINFU_SHU ->
                    YINFU_SHU_SCROLL.get();

            case BIHUO_JUE ->
                    BIHUO_JUE_SCROLL.get();

            default ->
                    null;
        };
    }

    private static ChatFormatting elementNameColor(
            String spellId
    ) {

        ModSpells.SpellDef spell =
                ModSpells.get(
                        spellId
                );


        String element =
                spell == null
                        ? ""
                        : SpiritRoots.primaryElement(
                        spell.element()
                );


        return switch (element) {

            case SpiritRoots.METAL ->
                    ChatFormatting.GOLD;

            case SpiritRoots.WOOD ->
                    ChatFormatting.GREEN;

            case SpiritRoots.WATER ->
                    ChatFormatting.AQUA;

            case SpiritRoots.FIRE ->
                    ChatFormatting.RED;

            case SpiritRoots.EARTH ->
                    ChatFormatting.YELLOW;

            case SpiritRoots.THUNDER ->
                    ChatFormatting.LIGHT_PURPLE;

            case SpiritRoots.ICE ->
                    ChatFormatting.BLUE;

            case SpiritRoots.WIND ->
                    ChatFormatting.DARK_AQUA;

            case SpiritRoots.LIGHT ->
                    ChatFormatting.WHITE;

            case SpiritRoots.DARK ->
                    ChatFormatting.DARK_PURPLE;

            default ->
                    ChatFormatting.GRAY;
        };
    }

    @EventBusSubscriber(
            modid = ImmortalCultivationMod.MODID,
            bus = EventBusSubscriber.Bus.MOD
    )
    public static final class ModBusEvents {

        private ModBusEvents() {
        }


        @SubscribeEvent
        public static void buildCreativeTab(
                BuildCreativeModeTabContentsEvent event
        ) {

            ResourceLocation cultivationTab =
                    ResourceLocation.fromNamespaceAndPath(
                            ImmortalCultivationMod.MODID,
                            "immortal_cultivation_tab"
                    );

            if (!event.getTabKey()
                    .location()
                    .equals(cultivationTab)) {

                return;
            }


            event.accept(
                    GUANYUN_SHU_SCROLL.get()
            );

            event.accept(
                    YINFU_SHU_SCROLL.get()
            );

            event.accept(
                    BIHUO_JUE_SCROLL.get()
            );

            event.accept(
                    BIJIE_SHU_SCROLL.get()
            );

            event.accept(
                    JIETIAN_DUOYUN_SHU_SCROLL.get()
            );
        }
    }
}