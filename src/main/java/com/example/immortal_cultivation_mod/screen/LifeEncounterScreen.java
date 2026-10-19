package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.network.OuterSystemPayloads;
import com.example.immortal_cultivation_mod.client.OuterSystemClient;
import com.example.immortal_cultivation_mod.client.LifeDiceRenderer;
import com.example.immortal_cultivation_mod.client.LifeDiceSequence;
import com.example.immortal_cultivation_mod.system.LifeSimulationBattle;
import com.example.immortal_cultivation_mod.system.LifeSimulationSprites;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

/** Side-on sprite battle and scrollable story scenes, driven entirely by server snapshots. */
public final class LifeEncounterScreen extends LifeWindowScreen {
    private CompoundTag state;
    private String menu = "main", target = "", selectedMove = "";
    private Component initiativeTooltip;
    private int page, targetPage, scroll;
    private long animationStart;
    private boolean waiting;
    private long waitingRevision;
    private static boolean autoBattleDice, autoStoryDice, expandDetails, importantOnly;
    private static int animationSpeed;
    private static final String PREFIX = "life.battle.";

    public LifeEncounterScreen(CompoundTag state) {
        super(Component.translatable("life.battle.title"));
        this.state = state.copy(); animationStart = System.currentTimeMillis();
    }
    private CompoundTag life() { return state.getCompound("LifeSimulation"); }
    private CompoundTag battle() { return life().getCompound(LifeSimulationBattle.ROOT); }
    public void updateState(CompoundTag state) {
        boolean changed = !life().equals(state.getCompound("LifeSimulation"));
        boolean acknowledged = waiting && state.getLong("LifeActionRevision") != waitingRevision;
        if (!java.util.Objects.equals(life().get("CurrentEvent"),state.getCompound("LifeSimulation").get("CurrentEvent"))) {
            page=0;scroll=0;menu="main";
        }
        if (battle().getInt("Reveal") != state.getCompound("LifeSimulation").getCompound("Battle").getInt("Reveal")) scroll = 0;
        this.state = state.copy();
        if (acknowledged) waiting = false;
        if (changed) animationStart = System.currentTimeMillis();
        if (changed || acknowledged) rebuildWidgets();
    }
    private void send(String action) {
        if (waiting) return;
        waiting = true; waitingRevision = state.getLong("LifeActionRevision");
        PacketDistributor.sendToServer(new OuterSystemPayloads.ServerboundLifeAction(action));
    }
    private void battleAction(String action) {
        send("battle:" + battle().getUUID("Id") + ":" + battle().getInt("Revision") + ":" + action);
    }
    private Component text(String key, Object... args) { return Component.translatable(PREFIX + key, args); }

    @Override protected void initContent() {
        int width = canvasWidth(), height = canvasHeight();
        int bottom = height - 28;
        button(8, bottom, 65, 20, text("close"), this::onClose);
        button(78, bottom, 78, 20, text("settings"), () -> { menu = menu.equals("settings") ? "main" : "settings"; page = 0; rebuildWidgets(); });
        button(162,bottom,130,20,Component.translatable("life.expansion.journal"),()->OuterSystemClient.transitionTo(new LifePlanningScreen(state,true)));
        if (menu.equals("settings")) { settings(); return; }
        if (!LifeSimulationBattle.active(life())) { storyWidgets(); return; }
        CompoundTag b = battle();
        List<CompoundTag> targets = LifeSimulationBattle.activeTargets(b);
        if (targets.stream().noneMatch(a -> a.getString("Id").equals(target))) {
            target = targets.stream().filter(a -> a.getBoolean("Enemy")).map(a -> a.getString("Id")).findFirst().orElse("player");
            targetPage = Math.max(0, java.util.stream.IntStream.range(0, targets.size()).filter(i -> targets.get(i).getString("Id").equals(target)).findFirst().orElse(0) / 8);
        }
        int commandY = height - 120, left = Math.max(8, width / 2), commandW = width - left - 8;
        if (LifeSimulationBattle.pending(b)) {
            button(left, commandY + 18, commandW, 28, text("roll"), () -> battleAction("reveal"));
            button(left, commandY + 51, commandW, 22, text("skip"), () -> battleAction("skip"));
        } else if (!b.getString("Result").isBlank()) {
            button(left, commandY + 50, commandW, 28, text("continue"), () -> send("battle:continue"));
        } else if (menu.equals("main")) {
            String[] commands = {"fight", "bag", "allies", "tactics"};
            for (int i = 0; i < 4; i++) {
                String command = commands[i];
                button(left + i % 2 * (commandW / 2), commandY + i / 2 * 30,
                        commandW / 2 - 3, 27, text(command), () -> { menu = command; page = 0; selectedMove = ""; rebuildWidgets(); });
            }
            String reaction = b.getString("Reaction");
            button(left, commandY + 65, commandW, 20, text("reaction", text("reaction." + reaction)), () -> {
                List<String> reactions = List.of("brace", "dodge", "counter", "disrupt", "preserve");
                battleAction("reaction:" + reactions.get((reactions.indexOf(reaction) + 1) % reactions.size()));
            });
        } else {
            List<Choice> choices = new ArrayList<>();
            if (menu.equals("fight")) {
                for (var move : LifeSimulationBattle.moves(life())) choices.add(new Choice(Component.translatable(move.name()), () -> { selectedMove = move.id(); rebuildWidgets(); }));
            } else if (menu.equals("bag")) {
                for (var tag : life().getList("Inventory", Tag.TAG_COMPOUND)) {
                    CompoundTag item = (CompoundTag) tag;
                    if (!LifeSimulationBattle.consumable(item.getString("Id")) || item.getInt("Count") <= 0) continue;
                    var id = ResourceLocation.tryParse(item.getString("Id"));
                    var registry = net.minecraft.core.registries.BuiltInRegistries.ITEM;
                    Component name = id != null && registry.containsKey(id) ? registry.get(id).getDescription() : text("unknown_item");
                    choices.add(new Choice(name.copy().append(" ×" + item.getInt("Count")), () -> battleAction("plan:bag_" + item.getString("Id").replace(':', '~') + ":player")));
                }
            } else if (menu.equals("allies")) {
                for (var tag : b.getList("Actors", Tag.TAG_COMPOUND)) {
                    CompoundTag ally = (CompoundTag) tag;
                    if (ally.getBoolean("Enemy") || ally.getString("Kind").equals("anchor") || ally.getBoolean("Hidden") || ally.getString("Id").equals("player") || ally.getDouble("Hp") <= 0) continue;
                    for (String instruction : List.of("protect", "attack", "withdraw", "return"))
                        choices.add(new Choice(actorName(ally).copy().append(" · ").append(text("ally." + instruction)), () -> battleAction("ally:" + ally.getString("Id") + ":" + instruction)));
                    if (LifeSimulationBattle.find(b,target).getString("Kind").equals("anchor"))
                        choices.add(new Choice(actorName(ally).copy().append(" · ").append(text("ally.objective")),
                                () -> battleAction("ally:" + ally.getString("Id") + ":objective@" + target),
                                LifeSimulationBattle.allyObjectiveIllegal(life(),ally.getString("Id"),target)));
                }
            } else if (menu.equals("tactics")) {
                for (String action : List.of("guard", "reposition", "recover", "inspect", "escape", "surrender", "objective")) {
                    if (action.equals("objective") && !LifeSimulationBattle.supportsObjective(b)) continue;
                    choices.add(new Choice(text("move." + action), () -> battleAction("plan:" + action + ":" + target),
                            action.equals("objective") ? LifeSimulationBattle.objectiveIllegal(life(),target) : ""));
                }
            }
            for (int i = 0; i < 4 && page * 4 + i < choices.size(); i++) {
                Choice choice = choices.get(page * 4 + i);
                var option = button(left + i % 2 * (commandW / 2), commandY + i / 2 * 29, commandW / 2 - 3, 26, choice.text(), choice.action());
                option.active = !waiting && choice.illegal().isBlank();
                if (!choice.illegal().isBlank()) option.setTooltip(Tooltip.create(choice.text().copy().append("\n").append(text("illegal."+choice.illegal()))));
            }
            button(left, commandY + 62, commandW / 3 - 2, 20, text("back"), () -> { menu = "main"; selectedMove = ""; rebuildWidgets(); });
            if (choices.size() > 4) button(left + commandW / 3, commandY + 62, commandW / 3 - 2, 20, text("page", page + 1), () -> { page = (page + 1) % ((choices.size() + 3) / 4); rebuildWidgets(); });
            if (!selectedMove.isBlank()) {
                String reason = LifeSimulationBattle.illegal(life(), selectedMove, target);
                var confirm = button(left + 2 * commandW / 3, commandY + 62, commandW / 3 - 2, 20, text("confirm"), () -> battleAction("plan:" + selectedMove + ":" + target));
                confirm.active = !waiting && reason.isBlank(); if (!reason.isBlank()) confirm.setTooltip(Tooltip.create(text("illegal." + reason)));
            }
        }
        // Unrevealed round state must not expose newly revealed enemies or future anchors.
        if (LifeSimulationBattle.pending(b)) return;
        int pages = Math.max(1,(targets.size()+7)/8);
        targetPage = Mth.clamp(targetPage,0,pages-1);
        int n = Math.min(8,targets.size()-targetPage*8);
        int cell = (width - 16) / Math.max(1,n);
        for (int i = 0; i < n; i++) {
            CompoundTag actor = targets.get(targetPage*8+i);
            var select = button(8 + i * cell, height - 150, cell - 3, 19, actorName(actor), () -> { target = actor.getString("Id"); rebuildWidgets(); });
            select.setTooltip(Tooltip.create(actor.getString("Kind").equals("anchor") ? anchorDescription(actor)
                    : text("target", actorName(actor), number(actor.getDouble("Hp")), number(actor.getDouble("Qi")))));
        }
        if (pages > 1) button(width-120,bottom,112,20,text("targets_page",targetPage+1,pages),()->{targetPage=(targetPage+1)%pages;rebuildWidgets();});
    }

    private record Choice(Component text, Runnable action, String illegal) {
        Choice(Component text, Runnable action) { this(text,action,""); }
    }
    private void storyWidgets() {
        int width = canvasWidth(), height = canvasHeight();
        CompoundTag story = life().getCompound("Story");
        if (story.contains("PendingRoll", Tag.TAG_COMPOUND)) {
            button(width / 2 - 60, height - 66, 120, 28, text("roll"), () -> send("story:reveal")); return;
        }
        ListTag choices = life().getCompound("CurrentEvent").getList("Choices", Tag.TAG_COMPOUND);
        page=Mth.clamp(page,0,Math.max(0,(choices.size()-1)/4));
        int columns = width < 420 ? 1 : 2;
        int count = Math.min(4, choices.size() - page * 4), rows = (count + columns - 1) / columns;
        int h = 34, startY = height - 37 - rows * (h + 3) - (choices.size() > 4 ? 23 : 0);
        for (int i = 0; i < count; i++) {
            CompoundTag choice = choices.getCompound(page * 4 + i);
            int w = (width - 24) / columns;
            Component base=Component.translatable(choice.getString("TextKey"));
            if(choice.contains("Danger",Tag.TAG_INT))base=base.copy().append(" ").append(Component.translatable("screen.immortal_cultivation_mod.life.danger."+choice.getInt("Danger")));
            var label = com.example.immortal_cultivation_mod.client.LifeStoryCheckText.label(life(), choice, base);
            var select = wrappedButton(12 + i % columns * w, startY + i / columns * (h + 3), w - 4, h,
                    label, () -> send("choice:" + choice.getString("Id")));
            String reason = choice.getString("Illegal");
            select.active = !waiting && reason.isBlank();
            Component help = com.example.immortal_cultivation_mod.client.LifeStoryCheckText.tooltip(life(), choice, label);
            if (!choice.getString("requirement_key").isBlank()) help=help.copy().append("\n").append(Component.translatable(choice.getString("requirement_key")));
            if (!reason.isBlank()) help = help.copy().append("\n").append(text("choice_illegal."+reason));
            select.setTooltip(Tooltip.create(help));
        }
        if (choices.size() > 4) button(width - 95, height - 56, 82, 20, text("page", page + 1), () -> { page = (page + 1) % ((choices.size() + 3) / 4); rebuildWidgets(); });
    }

    private void settings() {
        int width = canvasWidth(), height = canvasHeight();
        List<Choice> options = List.of(
                new Choice(text("auto_battle", text(autoBattleDice ? "on" : "off")), () -> autoBattleDice = !autoBattleDice),
                new Choice(text("auto_story", text(autoStoryDice ? "on" : "off")), () -> autoStoryDice = !autoStoryDice),
                new Choice(text("details", text(expandDetails ? "on" : "off")), () -> expandDetails = !expandDetails),
                new Choice(text("important", text(importantOnly ? "on" : "off")), () -> importantOnly = !importantOnly),
                new Choice(text("speed", text("speed." + animationSpeed)), () -> animationSpeed = (animationSpeed + 1) % 3));
        int visible = Math.max(1, (height - 65) / 28);
        for (int i = 0; i < visible && page * visible + i < options.size(); i++) {
            Choice choice = options.get(page * visible + i);
            button(20, 30 + i * 28, width - 40, 24, choice.text(), () -> { choice.action().run(); rebuildWidgets(); });
        }
        if (options.size() > visible) button(width - 95, height - 28, 82, 20, text("page", page + 1), () -> { page = (page + 1) % ((options.size() + visible - 1) / visible); rebuildWidgets(); });
    }

    @Override public void tick() {
        if (waiting || menu.equals("settings")) return;
        long delay = animationSpeed == 0 ? 1000 : animationSpeed == 1 ? 250 : 50;
        if (System.currentTimeMillis() - animationStart < delay) return;
        if(LifeSimulationBattle.active(life())&&autoBattleDice&&LifeSimulationBattle.pending(battle())) {
            var frame=battle().getList("Frames",Tag.TAG_COMPOUND).getCompound(battle().getInt("Reveal"));
            if(System.currentTimeMillis()-animationStart>=LifeDiceSequence.sequenceMillis(LifeDiceSequence.pools(frame),animationSpeed))battleAction("reveal");
        }
        else if (!LifeSimulationBattle.active(life()) && autoStoryDice && life().getCompound("Story").contains("PendingRoll") && System.currentTimeMillis()-animationStart>=storyRollDuration()) send("story:reveal");
    }

    @Override protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        initiativeTooltip = null;
        if (!menu.equals("settings")) {
            if (LifeSimulationBattle.active(life())) renderBattle(g);
            else renderStory(g);
        }
        if (initiativeTooltip != null && mouseY >= 19 && mouseY < 28) g.renderTooltip(font, initiativeTooltip, mouseX, mouseY);
    }
    private void renderBattle(GuiGraphics g) {
        int width = canvasWidth(), height = canvasHeight();
        CompoundTag b = battle(); int arenaBottom = height - 155;
        String arena = b.getString("Arena");
        int color = switch (arena) { case "cave", "mine" -> 0xFFDCDDD9; case "inner" -> 0xFFE8E1EC; case "tribulation" -> 0xFFDDDDEA; case "ferry" -> 0xFFD9E5E8; default -> 0xFFE2EBDD; };
        SystemGuiTextures.drawInsetPanel(g,8,28,width-16,Math.max(22,arenaBottom-28),color);
        // Quiet field silhouettes keep the sprites, numbers and command choices readable.
        for (int i = 10; i < width-10; i += 28) g.fill(i, Math.max(30, arenaBottom - 13 - i % 19), Math.min(width-10, i + 28), arenaBottom-2, 0xFFC0CBC0);
        label(g, text("round", b.getInt("Round"), text("arena." + arena)), 10, 9);
        ListTag display = b.getList("Actors", Tag.TAG_COMPOUND);
        ListTag frames = b.getList("Frames", Tag.TAG_COMPOUND);
        int reveal = b.getInt("Reveal");
        if (!frames.isEmpty()) display = reveal == 0 ? b.getList("RoundStartActors", Tag.TAG_COMPOUND) : frames.getCompound(Math.min(reveal - 1, frames.size() - 1)).getList("Actors", Tag.TAG_COMPOUND);
        CompoundTag shown = !frames.isEmpty() && reveal > 0 ? frames.getCompound(Math.min(reveal-1,frames.size()-1)) : b;
        String phaseKey = !frames.isEmpty() && reveal == 0 ? b.getString("RoundStartPhaseKey") : shown.getString("PhaseKey");
        if (!phaseKey.isBlank()) label(g,Component.literal(font.plainSubstrByWidth(Component.translatable(phaseKey).getString(),width/2-20)),width/2+4,9);
        List<CompoundTag> visible = display.stream().map(t->(CompoundTag)t).filter(a->!a.getBoolean("Hidden")&&!a.getBoolean("Locked")).toList();
        List<CompoundTag> anchors = visible.stream().filter(a->a.getString("Kind").equals("anchor")).toList();
        List<CompoundTag> fighters = visible.stream().filter(a->!a.getString("Kind").equals("anchor")).toList();
        int friendIndex = 0, enemyIndex = 0;
        boolean compact = height < 330;
        int sideCount = Math.max((int)fighters.stream().filter(t -> !t.getBoolean("Enemy")).count(),
                (int)fighters.stream().filter(t -> t.getBoolean("Enemy")).count());
        int widthLimit = (int)((width / 2.0 - 42) / (1 + Math.max(0, sideCount - 1) * .5));
        int size = Math.max(24, Math.min(Math.min(100, widthLimit), arenaBottom - (compact ? 57 : 88)));
        for (CompoundTag actor : fighters) {
            boolean enemy = actor.getBoolean("Enemy");
            int slot = enemy ? enemyIndex++ : friendIndex++;
            int x = enemy ? width - size - 28 - slot * size / 2 : 28 + slot * size / 2;
            int y = Math.max(compact ? 57 : 88, arenaBottom - size - (slot > 0 ? 8 : 0));
            double progress = Math.min(1, (System.currentTimeMillis() - animationStart) / (animationSpeed == 0 ? 1300.0 : 340.0));
            int shift = 0;
            if (reveal > 0 && frames.getCompound(reveal - 1).getString("Actor").equals(actor.getString("Id"))) shift = (int) (Math.sin(progress * Math.PI) * 12) * (enemy ? -1 : 1);
            if (reveal > 0 && progress < 1) {
                CompoundTag frame = frames.getCompound(reveal - 1);
                for (var hitTag : frame.getList("Hits", Tag.TAG_COMPOUND)) {
                    CompoundTag hit = (CompoundTag) hitTag;
                    if (!hit.getString("Target").equals(actor.getString("Id"))) continue;
                    shift += hit.getString("Outcome").equals("miss") ? (int)(Math.sin(progress*Math.PI)*10)*(enemy?1:-1)
                            : (int)(Math.sin(progress*Math.PI*6)*3);
                    if (hit.getDouble("HpDamage") > 0) label(g, Component.literal("-"+number(hit.getDouble("HpDamage"))), x+size/2, y-(int)(progress*15));
                }
            }
            var sprite = LifeSimulationSprites.actor(life(), actor);
            int pixels = sprite.pixels();
            if (actor.getBoolean("Withdrawn") || actor.getDouble("Hp") <= 0) y += size / 3;
            g.pose().pushPose();
            g.pose().translate(x + shift, y, 0);
            // Reverse UVs, preserving front-facing quad winding so allies remain visible.
            boolean mirrored = sprite.facesRight() == enemy;
            g.blit(sprite.texture(), 0, 0, size, size, mirrored ? pixels : 0, 0,
                    mirrored ? -pixels : pixels, pixels, pixels, pixels);
            g.pose().popPose();
            if (actor.getDouble("Shield") > 0) {
                int center=x+size/2, top=y+size/5;
                g.fill(center-size/5-1,top,center-size/5+1,y+size,0xFF7EC5E7);
                g.fill(center+size/5-1,top,center+size/5+1,y+size,0xFF7EC5E7);
            }
            if (actor.getString("Id").equals(target)) { g.fill(x + size / 2 - 3, y - 7, x + size / 2 + 4, y - 3, 0xFFFFDC69); }
            if (slot == 0) stats(g, actor, enemy ? width / 2 + 4 : 12, 32, width / 2 - 16, b.getInt("Round"));
        }
        for (int i=0;i<anchors.size();i++) {
            CompoundTag anchor = anchors.get(i);
            int x=width/2+(i-(anchors.size()-1)/2)*24, y=arenaBottom-27;
            int anchorColor=anchor.getBoolean("Resolved")?0xFF78997B:anchor.getBoolean("Protected")?0xFF58A6C8:0xFFAA8E4C;
            g.fill(x-8,y-5,x+8,y+5,anchorColor);g.fill(x-5,y-8,x+5,y+8,anchorColor);
            g.fill(x-4,y-4,x+4,y+4,0xFFEDEBE2);
            if (anchor.getString("Id").equals(target)) g.fill(x-4,y-13,x+5,y-10,0xFFFFDC69);
        }
        int commandY = height - 120;
        SystemGuiTextures.drawInsetPanel(g,8,commandY-10,width/2-13,height-34-(commandY-10));
        Component objective = Component.translatable(b.getString("ObjectiveKey"));
        label(g, Component.literal(font.plainSubstrByWidth(objective.getString(), Math.max(80, width / 2 - 26))), 13, commandY - 7);
        int objectiveValue=!frames.isEmpty()&&reveal==0?b.getInt("RoundStartObjective"):shown.getInt("Objective");
        int hazardValue=!frames.isEmpty()&&reveal==0?b.getInt("RoundStartHazard"):shown.getInt("Hazard");
        if (LifeSimulationBattle.supportsObjective(b)) label(g, text("objective", objectiveValue, b.getInt("Goal"), hazardValue), 13, commandY + 4);
        if (!selectedMove.isBlank() && !LifeSimulationBattle.pending(b)) {
            var move = LifeSimulationBattle.moves(life()).stream().filter(m -> m.id().equals(selectedMove)).findFirst().orElse(null);
            if (move != null) {
                CompoundTag preview = LifeSimulationBattle.preview(life(), move, target);
                wrapped(g, text("preview", Component.translatable(move.name()), actorName(LifeSimulationBattle.find(b, target)),
                        (int) (preview.getDouble("Hit") * 100), number(preview.getDouble("Low")), number(preview.getDouble("High")), number(preview.getDouble("Qi")), move.cooldown(), text("effect." + (move.effect().isBlank() ? "none" : move.effect()))), 13, commandY + 17, Math.max(80, width / 2 - 26), 7);
            }
        } else if (reveal > 0) details(g, frames.getCompound(reveal - 1), 13, commandY + 17, Math.max(80, width / 2 - 26));
        else {
            CompoundTag selected = LifeSimulationBattle.find(b,target);
            Component content = selected.getString("Kind").equals("anchor") ? anchorDescription(selected)
                    : !b.getString("HistoryKey").isBlank() ? Component.translatable(b.getString("HistoryKey"))
                    : text("choose",text("reaction."+b.getString("Reaction")));
            scrollableText(g,content,13,commandY+17,Math.max(80,width/2-26),6);
        }
        if (!b.getString("Result").isBlank() && !LifeSimulationBattle.pending(b)) label(g, text("result." + b.getString("Result")), 13, commandY + 64);
        if (!b.getList("Queue", Tag.TAG_STRING).isEmpty()) {
            Component order = text("initiative");
            for (var id : b.getList("Queue", Tag.TAG_STRING)) {
                CompoundTag actor = LifeSimulationBattle.find(b, id.getAsString());
                order = order.copy().append(" ").append(actorName(actor)).append("(" + actor.getInt("Initiative") + ")");
            }
            // Keep initiative above the arena, including at the smallest GUI size.
            label(g, Component.literal(font.plainSubstrByWidth(order.getString(), width - 24)), 12, 19);
            initiativeTooltip = order;
        }
        if(LifeSimulationBattle.pending(b))renderLegacyDice(g,frames.getCompound(reveal));
    }
    private void renderLegacyDice(GuiGraphics g,CompoundTag frame){
        var pools=LifeDiceSequence.pools(frame);if(pools.isEmpty())return;
        long elapsed=System.currentTimeMillis()-animationStart;
        int index=LifeDiceSequence.index(pools,elapsed,animationSpeed);
        var pool=pools.get(index);int width=canvasWidth();
        labelDice(g,Component.translatable("life.dice.pool",index+1,pools.size(),Component.translatable("life.dice.kind."+pool.kind())),width/2-150,29);
        LifeDiceRenderer.drawPool(g,pool.roll(),width/2F,143,30,
                elapsed-index*LifeDiceSequence.poolMillis(animationSpeed),storyRollDuration(),300,0xFF263C31);
    }
    private void labelDice(GuiGraphics g,Component text,int x,int y){
        float scale=Math.min(1,300F/Math.max(1,font.width(text)));
        g.pose().pushPose();g.pose().translate(x,y,0);g.pose().scale(scale,scale,1);g.drawString(font,text,0,0,0xFF263C31,false);g.pose().popPose();
    }
    private Component anchorDescription(CompoundTag anchor) {
        Component status=text("anchor."+(anchor.getBoolean("Resolved")?"resolved":anchor.getBoolean("Protected")?"protected":anchor.getBoolean("Identified")?"identified":"unknown"));
        Component description = text("anchor",actorName(anchor),number(anchor.getDouble("Hp")),number(anchor.getDouble("MaxHp")),status);
        if (!anchor.getString("MemoryKey").isBlank()) description=description.copy().append("\n").append(Component.translatable(anchor.getString("MemoryKey")));
        return description;
    }
    private void scrollableText(GuiGraphics g,Component content,int x,int y,int w,int capacity) {
        var lines=font.split(content,w);
        scroll=Mth.clamp(scroll,0,Math.max(0,lines.size()-capacity));
        for(int i=0;i<capacity&&i+scroll<lines.size();i++) g.drawString(font,lines.get(i+scroll),x,y+i*9,0xFF202020,false);
        if(lines.size()>capacity) label(g,text("scroll",scroll+1,lines.size()),x,canvasHeight()-43);
    }
    private void stats(GuiGraphics g, CompoundTag actor, int x, int y, int w, int round) {
        if (canvasHeight() < 330) {
            g.fill(x, y, x+w, y+23, 0xD91C2E2B);
            scissorCanvas(g,x,y,x+w,y+23);
            label(g, actorName(actor), x+3,y+1);
            bar(g,x+3,y+11,w-6,actor.getDouble("Hp"),actor.getDouble("MaxHp"),0xFF60BD68);
            bar(g,x+3,y+19,w-6,actor.getDouble("Qi"),actor.getDouble("MaxQi"),0xFF59A2D0);
            g.disableScissor();return;
        }
        SystemGuiTextures.drawInsetPanel(g,x,y,w,59);
        scissorCanvas(g,x,y,x+w,y+59);
        label(g, actorName(actor).copy().append(" · ").append(realm(actor.getInt("Realm"))), x + 4, y + 3);
        bar(g, x + 4, y + 15, w - 8, actor.getDouble("Hp"), actor.getDouble("MaxHp"), 0xFF60BD68);
        bar(g, x + 4, y + 26, w - 8, actor.getDouble("Qi"), actor.getDouble("MaxQi"), 0xFF59A2D0);
        label(g, text("resources", number(actor.getDouble("Hp")), number(actor.getDouble("MaxHp")), number(actor.getDouble("Qi")), number(actor.getDouble("Shield"))), x + 4, y + 37);
        if (actor.getBoolean("Enemy")) label(g, text("intent", actor.getBoolean("Identified") ? actorMoveName(actor, actor.getString("Plan")) : text("intent_unknown")), x + 4, y + 47);
        g.disableScissor();
    }
    private void bar(GuiGraphics g, int x, int y, int w, double value, double max, int color) {
        g.fill(x, y, x + w, y + 7, 0xFF101F1B); g.fill(x, y, x + (int) (w * Mth.clamp(value / Math.max(1, max), 0, 1)), y + 7, color);
    }
    private void details(GuiGraphics g, CompoundTag frame, int x, int y, int w) {
        int height = canvasHeight();
        Component content = text("action", actorMoveName(LifeSimulationBattle.find(battle(), frame.getString("Actor")), frame.getString("Action")));
        ListTag hits = frame.getList("Hits", Tag.TAG_COMPOUND);
        if (importantOnly && hits.isEmpty() && !frame.contains("Roll") && !frame.contains("DamageRoll") && !frame.contains("FinalWillRoll") && !frame.contains("Amount") && frame.getString("StoryMessageKey").isBlank()) { wrapped(g,content,x,y,w,2); return; }
        if (!hits.isEmpty()) {
            CompoundTag hit = hits.getCompound(0), attack = hit.getCompound("Attack"), damage = hit.getCompound("DamageRoll");
            content = text("roll_details", text("outcome." + hit.getString("Outcome")), attack.getInt("Selected"), attack.getInt("Modifier"), attack.getInt("Total"), attack.getInt("DC"),
                    java.util.Arrays.toString(damage.getIntArray("Dice")), number(hit.getDouble("Raw")), number(hit.getDouble("Shield")), number(hit.getDouble("HpDamage")));
            if (hits.size() > 1) content = content.copy().append(" ").append(text("grouped", hits.size()));
            if (!expandDetails) content = text("hit_summary", text("outcome." + hit.getString("Outcome")), number(hit.getDouble("HpDamage")), number(hit.getDouble("Shield")));
        } else if (frame.contains("Roll")) {
            CompoundTag roll = frame.getCompound("Roll"); content = text("check_details", roll.getInt("Selected"), roll.getInt("Modifier"), roll.getInt("Total"), roll.getInt("DC"), text("degree." + roll.getInt("Degree")));
            if (roll.getInt("Sides") != 20) content = text("damage_dice",java.util.Arrays.toString(roll.getIntArray("Dice")),roll.getInt("Total"));
            else if (!expandDetails) content = text("check_summary", text("degree." + roll.getInt("Degree")), roll.getInt("Total"), roll.getInt("DC"));
        }
        if (hits.isEmpty() && frame.contains("Amount")) content=content.copy().append("\n").append(text("support_result",actorName(LifeSimulationBattle.find(battle(),frame.getString("Target"))),number(frame.getDouble("Amount"))));
        if (hits.isEmpty() && frame.contains("DamageRoll")) {
            CompoundTag roll=frame.getCompound("DamageRoll");
            content=content.copy().append("\n").append(text("damage_dice",java.util.Arrays.toString(roll.getIntArray("Dice")),roll.getInt("Total")));
        }
        if (frame.contains("FinalWillRoll",Tag.TAG_COMPOUND)) {
            CompoundTag will=frame.getCompound("FinalWillRoll");
            content=content.copy().append("\n").append(text("final_will",will.getInt("Selected"),will.getInt("Modifier"),will.getInt("Total"),will.getInt("DC"),text("degree."+will.getInt("Degree"))));
        }
        if(!frame.getString("StoryMessageKey").isBlank()) content=content.copy().append("\n").append(Component.translatable(frame.getString("StoryMessageKey")));
        if(!frame.getString("WarningKey").isBlank()) content=content.copy().append("\n").append(Component.translatable(frame.getString("WarningKey")));
        int textWidth = w;
        var lines = font.split(content, textWidth);
        int capacity = Math.max(1, Math.min(expandDetails ? 8 : 6, (height - 35 - y) / 9));
        if (lines.size() > capacity) capacity = Math.max(1, capacity - 1);
        scroll = Mth.clamp(scroll, 0, Math.max(0, lines.size() - capacity));
        for (int i = 0; i < capacity && i + scroll < lines.size(); i++) g.drawString(font, lines.get(i + scroll), x, y + i * 9, 0xFF202020, false);

        if (lines.size() > capacity) label(g, text("scroll", scroll + 1, lines.size()), x, height - 43);
    }
    private Component moveName(String id) {
        if (id.startsWith("bag_")) return text("bag");
        var move=LifeSimulationBattle.moves(life()).stream().filter(m->m.id().equals(id)).findFirst().orElse(null);
        return move == null ? text("move."+id) : Component.translatable(move.name());
    }
    private Component actorMoveName(CompoundTag actor, String id) {
        var move = LifeSimulationBattle.actorMoves(life(), actor).stream().filter(m -> m.id().equals(id)).findFirst().orElse(null);
        return move == null ? moveName(id) : Component.translatable(move.name());
    }
    private long storyRollDuration(){return LifeDiceSequence.rollMillis(animationSpeed);}
    private void renderStory(GuiGraphics g) {
        int width = canvasWidth(), height = canvasHeight();
        CompoundTag event = life().getCompound("CurrentEvent"), story = life().getCompound("Story");
        label(g, event.getString("TitleKey").isBlank()?text("title"):Component.translatable(event.getString("TitleKey")), 12, 10);
        var location=com.example.immortal_cultivation_mod.system.LifeSimulationWorldMap.location(life().getCompound("WorldMap").getString("CurrentLocation"));
        Component locationName=!event.getString("LocationKey").isBlank()?Component.translatable(event.getString("LocationKey"))
                :location!=null?Component.translatable(location.nameKey()):life().getString("Birthplace").isBlank()?Component.literal("—"):Component.translatable("screen.immortal_cultivation_mod.life.birthplace."+life().getString("Birthplace"));
        label(g, text("date", life().getInt("Age"), locationName, life().getInt("AnnualActionPoints")), 12, 23);
        int choices = event.getList("Choices", Tag.TAG_COMPOUND).size(), columns = width < 420 ? 1 : 2;
        int rows = (Math.min(4, choices) + columns - 1) / columns;
        int bottom = height - 59 - rows * 37 - (choices > 4 ? 23 : 0);
        boolean pending=story.contains("PendingRoll"),challenge=story.getInt("Successes")>0||story.getInt("Failures")>0;
        if(pending)bottom=height-(challenge?193:179);
        int panelTop = 40, panelBottom = Math.max(panelTop + 20, bottom);
        SystemGuiTextures.drawInsetPanel(g,10,panelTop,width-20,panelBottom-panelTop);
        var lines = font.split(com.example.immortal_cultivation_mod.system.LifeAuthoredStories.text(event), width - 36);
        int capacity = Math.max(1, (panelBottom - panelTop - 14) / 11);
        scroll = Mth.clamp(scroll, 0, Math.max(0, lines.size() - capacity));
        scissorCanvas(g,14,panelTop+4,width-14,panelBottom-4);
        for (int i = 0; i < capacity && scroll + i < lines.size(); i++) g.drawString(font, lines.get(scroll + i), 18, panelTop + 7 + i * 11, 0xFF202020, false);
        g.disableScissor();
        if (lines.size() > capacity) label(g, text("scroll", scroll + 1, lines.size()), width - 100, panelBottom - 10);
        if(challenge)label(g,text("challenge",story.getInt("Successes"),story.getInt("Failures"),story.getInt("Meter")),14,panelBottom+5);
        if(pending) {
            var roll=story.getCompound("PendingRoll");
            LifeDiceRenderer.drawPool(g,roll,width/2F,height-130,28,System.currentTimeMillis()-animationStart,storyRollDuration(),width-70,0xFF263C31);
            Component summary=text("pending_check",text("purpose."+roll.getString("Purpose")),roll.getInt("Modifier"),roll.getInt("DC"));
            var summaryLines=font.split(summary,width-28);
            for(int i=0;i<Math.min(2,summaryLines.size());i++)g.drawString(font,summaryLines.get(i),14,height-91+i*(font.lineHeight+2),0xFF202020,false);
        }
    }
    private Component actorName(CompoundTag actor) {
        String name = actor.getString("Name");
        return name.startsWith("life.") ? Component.translatable(name) : Component.literal(name);
    }
    private Component realm(int value) {
        if (value <= 0) return Component.translatable("screen.immortal_cultivation_mod.life.realm.mortal");
        return Component.translatable("screen.immortal_cultivation_mod.life.realm." + Math.min(9, (value - 1) / 3 + 1))
                .append(" ").append(Component.translatable("screen.immortal_cultivation_mod.life.stage." + (value - 1) % 3));
    }
    private static String number(double number) {
        if (Math.abs(number)>=1_000_000_000) return String.format(java.util.Locale.ROOT,"%.1fB",number/1_000_000_000);
        if (Math.abs(number)>=1_000_000) return String.format(java.util.Locale.ROOT,"%.1fM",number/1_000_000);
        if (Math.abs(number)>=10_000) return String.format(java.util.Locale.ROOT,"%.1fK",number/1_000);
        return String.format(java.util.Locale.ROOT, "%.0f", number);
    }
    private void label(GuiGraphics g, Component text, int x, int y) { g.drawString(font, text, x, y, 0xFF202020, false); }
    private void wrapped(GuiGraphics g, Component text, int x, int y, int w, int max) {
        var lines = font.split(text, w); for (int i = 0; i < Math.min(max, lines.size()); i++) g.drawString(font, lines.get(i), x, y + i * 9, 0xFF202020, false);
    }
    @Override protected boolean scrollContent(double x, double y, double horizontal, double vertical) {
        scroll = Math.max(0, scroll - (int) (vertical * 3)); return true;
    }
}
