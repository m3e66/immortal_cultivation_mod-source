package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.client.LifeDiceRenderer;
import com.example.immortal_cultivation_mod.client.LifeDiceSequence;
import com.example.immortal_cultivation_mod.client.LifeBattleSmoke;
import com.example.immortal_cultivation_mod.client.OuterSystemClient;
import com.example.immortal_cultivation_mod.config.ModClientConfig;
import com.example.immortal_cultivation_mod.network.OuterSystemPayloads;
import com.example.immortal_cultivation_mod.system.*;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/** Playable card hand and pixel sprite arena, driven by saved server state. */
public final class LifeCardBattleScreen extends LifeWindowScreen {
    private CompoundTag state;
    private final List<SmokeImpact> smokeImpacts=new ArrayList<>();
    private String smokeFrame="";
    private CompoundTag diceFrame;
    private List<LifeDiceSequence.Pool> dicePools=List.of();
    private record SmokeImpact(int x,long started,int seed) {}
    private String selected="",target="player",picker="",supportMode="slot",animationKey="",dragTarget="";
    private int slot,page,hoveredHand=-1;
    private boolean waiting,dragging,dragMoved;
    private SystemGuiButton playButton;
    private double dragStartX,dragStartY,dragX,dragY;
    private long waitingRevision,animationStart;
    public LifeCardBattleScreen(CompoundTag state){super(text("title"));this.state=state.copy();animationStart=System.currentTimeMillis();}
    private CompoundTag life(){return state.getCompound("LifeSimulation");}
    private CompoundTag battle(){return life().getCompound("Battle");}
    private static Component text(String key,Object...args){return Component.translatable("life.cards."+key,args);}
    public void updateState(CompoundTag state){
        boolean acknowledged=waiting&&state.getLong("LifeActionRevision")!=waitingRevision;
        boolean changed=!life().equals(state.getCompound("LifeSimulation"));this.state=state.copy();
        if(acknowledged)waiting=false;
        if(!battle().getString("CardPhase").equals("prepare")||!battle().getString("Result").isBlank())picker="";
        if(!selected.isBlank()&&LifeCardBattle.handCard(battle(),selected).isEmpty()){selected="";dragging=false;dragMoved=false;}
        if(changed||acknowledged)rebuildWidgets();
    }
    private void send(String command){
        if(waiting)return;waiting=true;waitingRevision=state.getLong("LifeActionRevision");
        PacketDistributor.sendToServer(new OuterSystemPayloads.ServerboundLifeAction("battle:"+battle().getUUID("Id")+":"+battle().getInt("Revision")+":"+command));
        rebuildWidgets();
    }
    private void continueStory(){PacketDistributor.sendToServer(new OuterSystemPayloads.ServerboundLifeAction("battle:continue"));}
    private void animationMode(){int next=(ModClientConfig.LIFE_CARD_ANIMATION.get()+1)%3;ModClientConfig.LIFE_CARD_ANIMATION.set(next);ModClientConfig.SPEC.save();rebuildWidgets();}
    @Override public void tick(){
        if(LifeSimulationBattle.pending(battle())&&!waiting){
            String key=battle().getUUID("Id")+":"+battle().getInt("Reveal")+":"+battle().getList("Frames",Tag.TAG_COMPOUND).size()+":"+battle().getCompound("RoundStartActors");
            CompoundTag frame=battle().getList("Frames",Tag.TAG_COMPOUND).getCompound(battle().getInt("Reveal"));
            if(frame.getCompound("Roll").hasUUID("Id"))key=frame.getCompound("Roll").getUUID("Id").toString();
            if(!key.equals(animationKey)){animationKey=key;animationStart=System.currentTimeMillis();}
            long duration=frameDuration();
            if(System.currentTimeMillis()-animationStart>=duration)send("reveal");
        }
    }
    @Override protected void initContent(){
        int h=canvasHeight();
        if(!picker.isBlank()){initPicker();return;}
        if(!battle().getString("Result").isBlank()&&!LifeSimulationBattle.pending(battle())){
            button(345,h-30,235,26,text("continue"),this::continueStory);return;
        }
        if(battle().getString("CardPhase").equals("prepare")){initPreparation();return;}
        button(4,4,50,20,text("close"),this::onClose);
        button(341,4,115,20,text("journal"),()->OuterSystemClient.transitionTo(new LifePlanningScreen(state,true)));
        button(461,4,127,20,text("animation",text("speed."+ModClientConfig.LIFE_CARD_ANIMATION.get())),this::animationMode);
        var play=playButton=button(477,263,107,31,text("play"),()->send("play:"+selected+":"+target));
        String reason=LifeCardBattle.illegal(life(),selected,target);play.active=!waiting&&reason.isBlank();
        if(!reason.isBlank())play.setTooltip(Tooltip.create(text("illegal."+reason)));
        var end=button(477,302,107,31,text("end"),()->send("end"));end.active=!waiting&&!LifeSimulationBattle.pending(battle());
        if(LifeSimulationBattle.pending(battle()))button(477,229,107,25,text("skip"),()->send("skip")).active=!waiting;
    }
    private void preparationFooter(){
        button(6,canvasHeight()-30,98,26,text("withdraw"),()->send("withdraw")).active=!waiting;
        button(110,canvasHeight()-30,310,26,text("animation",text("speed."+ModClientConfig.LIFE_CARD_ANIMATION.get())),this::animationMode);
        var ready=button(426,canvasHeight()-30,160,26,text("ready"),()->send("ready"));ready.active=!waiting&&LifeCardBattle.player(life()).getDouble("Hp")>0;
        if(!ready.active)ready.setTooltip(Tooltip.create(text("illegal.knocked_out")));
    }
    private CompoundTag companion(){return battle().getList("Actors",Tag.TAG_COMPOUND).stream().map(t->(CompoundTag)t).filter(t->t.getBoolean("Companion")).findFirst().orElse(new CompoundTag());}
    private void openPreparation(String category){picker=category;slot=0;page=0;rebuildWidgets();}
    private void initPreparation(){
        var deck=button(12,57,278,126,text("category.deck"),()->openPreparation("deck"));deck.active=!waiting;
        deck.setArtwork((g,hover)->renderPreparationCategory(g,true,hover));
        var support=button(302,57,278,126,text("category.support"),()->openPreparation("support"));support.active=!waiting;
        support.setArtwork((g,hover)->renderPreparationCategory(g,false,hover));
        preparationFooter();
    }
    private void renderPreparationCategory(GuiGraphics g,boolean deck,boolean hover){
        int lift=hover?-4:0;
        // Both categories are physical decks with the same fan, front card and hover motion.
        for(int i=0;i<4;i++){
            g.pose().pushPose();g.pose().translate(139+(i-2)*13,96+lift,0);
            g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees((i-2)*9));
            drawPreparationCardBack(g,-32,-88,64,88,hover);
            g.flush();g.pose().popPose();
        }
        LifeCardCatalog.Card card=null;
        if(deck){
            var cards=battle().getList("RegularDeck",Tag.TAG_STRING);
            if(!cards.isEmpty())card=LifeCardCatalog.get(life(),LifeCardBattle.player(life()),cards.getString(0));
        }else{
            for(var tag:battle().getList("ConsumableSlots",Tag.TAG_STRING)){
                if(!tag.getAsString().isBlank()){card=LifeCardCatalog.item(LifeCardBattle.player(life()),tag.getAsString());if(card!=null)break;}
            }
        }
        g.pose().pushPose();g.pose().translate(113,3+lift,1);g.pose().scale(.72F,.72F,1);
        cardFrame(g,0,0,88,132,card==null?0xFF81603D:cardAccent(card),hover);
        if(card!=null){
            g.drawString(font,Integer.toString(card.energy()),7,7,0xFFF8EAD3,false);
            var lines=font.split(name(card),74);
            for(int i=0;i<Math.min(3,lines.size());i++)g.drawString(font,lines.get(i),(88-font.width(lines.get(i)))/2,27+i*10,0xFF2D4136,false);
            g.pose().pushPose();g.pose().translate(28,65,1);g.pose().scale(1.6F,1.6F,1);icon(g,card,0,0);g.flush();g.pose().popPose();
        }else fittedLine(g,text("empty"),7,45,74,0xFF635840);
        g.fill(21,108,67,109,0xFFB79D63);g.fill(30,113,58,114,0xFFB79D63);
        g.flush();g.pose().popPose();
        Component label=text(deck?"category.deck":"category.support");
        g.drawString(font,label,(278-font.width(label))/2,112,hover?0xFF996B25:0xFF30483B,false);
        if(hover)g.fill(112,124,166,125,0xFFB7914F);
    }
    private void drawPreparationCardBack(GuiGraphics g,int x,int y,int w,int h,boolean hover){
        g.fill(x+3,y+3,x+w+3,y+h+3,0x30000000);
        g.fill(x,y,x+w,y+h,hover?0xFFC69C51:0xFF697A65);
        g.fill(x+2,y+2,x+w-2,y+h-2,0xFFE5D7B4);
        g.fill(x+4,y+4,x+w-4,y+h-4,0xFF345648);
        g.renderOutline(x+7,y+7,w-14,h-14,0xFFB79D63);
        int cx=x+w/2,cy=y+h/2;
        for(int row=-12;row<=12;row++){int half=12-Math.abs(row);g.fill(cx-half,cy+row,cx+half+1,cy+row+1,0xFFB79D63);}
        g.fill(cx-2,cy-7,cx+3,cy+8,0xFF345648);
    }
    private List<String> choices(){
        if(picker.equals("deck")){
            var equipped=battle().getList("RegularDeck",Tag.TAG_STRING);
            return LifeCardCatalog.available(life(),LifeCardBattle.player(life())).stream().map(LifeCardCatalog.Card::id)
                    .filter(id->equipped.stream().noneMatch(t->t.getAsString().equals(id))).distinct().toList();
        }
        if(supportMode.equals("slot")){
            var equipped=battle().getList("ConsumableSlots",Tag.TAG_STRING);
            return LifeCardBattle.consumables(life()).stream().filter(id->equipped.stream().filter(t->t.getAsString().equals(id)).count()<LifeSimulationBattle.itemCount(life(),id)).toList();
        }
        CompoundTag current=companion();
        return LifeCardBattle.companions(life()).stream().filter(n->!current.hasUUID("NpcId")||!n.getUUID("Id").equals(current.getUUID("NpcId"))).map(n->n.getUUID("Id").toString()).toList();
    }
    private Component choiceLabel(String value){
        if(picker.equals("deck")){var card=LifeCardCatalog.get(life(),LifeCardBattle.player(life()),value);return name(card).copy().append("\n").append(text("cost",card.energy(),number(card.qi()))).append(" · ").append(text("copy_limit",card.copies()));}
        if(supportMode.equals("slot"))return itemName(value).copy().append("\n").append(text("owned",LifeSimulationBattle.itemCount(life(),value)));
        var person=LifeCardBattle.companions(life()).stream().filter(n->n.getUUID("Id").toString().equals(value)).findFirst().orElse(new CompoundTag());
        return actorName(person).copy().append("\n").append(text("injury",number(LifeCardInjuries.hp(person)),number(LifeCardInjuries.maxHp(person)),person.getInt("KnockoutStreak")));
    }
    private void initPicker(){
        button(10,35,100,24,text("category.deck"),()->openPreparation("deck")).active=!waiting;
        button(116,35,100,24,text("category.support"),()->openPreparation("support")).active=!waiting;
        button(468,35,114,24,text("back"),()->{picker="";rebuildWidgets();}).active=!waiting;
        if(picker.equals("support")){
            button(236,35,110,24,text("section.items"),()->{supportMode="slot";slot=0;page=0;rebuildWidgets();}).active=!waiting;
            button(352,35,110,24,text("section.companion"),()->{supportMode="companion";page=0;rebuildWidgets();}).active=!waiting;
        }
        if(picker.equals("deck")){
            var deck=battle().getList("RegularDeck",Tag.TAG_STRING);slot=Mth.clamp(slot,0,Math.max(0,deck.size()-1));
            for(int i=0;i<deck.size();i++){
                var card=LifeCardCatalog.get(life(),LifeCardBattle.player(life()),deck.getString(i));
                Component title=card==null?text("unknown"):name(card);
                preparationSlot(i,title,title,card==null?Component.empty():text("cost",card.energy(),number(card.qi())),
                        card==null?0xFF697A65:cardAccent(card));
            }
        }else if(supportMode.equals("slot")){
            var items=battle().getList("ConsumableSlots",Tag.TAG_STRING);slot=Mth.clamp(slot,0,Math.max(0,items.size()-1));
            for(int i=0;i<items.size();i++){
                String value=items.getString(i);
                var card=value.isBlank()?null:LifeCardCatalog.item(LifeCardBattle.player(life()),value);
                Component title=value.isBlank()?text("empty"):itemName(value);
                preparationSlot(i,text("support_slot",i+1,title),title,
                        card==null?text("support_slot",i+1,text("empty")):text("cost",card.energy(),number(card.qi())),
                        card==null?0xFF697A65:cardAccent(card));
            }
            button(12,235,272,24,text("clear_slot"),()->send("slot:"+slot+":none")).active=!waiting&&!items.getString(slot).isBlank();
        }else{
            var current=companion();
            wrappedButton(12,82,272,80,text("companion",current.isEmpty()?text("none"):actorName(current)).copy().append("\n").append(current.isEmpty()?Component.empty():text("injury",number(current.getDouble("Hp")),number(current.getDouble("MaxHp")),current.getInt("KnockoutStreak"))),()->{}).active=false;
            var modes=List.of("offensive","defensive","supportive");String behavior=life().getString("CardCompanionBehavior");
            wrappedButton(12,173,272,48,text("behavior",text("behavior."+(modes.contains(behavior)?behavior:"offensive"))),()->send("behavior:"+modes.get((Math.max(0,modes.indexOf(behavior))+1)%3))).active=!waiting;
            button(12,273,272,24,text("clear_companion"),()->send("companion:none")).active=!waiting&&!current.isEmpty();
        }
        var options=choices();page=Mth.clamp(page,0,Math.max(0,(options.size()-1)/5));
        for(int i=0;i<Math.min(5,options.size()-page*5);i++){
            String value=options.get(page*5+i);
            var choice=wrappedButton(314,82+i*38,266,34,choiceLabel(value),()->{
                if(picker.equals("deck"))send("deck:"+slot+":"+value);
                else if(supportMode.equals("slot"))send("slot:"+slot+":"+value.replace(':','~'));
                else send("companion:"+value);
            });choice.active=!waiting;
            if(picker.equals("deck")){
                var proposed=battle().getList("RegularDeck",Tag.TAG_STRING).copy();proposed.set(slot,net.minecraft.nbt.StringTag.valueOf(value));
                String reason=LifeCardBattle.deckIllegal(life(),proposed);if(!reason.isBlank()){choice.active=false;choice.setTooltip(Tooltip.create(text("illegal."+reason)));}
            }
        }
        button(314,273,128,24,text("previous"),()->{page--;rebuildWidgets();}).active=page>0&&!waiting;
        button(448,273,132,24,text("next"),()->{page++;rebuildWidgets();}).active=(page+1)*5<options.size()&&!waiting;
        preparationFooter();
    }
    /** Attack and support decks share slot geometry, artwork, hover and selection behavior. */
    private void preparationSlot(int index,Component narration,Component title,Component detail,int accent){
        var entry=wrappedButton(12+(index%3)*89,82+(index/3)*38,83,34,narration,()->{slot=index;rebuildWidgets();});
        entry.active=!waiting;
        entry.setTooltip(Tooltip.create(narration.copy().append("\n").append(detail)));
        entry.setArtwork((g,hover)->{
            cardFrame(g,0,0,83,34,accent,hover||slot==index);
            g.fill(5,5,78,29,0xFFF2E9D2);g.fill(5,5,8,29,accent);
            fittedLine(g,title,12,8,65,0xFF2D4136);
            fittedLine(g,detail,12,20,65,0xFF635840);
        });
    }
    @Override protected void renderContent(GuiGraphics g,int mx,int my,float partial){
        if(!picker.isBlank()){renderPreparation(g);renderPreparationPicker(g);return;}
        if(battle().getString("CardPhase").equals("prepare")&&battle().getString("Result").isBlank()){renderPreparation(g);return;}
        renderArena(g);
        if(!battle().getString("Result").isBlank()&&!LifeSimulationBattle.pending(battle())){
            g.drawString(font,text("result."+battle().getString("Result")),16,216,0xFF713F30,false);
            g.drawString(font,text("injury",number(LifeCardBattle.player(life()).getDouble("Hp")),number(LifeCardBattle.player(life()).getDouble("MaxHp")),life().getInt("KnockoutStreak")),16,235,0xFF293D36,false);return;
        }
        ListTag hand=battle().getList("Hand",Tag.TAG_COMPOUND);
        hoveredHand=LifeSimulationBattle.pending(battle())||dragMoved?-1:handAt(mx,my);
        int front=hoveredHand;
        if(front<0)for(int i=0;i<hand.size();i++)if(hand.getCompound(i).getString("Instance").equals(selected))front=i;
        scissorCanvas(g,0,25,canvasWidth(),canvasHeight());
        for(int i=0;i<hand.size();i++)if(i!=front&&!(dragMoved&&hand.getCompound(i).getString("Instance").equals(selected)))renderHandCard(g,i,false);
        if(front>=0&&!dragMoved)renderHandCard(g,front,true);
        g.disableScissor();
        if(hoveredHand>=0){var card=LifeCardCatalog.get(life(),LifeCardBattle.player(life()),hand.getCompound(hoveredHand).getString("Card"));
            var notes=cardNotes(card);String reason=LifeCardBattle.illegal(life(),hand.getCompound(hoveredHand).getString("Instance"),defaultTarget(card));
            if(!reason.isBlank()&&!reason.equals("busy"))notes.add(text("illegal."+reason));
            if(!notes.isEmpty())tooltip(g,notes,Math.min(254,canvasWidth()-228),106,220);
        }else if(!dragMoved)renderActorTooltip(g,mx,my);
        if(dragMoved)renderDraggedCard(g);
        if(LifeSimulationBattle.pending(battle()))renderDice(g);
        else if(dragMoved){
            fittedLine(g,dragHint(),178,180,232,dragTarget.isBlank()?0xFF97463E:0xFF315D4E);
        }
        else if(!selected.isBlank()){
            String reason=LifeCardBattle.illegal(life(),selected,target);
            fittedLine(g,reason.isBlank()?text("confirm",actorName(LifeSimulationBattle.find(battle(),target))):text("illegal."+reason),178,180,232,reason.isBlank()?0xFF315D4E:0xFF97463E);
        }
        else if(battle().getBoolean("ObjectiveEnabled"))fittedLine(g,text("objective_progress",battle().getInt("Objective"),battle().getInt("Goal"),battle().getInt("Hazard"),battle().getInt("HazardLimit"),Component.translatable(battle().getString("PhaseKey"))),62,29,510,0xFF315D4E);
    }
    private void renderPreparation(GuiGraphics g){
        CompoundTag p=LifeCardBattle.player(life());
        g.drawString(font,text("prepare"),10,5,0xFF30483B,false);
        String stakes=battle().getString("Stakes");
        fittedLine(g,text("risk",p.getInt("KnockoutStreak"),text("stakes."+(List.of("lethal","nonlethal","retreat","injury","capture").contains(stakes)?stakes:"default"))),10,20,572,0xFF8B4739);
        if(picker.isBlank()){
            g.drawString(font,text("regular",battle().getList("RegularDeck",Tag.TAG_STRING).size()),12,204,0xFF30483B,false);
            fittedLine(g,text("consumables"),12,221,568,0xFF30483B);
            var current=companion();g.drawString(font,text("companion",current.isEmpty()?text("none"):actorName(current)),12,243,0xFF30483B,false);
        }
        if(p.getInt("KnockoutStreak")==2||companion().getInt("KnockoutStreak")==2)fittedLine(g,text("third_warning"),12,300,568,0xFFB53838);
    }
    private void renderPreparationPicker(GuiGraphics g){
        SystemGuiTextures.drawInsetPanel(g,8,78,282,193);SystemGuiTextures.drawInsetPanel(g,309,78,277,193);
        fittedLine(g,text(picker.equals("deck")?"current_deck":supportMode.equals("slot")?"current_items":"current_companion"),12,66,272,0xFF30483B);
        fittedLine(g,text("alternatives"),314,66,266,0xFF30483B);
        if(picker.equals("deck")||supportMode.equals("slot")){
            int x=12+(slot%3)*89,y=82+(slot/3)*38,w=83;
            g.fill(x-2,y-2,x+w+2,y+36,0xFF3276AC);
        }
        if(picker.equals("deck")||supportMode.equals("slot"))fittedLine(g,text("select_slot_hint"),12,280,272,0xFF30483B);
        if(choices().isEmpty())fittedLine(g,text("no_alternatives"),318,95,254,0xFF666666);
    }
    private ListTag shownActors(){
        if(!LifeSimulationBattle.pending(battle()))return battle().getList("Actors",Tag.TAG_COMPOUND);
        int reveal=battle().getInt("Reveal");return reveal==0?battle().getList("RoundStartActors",Tag.TAG_COMPOUND):battle().getList("Frames",Tag.TAG_COMPOUND).getCompound(reveal-1).getList("Actors",Tag.TAG_COMPOUND);
    }
    private List<CompoundTag> combatants(){return shownActors().stream().map(t->(CompoundTag)t).filter(a->!a.getString("Kind").equals("anchor")&&!a.getBoolean("Hidden")&&!a.getBoolean("Reserve")&&(a.getString("Id").equals("player")||a.getDouble("Hp")>0&&!a.getBoolean("KnockedOut"))).toList();}
    private void renderArena(GuiGraphics g){
        int w=canvasWidth(),bottom=185;
        int color=switch(battle().getString("Arena")){case "cave","mine"->0xFFDCDDD9;case "inner"->0xFFE8E1EC;case "tribulation"->0xFFDDDDEA;case "ferry"->0xFFD9E5E8;default->0xFFE2EBDD;};
        SystemGuiTextures.drawInsetPanel(g,2,0,w-4,canvasHeight(),color);
        g.fill(4,bottom-8,w-4,canvasHeight()-2,0xFFC0CBC0);
        for(int i=4;i<w-4;i+=28)g.fill(i,Math.max(20,bottom-13-i%19),Math.min(w-4,i+28),bottom-2,0xFFC0CBC0);
        fittedLine(g,text("turn",battle().getInt("Round"),battle().getInt("Energy")),62,9,265,0xFF263C31);
        int allies=0,enemies=0;
        scissorCanvas(g,0,25,w,canvasHeight());
        long now=System.currentTimeMillis();
        if(LifeSimulationBattle.pending(battle())&&actionProgress()>=.7F&&!smokeFrame.equals(animationKey)) {
            smokeFrame=animationKey;
            var impact=battle().getList("Frames",Tag.TAG_COMPOUND).getCompound(battle().getInt("Reveal"));
            for(var actor:combatants())if(LifeBattleSmoke.heavy(impact,actor))
                smokeImpacts.add(new SmokeImpact(actorX(actor.getString("Id")),now,animationKey.hashCode()));
        }
        smokeImpacts.removeIf(impact->now-impact.started()>LifeBattleSmoke.DURATION_MILLIS);
        for(var impact:smokeImpacts)LifeBattleSmoke.draw(g,impact.x(),bottom-6,.38F+.62F*(now-impact.started())/LifeBattleSmoke.DURATION_MILLIS,impact.seed());
        for(var actor:combatants()){
            boolean enemy=actor.getBoolean("Enemy");int index=enemy?enemies++:allies++;
            int x=actorX(actor.getString("Id"));
            float progress=actionProgress();
            float lunge=0;String action="";boolean hurt=false;if(LifeSimulationBattle.pending(battle())){
                var current=battle().getList("Frames",Tag.TAG_COMPOUND).getCompound(battle().getInt("Reveal"));
                if(current.getString("Actor").equals(actor.getString("Id"))){action=current.getString("Effect");lunge=(float)Math.sin(progress*Math.PI)*(List.of("dodge","run").contains(action)?-20:9);}
                if(current.getString("Target").equals(actor.getString("Id"))&&!current.getList("Hits",Tag.TAG_COMPOUND).isEmpty()&&progress>0){
                    int hits=current.getList("Hits",Tag.TAG_COMPOUND).size();hurt=Math.sin(progress*Math.PI*hits*2)>0;
                }
            }
            var sprite=LifeSimulationSprites.actor(life(),actor);int size=bottom==185?90:74,pixels=sprite.pixels();
            int shift=(int)lunge*(enemy?-1:1)+(hurt?(int)(Math.sin(progress*36)*3):0);
            int lift=List.of("dodge","run").contains(action)?(int)(Math.sin(progress*Math.PI)*7):0;
            g.pose().pushPose();g.pose().translate(x-size/2+shift,bottom-3-size-lift,0);
            boolean mirrored=sprite.facesRight()==enemy;
            g.blit(sprite.texture(),0,0,size,size,mirrored?pixels:0,0,mirrored?-pixels:pixels,pixels,pixels,pixels);
            g.pose().popPose();
            if(actor.getDouble("Shield")>0){g.fill(x-size/5-1,bottom-size*4/5,x-size/5+1,bottom-4,0xFF7EC5E7);g.fill(x+size/5-1,bottom-size*4/5,x+size/5+1,bottom-4,0xFF7EC5E7);}
            if(actor.getString("Id").equals(dragMoved?dragTarget:target))g.fill(x-9,bottom-4,x+9,bottom-1,0xFFFFD68A);
        }
        allies=0;enemies=0;
        for(var actor:combatants()){boolean enemy=actor.getBoolean("Enemy");int index=enemy?enemies++:allies++;int width=sideCount(enemy)==1?170:118;stats(g,actor,Mth.clamp(actorX(actor.getString("Id"))-width/2,7,w-width-7),190,width);}
        g.disableScissor();
        renderAction(g,bottom);
        int offset=7;
        for(var tag:battle().getList("Actors",Tag.TAG_COMPOUND)){
            CompoundTag a=(CompoundTag)tag;if(!a.getString("Kind").equals("anchor")||a.getBoolean("Hidden"))continue;
            String title=actorName(a).getString();int width=Math.min(102,font.width(title)+12);
            g.fill(offset,43,offset+width,60,a.getBoolean("Resolved")?0xFF849487:a.getString("Id").equals(target)?0xFFDEC386:0xFFADAAA0);
            g.drawString(font,font.plainSubstrByWidth(title,width-8),offset+4,47,0xFF25372C,false);offset+=width+4;
        }
    }
    private long sideCount(boolean enemy){return combatants().stream().filter(a->a.getBoolean("Enemy")==enemy).count();}
    private int actorX(String id){int allies=0,enemies=0;for(var actor:combatants()){boolean enemy=actor.getBoolean("Enemy");int index=enemy?enemies++:allies++;if(actor.getString("Id").equals(id))return enemy?(sideCount(true)==1?529:393+index*136):57+index*135;}return canvasWidth()/2;}
    private void renderAction(GuiGraphics g,int bottom){
        if(!LifeSimulationBattle.pending(battle()))return;var detail=battle().getList("Frames",Tag.TAG_COMPOUND).getCompound(battle().getInt("Reveal"));
        float progress=actionProgress();if(progress<=0||progress>=1)return;
        int from=actorX(detail.getString("Actor")),to=actorX(detail.getString("Target")),y=bottom-54;
        int color=detail.getString("Action").contains("fire")||detail.getString("Action").contains("yan")?0xFFEBA55D:0xFFAFDDD6;
        if(detail.getString("Effect").equals("attack")){
            int hits=Math.max(1,detail.getList("Hits",Tag.TAG_COMPOUND).size());float phase=progress*hits%1;int x=(int)Mth.lerp(phase,from,to);
            for(int i=0;i<4;i++){int tail=x-(Integer.signum(to-from)*i*5);g.fill(tail-2,y-2,tail+3,y+3,color);}
            if(phase>.7){g.fill(to-10,y-1,to+10,y+1,color);g.fill(to-1,y-10,to+1,y+10,color);}
        }else if(List.of("heal","qi","heal_over_time","qi_over_time","buff","gather","burn").contains(detail.getString("Effect"))){
            for(int i=0;i<10;i++){double angle=progress*Math.PI*2+i*Math.PI/5;int x=to+(int)(Math.cos(angle)*20),z=y+(int)(Math.sin(angle)*9);g.fill(x-1,z-1,x+2,z+2,color);}
        }
    }
    private void stats(GuiGraphics g,CompoundTag a,int x,int y,int width){
        fittedLine(g,actorName(a),x,y,width,0xFF263C31);
        bar(g,x,y+12,width,a.getDouble("Hp"),a.getDouble("MaxHp"),a.getBoolean("Enemy")?0xFF60BD68:0xFFC96960);
        fittedLine(g,Component.literal(number(a.getDouble("Hp"))+"/"+number(a.getDouble("MaxHp"))),x+3,y+12,width-6,0xFF16241C);
        bar(g,x,y+24,width,a.getDouble("Qi"),a.getDouble("MaxQi"),0xFF59A2D0);
        fittedLine(g,text("qi",number(a.getDouble("Qi")),number(a.getDouble("MaxQi"))),x+3,y+24,width-6,0xFF173C52);
        var status=statuses(a);
        if(!a.getBoolean("Enemy")&&a.getInt("KnockoutStreak")>0)fittedLine(g,text("knockouts",a.getInt("KnockoutStreak")),x,y+36,width,0xFF963C32);
        else if(!status.isEmpty())fittedLine(g,status.getFirst(),x,y+36,width,0xFF775E2F);
        // Intents belong over the opponent, leaving health and Qi directly below the sprite.
        if(a.getBoolean("Enemy")){
            CompoundTag intent=LifeSimulationBattle.find(battle(),a.getString("Id")).getCompound("Intent");
            Component title=intent.isEmpty()?text("unknown"):Component.translatable(intent.getString("Name"));
            fittedLine(g,title,x,62,width,0xFF754632);
            ListTag hits=intent.getList("Hits",Tag.TAG_DOUBLE);String damage="";
            for(int i=0;i<hits.size();i++)damage+=(i==0?"":" / ")+number(hits.getDouble(i)*.95)+"–"+number(hits.getDouble(i)*1.05);
            fittedLine(g,Component.literal(damage),x,74,width,0xFF414A40);
        }
    }
    private void bar(GuiGraphics g,int x,int y,int w,double amount,double max,int color){g.fill(x,y,x+w,y+10,0xFF26392F);g.fill(x,y,x+(int)(w*Mth.clamp(amount/Math.max(1,max),0,1)),y+10,color);}
    private record HandLayout(float center,float top,float angle,int width,int height){
        boolean contains(double x,double y){
            double radians=Math.toRadians(angle),dx=x-center,dy=y-top-height;
            double localX=dx*Math.cos(radians)+dy*Math.sin(radians),localY=-dx*Math.sin(radians)+dy*Math.cos(radians);
            return localX>=-width/2.&&localX<width/2.&&localY>=-height&&localY<0;
        }
    }
    private HandLayout handLayout(int index){
        int count=battle().getList("Hand",Tag.TAG_COMPOUND).size();float offset=index-(count-1)/2F;
        float spacing=Math.min(76,360F/Math.max(1,count-1));
        boolean raised=index==hoveredHand||battle().getList("Hand",Tag.TAG_COMPOUND).getCompound(index).getString("Instance").equals(selected);
        return new HandLayout(246+offset*spacing,229+Math.abs(offset)*7-(raised?24:0),raised?0:offset*5,94,110);
    }
    private int handAt(double x,double y){
        var hand=battle().getList("Hand",Tag.TAG_COMPOUND);
        if(hoveredHand>=0&&hoveredHand<hand.size()&&handLayout(hoveredHand).contains(x,y))return hoveredHand;
        for(int i=0;i<hand.size();i++)if(hand.getCompound(i).getString("Instance").equals(selected)&&handLayout(i).contains(x,y))return i;
        for(int i=hand.size()-1;i>=0;i--)if(handLayout(i).contains(x,y))return i;
        return -1;
    }
    private void renderHandCard(GuiGraphics g,int index,boolean front){
        var entry=battle().getList("Hand",Tag.TAG_COMPOUND).getCompound(index);
        var card=LifeCardCatalog.get(life(),LifeCardBattle.player(life()),entry.getString("Card"));if(card==null)return;
        var layout=handLayout(index);g.pose().pushPose();g.pose().translate(layout.center(),layout.top()+layout.height(),front?30:10+index);
        g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(layout.angle()));
        drawCard(g,card,-layout.width()/2,-layout.height(),layout.width(),layout.height(),entry.getString("Instance").equals(selected)||front,false);
        g.flush();g.pose().popPose();
    }
    private int cardAccent(LifeCardCatalog.Card card){
        if(card.consumable())return 0xFF81603D;
        return switch(card.effect()){
            case "attack"->0xFF8B4540;
            case "block","dodge"->0xFF3D687A;
            case "qi","gather","heal"->0xFF42775C;
            default->0xFF6E5686;
        };
    }
    private void cardFrame(GuiGraphics g,int x,int y,int w,int h,int accent,boolean selected){
        g.fill(x+3,y+4,x+w+3,y+h+4,0x33000000);
        g.fill(x+2,y,x+w-2,y+h,0xFF393E36);g.fill(x,y+2,x+w,y+h-2,0xFF393E36);
        g.fill(x+2,y+2,x+w-2,y+h-2,selected?0xFFF3CE7B:0xFFAD9564);
        g.fill(x+4,y+4,x+w-4,y+h-4,0xFFF2E9D2);
        g.fill(x+5,y+5,x+w-5,y+18,accent);
        g.fill(x+5,y+h-20,x+w-5,y+h-5,0xFFE1D5B9);
        for(int side=0;side<2;side++){
            int cx=side==0?x+4:x+w-5;
            g.fill(cx,y+3,cx+1,y+10,0xFFFFE8B3);g.fill(cx,y+h-10,cx+1,y+h-3,0xFFFFE8B3);
        }
    }
    private void drawCard(GuiGraphics g,LifeCardCatalog.Card card,int x,int y,int w,int h,boolean selected,boolean large){
        int accent=cardAccent(card);cardFrame(g,x,y,w,h,accent,selected);
        // Separate cost badge, title, illustration, effect and footer prevent text collisions.
        g.fill(x+7,y+3,x+23,y+20,0xFF3B4037);g.renderOutline(x+7,y+3,16,17,0xFFE5C17B);
        Component energy=Component.literal(Integer.toString(card.energy()));
        g.drawString(font,energy,x+15-font.width(energy)/2,y+7,0xFFFFF3CD,false);
        int[] pool=LifeCardDice.pool(life(),LifeCardBattle.player(life()).getInt("Realm"));
        fittedLine(g,Component.literal(pool[0]+"d"+pool[1]),x+w-34,y+8,27,0xFFF8EAD3);
        var lines=font.split(name(card),w-14);
        for(int i=0;i<Math.min(2,lines.size());i++)g.drawString(font,lines.get(i),x+(w-font.width(lines.get(i)))/2,y+24+i*10,0xFF2D4136,false);
        int artY=y+46;
        g.fill(x+8,artY-3,x+w-8,artY+25,0xFFE5DABF);
        g.fill(x+8,artY-3,x+w-8,artY-2,0xFFC6B58D);
        g.pose().pushPose();g.pose().translate(x+w/2F-12,artY,1);g.pose().scale(1.2F,1.2F,1);icon(g,card,0,0);g.flush();g.pose().popPose();
        fittedLine(g,cardValue(card),x+8,y+76,w-16,accent);
        if(large&&h>=130&&card.duration()>0)fittedLine(g,text("duration",card.duration()),x+8,y+89,w-16,0xFF634D34);
        fittedLine(g,text("target.short."+card.target()),x+8,y+h-22,w-16,0xFF635840);
        fittedLine(g,text("card_qi",number(card.qi())),x+8,y+h-12,w-16,0xFF326275);
        if(!large){String instance="";for(var tag:battle().getList("Hand",Tag.TAG_COMPOUND)){CompoundTag entry=(CompoundTag)tag;if(entry.getString("Card").equals(card.id())){instance=entry.getString("Instance");break;}}
            String reason=LifeCardBattle.illegal(life(),instance,defaultTarget(card));if(!reason.isBlank()&&!reason.equals("busy"))g.fill(x+4,y+4,x+w-4,y+h-4,0x44554543);}
    }
    private String defaultTarget(LifeCardCatalog.Card card){
        if(card.target().equals("self")||card.target().equals("friend"))return "player";
        for(var tag:battle().getList("Actors",Tag.TAG_COMPOUND)){CompoundTag a=(CompoundTag)tag;
            if(card.target().equals("enemy")&&a.getBoolean("Enemy")&&LifeCardBattle.active(a))return a.getString("Id");
            if(card.target().equals("anchor")&&a.getString("Kind").equals("anchor")&&LifeSimulationBattle.objectiveIllegal(life(),a.getString("Id")).isBlank())return a.getString("Id");}
        return card.target().equals("any")?"player":target;
    }
    private CompoundTag previewTarget(LifeCardCatalog.Card card){
        if(card.target().equals("self"))return LifeCardBattle.player(life());
        String id=dragMoved&&!dragTarget.isBlank()?dragTarget:target;
        var candidate=LifeSimulationBattle.find(battle(),id);
        if(LifeCardBattle.active(candidate)&&!candidate.getBoolean("Hidden")&&(!card.target().equals("enemy")||!candidate.getBoolean("Protected"))
                &&(card.target().equals("enemy")?candidate.getBoolean("Enemy"):card.target().equals("friend")?!candidate.getBoolean("Enemy"):true))return candidate;
        return LifeSimulationBattle.find(battle(),defaultTarget(card));
    }
    private String range(double minimum,double maximum){
        return Math.abs(maximum-minimum)<.000001?number(minimum):number(Math.floor(minimum*100+1e-9)/100)+"–"+number(Math.ceil(maximum*100-1e-9)/100);
    }
    private Component cardValue(LifeCardCatalog.Card card){
        if(card.effect().equals("attack"))return text("value.attack",number(card.baseTotal()));
        if(List.of("dodge","run").contains(card.effect()))return text("value."+card.effect(),LifeCardDice.requirement(life(),LifeCardBattle.player(life()),card.amount()));
        var value=LifeCardBattle.preview(LifeCardBattle.player(life()),previewTarget(card),card);
        return text("value."+card.effect(),range(value.minimum(),value.maximum()));
    }
    private List<Component> cardNotes(LifeCardCatalog.Card card){
        List<Component> notes=new ArrayList<>();
        var victim=previewTarget(card);var preview=LifeCardBattle.preview(LifeCardBattle.player(life()),victim,card);
        if(List.of("attack","heal","qi","heal_over_time","qi_over_time","shield","clock_morning").contains(card.effect())){
            notes.add(cardValue(card));
            if(card.effect().equals("attack")){
                notes.add(text("preview_before_defense",range(preview.beforeDefense()*.95,preview.beforeDefense()*1.05)));
                notes.add(text("preview_hp_loss",range(preview.minimum(),preview.maximum())));
            }else notes.add(text("preview_base",number(preview.base())));
        }
        if(card.effect().equals("attack"))notes.add(text("preview_damage_note"));
        if(preview.turns()>0){
            String kind=card.effect().equals("heal_over_time")?"hp":"qi";
            notes.add(text("preview_tick."+kind,range(preview.minimum()/preview.turns(),preview.maximum()/preview.turns()),preview.turns()));
            notes.add(text("preview_now."+kind,range(preview.initialMinimum(),preview.initialMaximum())));
            notes.add(text("preview_timed_note"));
        }
        if(card.hits().length>1)notes.add(text("multi_hit",card.hits().length));
        if(card.id().equals("spell_jinfeng_zhi"))notes.add(text("jinfeng"));
        if(card.id().equals("spell_wanren_chuanxin"))notes.add(text("wanren"));
        if(card.effect().equals("buff"))notes.add(text("buff."+card.status()));
        if(card.effect().equals("gather"))notes.add(text("gather_note"));
        return notes;
    }
    private List<Component> statuses(CompoundTag actor){
        List<Component> notes=new ArrayList<>();int round=battle().getInt("Round");
        for(String id:List.of("Burn","Slow","Pressure","Obscured"))if(actor.getInt(id+"Until")>round)notes.add(text("status."+id,actor.getInt(id+"Until")-round));
        if(actor.getDouble("GatherQi")>0)notes.add(text("status.Gather",number(actor.getDouble("GatherQi"))));
        if(actor.getDouble("Shield")>0)notes.add(text("status.Shield",number(actor.getDouble("Shield"))));
        for(String id:actor.getCompound("CardBuffs").getAllKeys())if(LifeCardBattle.buff(actor,id)>0)notes.add(text("buff."+id).copy().append(" "+number(LifeCardBattle.buff(actor,id))+"%"));
        for(var tag:actor.getList("CardEffects",Tag.TAG_COMPOUND)){CompoundTag e=(CompoundTag)tag;notes.add(text("status."+e.getString("Kind"),number(e.getDouble("Amount")),e.getInt("Remaining")));}
        return notes;
    }
    private void renderActorTooltip(GuiGraphics g,int mx,int my){
        for(var a:combatants()){int width=sideCount(a.getBoolean("Enemy"))==1?170:118,x=Mth.clamp(actorX(a.getString("Id"))-width/2,7,canvasWidth()-width-7);
            if(mx<x||mx>=x+width||my<58||my>228)continue;
            List<Component> lines=new ArrayList<>();lines.add(actorName(a));lines.add(realm(a.getInt("Realm")));lines.add(text("knockouts",a.getInt("KnockoutStreak")));lines.addAll(statuses(a));
            for(var tag:a.getList("DefenseQueue",Tag.TAG_COMPOUND)){var defense=(CompoundTag)tag;lines.add(defense.getString("Kind").equals("block")?text("queue.block").copy().append(number(defense.getDouble("Strength")*100)+"%"):text(defense.getBoolean("Success")?"queue.dodge_success":"queue.dodge_failed"));}
            if(a.getBoolean("Enemy")){CompoundTag intent=LifeSimulationBattle.find(battle(),a.getString("Id")).getCompound("Intent");lines.add(text("intent",Component.translatable(intent.getString("Name"))));
                if(intent.getBoolean("Area"))lines.add(text("area_intent"));
                ListTag hits=intent.getList("Hits",Tag.TAG_DOUBLE);for(int i=0;i<hits.size();i++)lines.add(text("intent_hit",i+1,number(hits.getDouble(i)*.95),number(hits.getDouble(i)*1.05)));
                if(hits.isEmpty())lines.add(text("value."+intent.getString("Effect"),number(intent.getDouble("Amount"))));}
            tooltip(g,lines,Math.min(x,canvasWidth()-228),101,220);return;}
    }
    private void tooltip(GuiGraphics g,List<Component> lines,int x,int y,int width){
        var wrapped=new ArrayList<net.minecraft.util.FormattedCharSequence>();for(var line:lines)wrapped.addAll(font.split(line,width-12));
        int height=Math.min(180,wrapped.size()*10+10);g.pose().pushPose();g.pose().translate(0,0,400);g.fill(x,y,x+width,y+height,0xF21E3333);
        for(int i=0;i<Math.min(17,wrapped.size());i++)g.drawString(font,wrapped.get(i),x+6,y+5+i*10,0xFFF0E6CC,false);g.pose().popPose();
    }
    private void icon(GuiGraphics g,LifeCardCatalog.Card card,int x,int y){
        if(card.id().startsWith("spell_")){var spell=ModSpells.get(card.id().substring(6));if(spell!=null){TechniqueIcons.blitCentered(g,TechniqueIcons.spell(spell),x,y,22,22);return;}}
        String item=card.consumable()?card.item():switch(card.effect()){case "block"->"minecraft:shield";case "dodge"->"minecraft:feather";case "run"->"minecraft:leather_boots";case "qi"->"immortal_cultivation_mod:spirit_stone";case "inspect","objective"->"minecraft:ender_eye";default->"minecraft:iron_sword";};
        ResourceLocation id=ResourceLocation.tryParse(LifeCardCatalog.itemTextureId(item));if(id!=null){g.pose().pushPose();g.pose().translate(x,y,0);g.pose().scale(1.25F,1.25F,1);g.renderItem(new ItemStack(BuiltInRegistries.ITEM.get(id)),0,0);g.pose().popPose();}
    }
    private List<LifeDiceSequence.Pool> currentRolls(){
        var frame=battle().getList("Frames",Tag.TAG_COMPOUND).getCompound(battle().getInt("Reveal"));
        if(frame!=diceFrame){diceFrame=frame;dicePools=LifeDiceSequence.pools(frame);}return dicePools;
    }
    private int animationModeValue(){return ModClientConfig.LIFE_CARD_ANIMATION.get();}
    private long rollDuration(){return LifeDiceSequence.rollMillis(animationModeValue());}
    private long actionDelay(){return LifeDiceSequence.sequenceMillis(currentRolls(),animationModeValue());}
    private long actionDuration(){return switch(animationModeValue()){case 0->1800;case 1->700;default->70;};}
    private long frameDuration(){return actionDelay()+actionDuration()+50;}
    private float actionProgress(){return Mth.clamp((System.currentTimeMillis()-animationStart-actionDelay())/(float)actionDuration(),0,1);}
    private void renderDice(GuiGraphics g){
        CompoundTag detail=battle().getList("Frames",Tag.TAG_COMPOUND).getCompound(battle().getInt("Reveal"));
        var pools=currentRolls();if(pools.isEmpty())return;
        long elapsed=System.currentTimeMillis()-animationStart;
        int poolIndex=LifeDiceSequence.index(pools,elapsed,animationModeValue());
        var pool=pools.get(poolIndex);var roll=pool.roll();
        long poolElapsed=elapsed-poolIndex*LifeDiceSequence.poolMillis(animationModeValue());
        Component label=roll.contains("Requirement")?text("binary_roll",roll.getInt("Total"),roll.getInt("Requirement"),text(roll.getBoolean("Success")?"success":"failure"))
                :roll.contains("DC")?text("roll_check",roll.getInt("Total"),roll.getInt("DC")):pool.kind().equals("main")&&detail.getBoolean("Quantitative")?text("roll",roll.getInt("Total"),String.format(java.util.Locale.ROOT,"%+.1f",roll.getDouble("Variation")*100)):text("roll_discrete",roll.getInt("Total"));
        if(elapsed>=actionDelay()) {
            // Report actual capped HP/Qi change, rather than raw pre-defense damage or overhealing.
            if(detail.contains("AppliedAmount")){
                String effect=detail.getString("Effect");
                String kind=effect.equals("attack")?"damage":effect.startsWith("heal")?"heal":effect.startsWith("qi")?"qi":"shield";
                label=text("applied."+kind,number(detail.getDouble("AppliedAmount")));
            }
            fittedLine(g,label,186,165,220,0xFF315D4E);return;
        }
        LifeDiceRenderer.drawPool(g,roll,canvasWidth()/2F,134,35,poolElapsed,rollDuration(),350,0xFF263C31);
        if(pools.size()>1)fittedLine(g,Component.translatable("life.dice.pool",poolIndex+1,pools.size(),Component.translatable("life.dice.kind."+pool.kind())),175,32,242,0xFF263C31);
        if(pool.kind().equals("main")&&detail.getBoolean("Quantitative")){
            boolean block=detail.getString("Effect").equals("block"),percent=block||detail.getString("Effect").equals("buff");
            double scale=block?100:1;
            label=label.copy().append(" · ").append(text(percent?"rolled_effect_percent":"rolled_effect",number(detail.getDouble("Base")*scale),number(detail.getDouble("Amount")*scale)));
        }
        fittedLine(g,label,186,165,220,0xFF263C31);
    }
    private void fittedLine(GuiGraphics g,Component label,int x,int y,int width,int color){
        float scale=Math.min(1,width/(float)Math.max(1,font.width(label)));
        g.pose().pushPose();g.pose().translate(x,y,0);g.pose().scale(scale,scale,1);g.drawString(font,label,0,0,color,false);g.pose().popPose();
    }
    @Override protected boolean clickContent(double x,double y,int button){
        if(button!=0||!picker.isBlank()||battle().getString("CardPhase").equals("prepare")||LifeSimulationBattle.pending(battle()))return false;
        int index=handAt(x,y);
        if(index>=0){var entry=battle().getList("Hand",Tag.TAG_COMPOUND).getCompound(index);selected=entry.getString("Instance");dragging=true;dragMoved=false;dragTarget="";dragStartX=dragX=x;dragStartY=dragY=y;
            var card=LifeCardCatalog.get(life(),LifeCardBattle.player(life()),entry.getString("Card"));dragging=card!=null;if(card!=null)target=defaultTarget(card);rebuildWidgets();return true;}
        return selectTarget(x,y);
    }
    private String targetAt(double x,double y){
        for(var a:combatants()){
            int width=sideCount(a.getBoolean("Enemy"))==1?170:118,left=Mth.clamp(actorX(a.getString("Id"))-width/2,7,canvasWidth()-width-7);
            if(x>=left&&x<left+width&&y>=58&&y<228)return a.getString("Id");
        }
        int offset=7;
        for(var tag:battle().getList("Actors",Tag.TAG_COMPOUND)){var a=(CompoundTag)tag;if(!a.getString("Kind").equals("anchor")||a.getBoolean("Hidden"))continue;
            int width=Math.min(102,font.width(actorName(a).getString())+12);if(x>=offset&&x<offset+width&&y>=43&&y<60)return a.getString("Id");offset+=width+4;}
        return "";
    }
    private boolean selectTarget(double x,double y){String found=targetAt(x,y);if(found.isBlank())return false;target=found;rebuildWidgets();return true;}
    @Override protected boolean dragContent(double x,double y,int button){
        if(!dragging||button!=0)return false;
        dragX=x;dragY=y;if(Math.hypot(x-dragStartX,y-dragStartY)>=5)dragMoved=true;
        if(dragMoved&&playButton!=null)playButton.active=false;
        String found=targetAt(x,y);dragTarget=!found.isBlank()&&LifeCardBattle.illegal(life(),selected,found).isBlank()?found:"";
        return true;
    }
    @Override protected void releaseContent(double x,double y,int button){
        if(!dragging||button!=0)return;
        boolean moved=dragMoved;dragging=false;dragMoved=false;dragTarget="";
        var entry=LifeCardBattle.handCard(battle(),selected);var card=entry.isEmpty()?null:LifeCardCatalog.get(life(),LifeCardBattle.player(life()),entry.getString("Card"));
        String found=targetAt(x,y);
        if(moved&&card!=null&&!found.isBlank()&&LifeCardBattle.illegal(life(),selected,found).isBlank()){
            target=found;send("play:"+selected+":"+target);
        }else rebuildWidgets();
    }
    private Component dragHint(){
        var entry=LifeCardBattle.handCard(battle(),selected);
        var card=entry.isEmpty()?null:LifeCardCatalog.get(life(),LifeCardBattle.player(life()),entry.getString("Card"));
        if(dragTarget.isBlank())return text(card!=null&&card.target().equals("self")?"drag_self":"drop_invalid");
        if(card!=null&&card.target().equals("self"))return text("drop_self");
        return text(card!=null&&card.effect().equals("attack")?"drop_attack":"drop_play",actorName(LifeSimulationBattle.find(battle(),dragTarget)));
    }
    private void renderDraggedCard(GuiGraphics g){
        var entry=LifeCardBattle.handCard(battle(),selected);if(entry.isEmpty())return;
        var card=LifeCardCatalog.get(life(),LifeCardBattle.player(life()),entry.getString("Card"));if(card==null)return;
        g.pose().pushPose();g.pose().translate(0,0,80);
        drawCard(g,card,(int)dragX-47,(int)dragY-72,94,110,true,false);
        g.fill((int)dragX-4,(int)dragY-4,(int)dragX+5,(int)dragY+5,dragTarget.isBlank()?0xFFB95B4B:0xFF65B784);
        g.pose().popPose();
    }
    private Component name(LifeCardCatalog.Card card){return Component.translatable(card.name());}
    private Component actorName(CompoundTag actor){String name=actor.getString("NameKey").isBlank()?actor.getString("Name"):actor.getString("NameKey");return name.startsWith("life.")?Component.translatable(name):Component.literal(name);}
    private Component itemName(String id){var key=ResourceLocation.tryParse(LifeCardCatalog.itemTextureId(id));return key!=null?BuiltInRegistries.ITEM.get(key).getName(new ItemStack(BuiltInRegistries.ITEM.get(key))):text("unknown");}
    private Component realm(int value){return com.example.immortal_cultivation_mod.attachment.CultivationLevels.displayName(com.example.immortal_cultivation_mod.attachment.CultivationLevels.allLevels().get(Mth.clamp(value,0,com.example.immortal_cultivation_mod.attachment.CultivationLevels.allLevels().size()-1)));}
    private String number(double value){return com.example.immortal_cultivation_mod.alchemy.PillData.number(value);}
}
