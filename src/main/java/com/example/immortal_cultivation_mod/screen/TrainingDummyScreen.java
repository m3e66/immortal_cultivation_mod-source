package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.attachment.*;
import com.example.immortal_cultivation_mod.entity.NpcSpellcasting;
import com.example.immortal_cultivation_mod.network.TrainingDummyPayloads;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** Registry-backed controller with separate controls, exact knowledge selection and editable stats. */
public final class TrainingDummyScreen extends FittedMenuScreen {
    private static final int W=620,H=400,ROWS=8;
    private final UUID dummyId;
    private CompoundTag data;
    private int tab,page,ticks,realmIndex;
    private String query="",selected="",notice="";
    private EditBox search,mastery,name;
    private final List<EditBox> stats=new ArrayList<>();
    private final List<Runnable> labels=new ArrayList<>();
    private List<String> catalog=List.of();
    private TrainingDummyScreen(UUID id,CompoundTag state){super(text("title"),W+12,H+12);dummyId=id;data=state.copy();}
    public static void accept(TrainingDummyPayloads.State packet) {
        var mc=Minecraft.getInstance();
        if(packet.open())mc.setScreen(new TrainingDummyScreen(packet.dummyId(),packet.data()));
        else if(mc.screen instanceof TrainingDummyScreen screen&&screen.dummyId.equals(packet.dummyId())) {
            screen.data=packet.data().copy();if(screen.data.contains("Notice"))screen.notice=screen.data.getString("Notice");
        }
    }
    private static Component text(String key){return Component.translatable("training_dummy.immortal_cultivation_mod."+key);}
    private static Component yes(boolean value){return Component.translatable(value?"options.on":"options.off");}
    private String dummyName(){return data.getBoolean("Named")?data.getString("Name"):text("title").getString();}
    private int x(){return (width-W)/2;}
    private int y(){return (height-H)/2;}
    private void send(String action,String key,String value){notice="";PacketDistributor.sendToServer(new TrainingDummyPayloads.Edit(dummyId,action,key,value));}
    private CultivationGuiButton button(int x,int y,int width,Component title,Runnable action){var b=new CultivationGuiButton(x,y,width,22,title,action);addRenderableWidget(b);return b;}
    private EditBox input(int x,int y,int width,String value,int length) {
        var field=new EditBox(font,x,y,width,20,Component.empty());field.setMaxLength(length);field.setValue(value);addRenderableWidget(field);return field;
    }
    private boolean known(String id){return data.getList(tab==1?"Methods":"Spells",Tag.TAG_STRING).stream().anyMatch(t->t.getAsString().equals(id));}
    private Component entryName(String id){var method=CultivationMethods.get(id);return tab==1?Component.translatable(method.nameKey()):ModSpells.get(id).name();}
    @Override protected void initMenu() {
        clearWidgets();labels.clear();stats.clear();int x=x(),y=y();
        for(int i=0;i<4;i++){int index=i;button(x+20+i*148,y+65,142,text(List.of("controls","methods","spells","stats").get(i)),()->{tab=index;page=0;selected="";query="";initMenu();});}
        if(tab==0)controls();else if(tab==3)statistics();else selection();
        button(x+W-90,y+H-32,70,Component.translatable("gui.done"),this::onClose);
    }
    private void controls() {
        int x=x(),y=y();name=input(x+90,y+106,340,dummyName(),48);
        button(x+444,y+105,145,text("rename"),()->send("name","",name.getValue()));
        var keys=List.of("Domain","Combat","Mobile","Immortal","UnlimitedQi");
        var names=List.of("domain","combat","mobile","immortal","unlimited_qi");
        for(int i=0;i<keys.size();i++) {
            String key=keys.get(i),label=names.get(i);var b=button(x+30+(i%2)*285,y+146+(i/2)*30,270,Component.empty(),()->send("toggle",key,""));
            if(key.equals("Mobile"))b.setTooltip(net.minecraft.client.gui.components.Tooltip.create(text("flight_hint")));
            labels.add(()->b.setMessage(Component.translatable("training_dummy.immortal_cultivation_mod.toggle",text(label),yes(data.getBoolean(key)))));
        }
        button(x+315,y+206,270,text("reset"),()->send("reset","",""));
        button(x+30,y+244,270,text("target_self"),()->send("target_self","",""));
        button(x+315,y+244,270,text("stop"),()->send("stop","",""));
        button(x+30,y+274,270,text("copy_weapon"),()->send("copy_weapon","",""));
        button(x+315,y+274,270,text("refresh"),()->PacketDistributor.sendToServer(new TrainingDummyPayloads.Request(dummyId)));
        button(x+30,y+304,270,text("clear_debuffs"),()->send("clear_debuffs","","")).setTooltip(
                net.minecraft.client.gui.components.Tooltip.create(text("clear_debuffs_hint")));
    }
    private void selection() {
        int x=x(),y=y();search=input(x+30,y+108,270,query,64);
        button(x+310,y+107,66,text("search"),()->{query=search.getValue();page=0;initMenu();});
        button(x+398,y+107,188,text(tab==1?"unlock_all_methods":"unlock_all_spells"),
                ()->send(tab==1?"unlock_all_methods":"unlock_all_spells","",""));
        catalog=(tab==1?CultivationMethods.allMethods().stream().map(CultivationMethods.MethodDef::id):ModSpells.all().stream().map(ModSpells.SpellDef::id).filter(NpcSpellcasting::isNpcSupportedSpell))
                .sorted().filter(id->query.isBlank()||id.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT))||entryName(id).getString().toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT))).toList();
        page=Math.clamp(page,0,Math.max(0,(catalog.size()-1)/ROWS));
        if(!catalog.contains(selected))selected=catalog.isEmpty()?"":catalog.get(page*ROWS);
        for(int i=0;i<ROWS&&page*ROWS+i<catalog.size();i++) {
            String id=catalog.get(page*ROWS+i);int row=y+139+i*24;
            var checkbox=button(x+30,row,28,Component.empty(),()->send(tab==1?"method_known":"spell_known",id,""));
            labels.add(()->checkbox.setMessage(Component.literal(known(id)?"✓":"+")));
            button(x+65,row,300,Component.literal(font.plainSubstrByWidth(entryName(id).getString(),275)),()->{selected=id;initMenu();});
        }
        button(x+30,y+338,64,Component.literal("‹"),()->{page=Math.max(0,page-1);initMenu();});
        button(x+301,y+338,64,Component.literal("›"),()->{page=Math.min(Math.max(0,(catalog.size()-1)/ROWS),page+1);initMenu();});
        if(selected.isBlank())return;
        if(tab==1) {
            mastery=input(x+398,y+212,100,Integer.toString(data.getCompound("MethodProficiencies").getInt(selected)),5);
            var save=button(x+506,y+211,80,text("apply"),()->send("mastery",selected,mastery.getValue()));
            var activate=button(x+398,y+251,188,text("activate_method"),()->send("method",selected,""));
            labels.add(()->{save.active=activate.active=known(selected);});
        }else{
            var cast=button(x+398,y+212,188,text("cast"),()->send("cast",selected,""));
            labels.add(()->cast.active=known(selected)&&ModSpells.meetsRequirement(data.getString("Realm"),ModSpells.get(selected)));
        }
    }
    private void statistics() {
        int x=x(),y=y();realmIndex=Math.max(0,CultivationLevels.allLevels().indexOf(data.getString("Realm")));
        button(x+30,y+110,45,Component.literal("‹"),()->realmIndex=Math.max(0,realmIndex-1));
        button(x+545,y+110,45,Component.literal("›"),()->realmIndex=Math.min(CultivationLevels.allLevels().size()-1,realmIndex+1));
        String[] keys={"MaxHealth","MaxQi","Attack","Magic","Armor","Speed"};
        for(int i=0;i<6;i++) {
            String value=i==1?Integer.toString(data.getInt(keys[i])):Double.toString(data.getDouble(keys[i]));
            stats.add(input(x+35+(i%2)*285,y+170+(i/2)*50,255,value,24));
        }
        button(x+35,y+316,540,text("apply_stats"),()->send("stats",CultivationLevels.allLevels().get(realmIndex),String.join(",",stats.stream().map(EditBox::getValue).toList())));
    }
    @Override public void tick(){super.tick();if(++ticks%20==0&&tab==0)PacketDistributor.sendToServer(new TrainingDummyPayloads.Request(dummyId));}
    @Override public boolean isPauseScreen(){return false;}
    // Screen.render invokes this after our panel/text; its vanilla blur would blur the editor itself.
    @Override public void renderBackground(GuiGraphics graphics,int mouseX,int mouseY,float partialTick) { }
    @Override protected void renderMenu(GuiGraphics g,int mouseX,int mouseY,float partialTick) {
        int x=x(),y=y();g.fill(0,0,width,height,0xA0101716);CultivationGuiTextures.drawLandscape(g,x,y,W,H);
        g.drawString(font,title,x+W/2-font.width(title)/2,y+18,0xFF38352E,false);
        var status=Component.translatable("training_dummy.immortal_cultivation_mod.vitals",dummyName(),(int)data.getFloat("Health"),(int)data.getFloat("MaxHealth"),data.getInt("Qi"),data.getInt("MaxQi"));
        g.drawString(font,font.plainSubstrByWidth(status.getString(),W-48),x+24,y+52,0xFF565344,false);
        for(var update:labels)update.run();
        if(tab==0) {
            g.drawString(font,text("name"),x+30,y+112,0xFF38352E,false);
            var target=Component.translatable("training_dummy.immortal_cultivation_mod.target",data.getString("Target").isBlank()?text("none"):Component.literal(data.getString("Target")));
            g.drawString(font,font.plainSubstrByWidth(target.getString(),270),x+315,y+311,0xFF38352E,false);
            g.drawWordWrap(font,text("stick_hint"),x+30,y+337,510,0xFF6F685B);
        }else if(tab==3){
            var realm=CultivationLevels.displayName(CultivationLevels.allLevels().get(realmIndex));
            g.drawString(font,realm,x+W/2-font.width(realm)/2,y+117,0xFF38352E,false);
            var names=List.of("max_hp","max_qi","attack","magic","armor","speed");
            for(int i=0;i<6;i++)g.drawString(font,text(names.get(i)),x+35+(i%2)*285,y+157+(i/2)*50,0xFF38352E,false);
        }else{
            g.drawString(font,Component.literal((page+1)+" / "+Math.max(1,(catalog.size()+ROWS-1)/ROWS)),x+180,y+345,0xFF38352E,false);
            if(!selected.isBlank()) {
                g.drawWordWrap(font,entryName(selected),x+398,y+143,188,0xFF38352E);
                g.drawString(font,Component.translatable("training_dummy.immortal_cultivation_mod.known",yes(known(selected))),x+398,y+179,0xFF38352E,false);
                if(tab==1){g.drawString(font,text("proficiency"),x+398,y+197,0xFF38352E,false);
                    var method=CultivationMethods.get(data.getString("Method"));
                    g.drawWordWrap(font,Component.translatable("training_dummy.immortal_cultivation_mod.current_method",method==null?text("none"):Component.translatable(method.nameKey())),x+398,y+286,188,0xFF6F685B);
                }
                else g.drawWordWrap(font,text("cast_hint"),x+398,y+250,188,0xFF6F685B);
            }
        }
        if(!notice.isBlank())g.drawWordWrap(font,text(notice),x+24,y+H-31,480,0xFF9A4034);
        super.renderMenu(g,mouseX,mouseY,partialTick);
    }
}
