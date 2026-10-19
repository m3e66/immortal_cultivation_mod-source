package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.client.OuterSystemClient;
import com.example.immortal_cultivation_mod.network.OuterSystemPayloads;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.system.LifeSimulationWorldMap;
import com.example.immortal_cultivation_mod.system.LifeSimulationBattle;
import com.example.immortal_cultivation_mod.system.LifeSimulationSprites;
import com.example.immortal_cultivation_mod.system.LifeSimulationSystem;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.locale.Language;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;

/** Legacy desktop planner with explicit action selectors and a scrollable journal. */
public final class LifePlanningScreen extends LifeWindowScreen {
    private CompoundTag state;
    private final boolean journal;
    private int scroll, selectedSlot;
    private String selectionMenu = "";
    private boolean waiting, draggingScroll, draggingPeople;
    private int peopleScroll, scrollGrab;
    private JournalView journalView = JournalView.HISTORY;
    private LifeRelationshipFilter relationshipFilter = LifeRelationshipFilter.ALL;
    private static final int PEOPLE_VISIBLE = 5;
    private static final int PEOPLE_TOP = 61;
    private String selectedPerson = "", journalCacheKey = "";
    private Language journalLanguage;
    private List<FormattedCharSequence> journalCache = List.of();
    private long waitingRevision;
    private static final List<String> ACTIONS = LifeSimulationSystem.ACTIVITIES.stream().filter(action -> !action.equals("keep_oath")).toList();
    private static final List<String> CONDITIONS = LifeSimulationSystem.ROUTINE_CONDITIONS;

    public LifePlanningScreen(CompoundTag state, boolean journal) {
        super(Component.translatable(journal ? "life.expansion.journal" : "life.expansion.routine"));
        this.state=state.copy();this.journal=journal;
    }
    public boolean isJournal() { return journal; }
    public void updateState(CompoundTag updated) {
        boolean changed=!life().equals(updated.getCompound("LifeSimulation"));
        boolean acknowledged=waiting && updated.getLong("LifeActionRevision") != waitingRevision;
        state=updated.copy();
        if (changed) journalCacheKey="";
        if (acknowledged) waiting=false;
        if (changed || acknowledged) rebuildWidgets();
    }
    private CompoundTag life() { return state.getCompound("LifeSimulation"); }
    private Component text(String key,Object... args) { return Component.translatable("life.expansion."+key,args); }
    private String action(int slot) { String value=life().getCompound("Routine").getCompound(Integer.toString(slot)).getString("Action");return ACTIONS.contains(value)?value:"rest"; }
    private String condition(int slot) { String value=life().getCompound("Routine").getCompound(Integer.toString(slot)).getString("Condition");return CONDITIONS.contains(value)?value:"any"; }
    private void send(String action) {
        if (waiting) return;
        waiting=true;waitingRevision=state.getLong("LifeActionRevision");
        PacketDistributor.sendToServer(new OuterSystemPayloads.ServerboundLifeAction(action));
        rebuildWidgets();
    }
    private void runYear() {
        if (ready() && life().hasUUID("SimulationId")) send("routine:run:"+life().getUUID("SimulationId")+":"+life().getInt("Age"));
    }
    private boolean ready() {
        return life().getBoolean("Active") && life().getBoolean("Alive") && !life().getBoolean("Finished")
                && !life().contains("CurrentEvent") && !life().contains("Story") && !life().contains("Battle")
                && !LifeSimulationWorldMap.hasActiveJourney(life()) && !LifeSimulationWorldMap.hasPendingArrival(life());
    }
    private void back() {
        var life=life();
        OuterSystemClient.transitionTo(!life.getBoolean("Finished") && (life.contains("Story") || life.contains("Battle"))
                ? (com.example.immortal_cultivation_mod.system.LifeCardBattle.enabled(life().getCompound("Battle"))
                    ? new LifeCardBattleScreen(state) : new LifeEncounterScreen(state)) : new LifeSimulationScreen(state));
    }
    @Override protected void initContent() {
        int w=canvasWidth(),h=canvasHeight();
        if (journal) {
            int cell=(w-30)/4;
            for (var view : JournalView.values()) {
                var tab=button(6+view.ordinal()*(cell+6),0,cell,22,text("journal."+view.key),()->{
                    journalView=view;scroll=0;draggingScroll=false;journalCacheKey="";rebuildWidgets();
                });
                tab.setFocused(view==journalView);
            }
            if (journalView==JournalView.PEOPLE) initPeopleWidgets();
            button(6,h-28,100,22,Component.translatable("life.battle.back"),this::back);return;
        }
        if (!selectionMenu.isEmpty()) {
            var values=selectionMenu.equals("action")?ACTIONS:CONDITIONS;
            int columns=selectionMenu.equals("action")?3:1,cell=(w-24)/columns;
            for(int i=0;i<values.size();i++) {
                String value=values.get(i);
                Component label=Component.translatable(selectionMenu.equals("action")?"screen.immortal_cultivation_mod.life.activity."+value:"life.expansion.routine."+value);
                var choice=wrappedButton(12+i%columns*cell,48+i/columns*39,cell-6,32,label,()->{
                    String chosenAction=selectionMenu.equals("action")?value:action(selectedSlot);
                    String chosenCondition=selectionMenu.equals("condition")?value:condition(selectedSlot);
                    selectionMenu="";send("routine:slot:"+selectedSlot+":"+chosenAction+":"+chosenCondition);
                });
                String reason=selectionMenu.equals("action")?LifeSimulationSystem.activityUnavailable(life(),value):"";
                choice.active=!waiting&&reason.isBlank();
                if (ACTIONS.contains(value)) choice.setTooltip(Tooltip.create(label.copy().append("\n").append(cost(value))
                        .append(reason.isBlank()?Component.empty():Component.literal("\n").append(Component.translatable("life.expansion.activity_blocked."+reason)))));
                else choice.setTooltip(Tooltip.create(text("routine.conditions_help")));
            }
            button(6,h-28,100,22,Component.translatable("life.battle.back"),()->{selectionMenu="";rebuildWidgets();});return;
        }
        for(int i=0;i<3;i++) {
            int slot=i,y=39+i*80;
            var activity=button(45,y+9,296,25,Component.translatable("screen.immortal_cultivation_mod.life.activity."+action(i)),()->{selectedSlot=slot;selectionMenu="action";rebuildWidgets();});
            var rule=button(351,y+9,w-366,25,text("routine.condition",text("routine."+condition(i))),()->{selectedSlot=slot;selectionMenu="condition";rebuildWidgets();});
            activity.active=!waiting;rule.active=!waiting;
            activity.setTooltip(Tooltip.create(cost(action(i))));
            rule.setTooltip(Tooltip.create(text("routine.conditions_help")));
        }
        button(6,h-28,100,22,Component.translatable("life.battle.back"),this::back);
        var run=button(116,h-28,w-122,22,text("routine.run"),this::runYear);
        run.active=!waiting && ready();if(!ready())run.setTooltip(Tooltip.create(text("routine.blocked")));
    }
    @Override protected void renderContent(GuiGraphics g,int mx,int my,float pt) {
        int w=canvasWidth(),h=canvasHeight();
        if (journal) {
            renderJournal(g);
            return;
        }
        g.drawString(font,selectionMenu.isEmpty()?text("routine.status",life().getInt("AnnualActionPoints"),life().getInt("TrainingStrain")):text("routine.select."+selectionMenu,selectedSlot+1),12,7,0xFF17325E,false);
        if (!selectionMenu.isEmpty()) return;
        for(int i=0;i<3;i++) {
            int y=39+i*80;
            SystemGuiTextures.drawInsetPanel(g,6,y,w-12,66);
            g.fill(14,y+10,37,y+34,0xFF294D72);g.drawString(font,Integer.toString(i+1),22,y+18,0xFFEFEFEF,false);
            String reason=LifeSimulationSystem.activityUnavailable(life(),action(i));
            g.drawString(font,cost(action(i)),45,y+43,reason.isBlank()?0xFF404040:0xFF906044,false);
        }
        var help=font.split(text("routine.help"),w-24);
        for(int i=0;i<Math.min(2,help.size());i++)g.drawString(font,help.get(i),12,281+i*10,0xFF404040,false);
    }
    private Component cost(String activity){return activity.equals("seclusion")?text("routine.seclusion_cost"):text("routine.cost",1);}
    @Override protected boolean scrollContent(double x,double y,double horizontal,double vertical) {
        if (!journal) return false;
        if (journalView==JournalView.PEOPLE && x<196) changeScroll(true,peopleScroll-(int)(vertical*3));
        else changeScroll(false,scroll-(int)(vertical*3));
        return true;
    }

    private void initPeopleWidgets() {
        button(12,33,162,22,relationshipFilter.label(),()->{
            relationshipFilter=relationshipFilter.next();peopleScroll=0;scroll=0;
            journalCacheKey="";rebuildWidgets();
        });
        List<CompoundTag> people=peopleRecords();
        peopleScroll=Mth.clamp(peopleScroll,0,Math.max(0,people.size()-PEOPLE_VISIBLE));
        selectedPersonRecord(people);
        for (int row=0;row<Math.min(PEOPLE_VISIBLE,people.size()-peopleScroll);row++) {
            CompoundTag person=people.get(peopleScroll+row);
            String id=personId(person);
            Component name=personName(person);
            if (id.equals(selectedPerson)) name=Component.literal("[").append(name).append("]");
            var entry=wrappedButton(51,PEOPLE_TOP+row*42,122,38,Component.empty().append(name).append("\n").append(personStatus(person)),()->{
                selectedPerson=id;scroll=0;journalCacheKey="";rebuildWidgets();
            });
            entry.setTooltip(Tooltip.create(personName(person).copy().append(" · ").append(realm(person.getInt("Realm"))).append("\n").append(personStatus(person))));
        }
    }

    private List<CompoundTag> peopleRecords() {
        var result=new LinkedHashMap<String,CompoundTag>();
        for (var tag:life().getList("Relationships",Tag.TAG_COMPOUND)) {
            var person=(CompoundTag)tag;
            if (observed(person) && relationshipFilter.matches(person)) result.put(personId(person),person);
        }
        CompoundTag memorials=life().getCompound("NpcMemorials");
        for (String id:memorials.getAllKeys().stream().sorted().toList()) {
            var person=memorials.getCompound(id);
            if (observed(person) && relationshipFilter.matches(person)) result.putIfAbsent(personId(person),person);
        }
        return List.copyOf(result.values());
    }

    private boolean observed(CompoundTag record) { return !record.contains("Observed") || record.getBoolean("Observed"); }
    private String personId(CompoundTag person) {
        return person.hasUUID("Id")?person.getUUID("Id").toString():person.getString("Name")+":"+person.getInt("BirthYear");
    }
    private CompoundTag selectedPersonRecord(List<CompoundTag> people) {
        for (var person:people) if (personId(person).equals(selectedPerson)) return person;
        if (people.isEmpty()) {selectedPerson="";return new CompoundTag();}
        selectedPerson=personId(people.getFirst());journalCacheKey="";return people.getFirst();
    }
    private Component known(String key) {
        return Component.translatable(!key.isBlank()&&I18n.exists(key)?key:"life.expansion.journal.no_details");
    }
    private Component personName(CompoundTag person) {
        var name = !person.getString("NameKey").isBlank()&&I18n.exists(person.getString("NameKey"))
                ? Component.translatable(person.getString("NameKey"))
                : person.getString("Name").isBlank()?text("journal.person.unknown"):Component.literal(person.getString("Name"));
        return person.getBoolean("Alive") ? name : name.copy().withStyle(style -> style.withColor(0xB43434));
    }
    private Component personStatus(CompoundTag person) {
        return text("journal.person.status",text(person.getInt("Age")<16?"journal.person.child":"journal.person.adult"),
                Component.translatable(person.getBoolean("Alive")?"screen.immortal_cultivation_mod.life.alive":"screen.immortal_cultivation_mod.life.deceased"));
    }
    private Component realm(int index) {
        if (index<=0) return Component.translatable("screen.immortal_cultivation_mod.life.realm.mortal");
        return Component.translatable("screen.immortal_cultivation_mod.life.realm."+Math.min(9,(index-1)/3+1)).append(" ")
                .append(Component.translatable("screen.immortal_cultivation_mod.life.stage."+Math.floorMod(index-1,3)));
    }
    private void addLog(List<Component> components,int age,String key) {
        if (!key.isBlank()) components.add(Component.translatable("screen.immortal_cultivation_mod.life.log_line",age,known(key)));
    }
    private void heading(List<Component> components,String key) { components.add(text(key).copy().withStyle(ChatFormatting.DARK_BLUE)); }

    private List<FormattedCharSequence> journalLines() {
        if (journalLanguage!=Language.getInstance()) {journalLanguage=Language.getInstance();journalCacheKey="";}
        if (journalView==JournalView.PEOPLE) selectedPersonRecord(peopleRecords());
        String key=journalView.name()+":"+selectedPerson;
        if (key.equals(journalCacheKey)) return journalCache;
        List<Component> components=new ArrayList<>();
        switch (journalView) {
            case HISTORY -> {
                for (var tag:life().getList("History",Tag.TAG_COMPOUND)) {
                    var entry=(CompoundTag)tag;if(observed(entry))addLog(components,entry.getInt("Age"),entry.getString("Key"));
                }
            }
            case ENDINGS -> {
                var records=life().getCompound("StoryJournal");
                for (String id:records.getAllKeys().stream().sorted(java.util.Comparator.comparingInt(k->records.getCompound(k).getInt("Year"))).toList()) {
                    var record=records.getCompound(id);addLog(components,record.getInt("Year"),record.getString("TextKey"));
                    if(record.contains("TextParts"))components.add(com.example.immortal_cultivation_mod.system.LifeAuthoredStories.text(record));
                }
            }
            case LEADS -> addLeads(components);
            case PEOPLE -> addPersonDetails(components,selectedPersonRecord(peopleRecords()));
        }
        if (components.isEmpty()) components.add(text(journalView==JournalView.ENDINGS?"journal.empty":"journal.no_details"));
        List<FormattedCharSequence> lines=new ArrayList<>();
        for (var component:components) lines.addAll(font.split(component,journalView==JournalView.PEOPLE?345:canvasWidth()-44));
        journalCache=List.copyOf(lines);journalCacheKey=key;return journalCache;
    }

    private void addLeads(List<Component> components) {
        heading(components,"journal.current");
        var event=life().getCompound("CurrentEvent");
        if (!event.isEmpty()) {
            if (!event.getString("TitleKey").isBlank()) components.add(known(event.getString("TitleKey")));
            if (!event.getString("LocationKey").isBlank()) components.add(text("journal.location",known(event.getString("LocationKey"))));
            if (!event.getString("ObjectiveKey").isBlank()) components.add(text("journal.objective",known(event.getString("ObjectiveKey"))));
            if (!event.getString("TextKey").isBlank()) components.add(known(event.getString("TextKey")));
        } else if (life().contains("Battle",Tag.TAG_COMPOUND)) {
            components.add(text("journal.objective",known(life().getCompound("Battle").getString("ObjectiveKey"))));
        } else components.add(text("journal.no_current"));

        var map=LifeSimulationWorldMap.state(life());
        for (var location:LifeSimulationWorldMap.locations()) {
            if (!LifeSimulationWorldMap.discovered(map,location.id())) continue;
            var local=LifeSimulationWorldMap.locationState(map,location.id());
            if (local.getInt("MainQuestStage")<=0&&!local.getBoolean("MainQuestCompleted")) continue;
            if (local.getBoolean("MainQuestCompleted")) {
                components.add(text("journal.quest_done",known(location.nameKey())));
                components.add(known("life.immortal_cultivation_mod.map.quest."+location.id()+".ending."+local.getString("MainQuestEnding")+".choice"));
            } else components.add(text("journal.quest",known(location.nameKey()),local.getInt("MainQuestStage")));
        }
        heading(components,"journal.promises");
        boolean promise=addPromise(components,life().getCompound("SpiritualOath"));
        for (var tag:life().getList("Promises",Tag.TAG_COMPOUND)) promise|=addPromise(components,(CompoundTag)tag);
        if (!promise) components.add(text("journal.no_promises"));

        heading(components,"journal.letters");
        int before=components.size();
        // Only messages already logged or expressly discovered are readable. DelayedStories and hidden graph data are never read.
        for (var tag:life().getList("History",Tag.TAG_COMPOUND)) {
            var entry=(CompoundTag)tag;String key=entry.getString("Key");
            if (observed(entry)&&(key.contains("letter")||key.contains("memory")||key.contains("memorial"))) addLog(components,entry.getInt("Age"),key);
        }
        for (String collection:List.of("DiscoveredLetters","DiscoveredMemories")) {
            for (var tag:life().getList(collection,Tag.TAG_COMPOUND)) {
                var entry=(CompoundTag)tag;
                if (observed(entry)) addLog(components,entry.contains("Year")?entry.getInt("Year"):entry.getInt("Age"),entry.getString("TextKey"));
            }
        }
        for (var tag:life().getList("Memories",Tag.TAG_COMPOUND)) {
            var memory=(CompoundTag)tag;
            components.add(text("journal.memory",memory.getString("Name"),memory.getInt("Age"),realm(memory.getInt("HighestRealm"))));
        }
        if (components.size()==before) components.add(text("journal.no_letters"));
    }

    private boolean addPromise(List<Component> components,CompoundTag promise) {
        if (!promise.getBoolean("Accepted")||!observed(promise)) return false;
        String terms=promise.getString("TermsKey");
        if (terms.isBlank()) terms=promise.getString("TextKey");
        components.add(known(terms));
        String status=promise.getBoolean("Fulfilled")?"fulfilled":promise.getBoolean("Broken")?"broken":"pending";
        components.add(text("journal.promise_status",text("journal.promise."+status)));
        if (promise.contains("Deadline")) components.add(text("journal.deadline",promise.getInt("Deadline"),Math.max(0,promise.getInt("Deadline")-life().getInt("Age"))));
        return true;
    }

    private void addPersonDetails(List<Component> components,CompoundTag person) {
        if (person.isEmpty()) return;
        components.add(text("journal.person.identity",personName(person)));
        components.add(person.contains("BirthYear")?text("journal.person.age",person.getInt("Age"),person.getInt("BirthYear"))
                : text("journal.person.age_unknown_birth",person.getInt("Age")));
        components.add(text("journal.person.occupation",known("screen.immortal_cultivation_mod.life.occupation."+person.getString("Occupation"))));
        components.add(text("journal.person.relation",known("screen.immortal_cultivation_mod.life.relation."+person.getString("Relation"))));
        components.add(text("journal.person.trust",person.getInt("Trust"),person.getInt("Affinity")));
        components.add(text("journal.person.health",person.getInt("Health")));
        if (person.getBoolean("Friend")) components.add(text("journal.person.friend"));
        if (person.getBoolean("Protector")) components.add(text("journal.person.protector"));
        if (person.getBoolean("Nemesis")) components.add(text("journal.person.nemesis"));
        boolean supports=LifeSimulationBattle.eligibleAlly(person)&&person.getInt("Age")>=16&&person.getInt("InjuredUntil")<=life().getInt("Age");
        if (person.hasUUID("Id")) for (var tag:life().getCompound("Battle").getList("Actors",Tag.TAG_COMPOUND)) {
            var actor=(CompoundTag)tag;
            if (actor.getBoolean("Enemy")&&actor.hasUUID("NpcId")&&actor.getUUID("NpcId").equals(person.getUUID("Id"))) supports=false;
        }
        components.add(text("journal.person.support",text(supports?"journal.person.available":"journal.person.unavailable")));
        if (!person.getBoolean("Alive")) {
            components.add(person.contains("DeathYear")?text("journal.person.death_year",person.getInt("DeathYear")):text("journal.person.death_unknown"));
            String cause=person.getString("DeathCause");
            components.add(text("journal.person.death",known("life.expansion.journal.death."+(List.of("natural_death","battle","story","memory").contains(cause)?cause:"unknown"))));
        }
        heading(components,"journal.person.history");
        int before=components.size();
        for (var tag:person.getList("History",Tag.TAG_COMPOUND)) {
            var entry=(CompoundTag)tag;
            if (observed(entry)) addLog(components,entry.getInt("Age"),entry.getString("Key"));
        }
        if (person.hasUUID("Id")) {
            var records=life().getCompound("StoryJournal");
            for (String storyId:records.getAllKeys().stream().sorted(java.util.Comparator.comparingInt(k->records.getCompound(k).getInt("Year"))).toList()) {
                var record=records.getCompound(storyId);var cast=record.getCompound("Cast");
                boolean participant=false;
                for (String role:cast.getAllKeys()) if(cast.hasUUID(role)&&cast.getUUID(role).equals(person.getUUID("Id"))) participant=true;
                if (participant) addLog(components,record.getInt("Year"),record.getString("TextKey"));
            }
        }
        if (components.size()==before) components.add(text("journal.person.no_history"));
    }

    private void renderJournal(GuiGraphics graphics) {
        int w=canvasWidth(),h=canvasHeight();
        boolean peopleView=journalView==JournalView.PEOPLE;
        if (peopleView) {
            SystemGuiTextures.drawInsetPanel(graphics,6,29,190,h-65);
            SystemGuiTextures.drawInsetPanel(graphics,204,29,w-210,h-65);
            List<CompoundTag> people=peopleRecords();
            peopleScroll=Mth.clamp(peopleScroll,0,Math.max(0,people.size()-PEOPLE_VISIBLE));
            for (int row=0;row<Math.min(PEOPLE_VISIBLE,people.size()-peopleScroll);row++) drawPortrait(graphics,people.get(peopleScroll+row),15,PEOPLE_TOP+4+row*42,28);
            graphics.drawString(font,text("journal.people_count",people.size()),16,291,0xFF404040,false);
            var person=selectedPersonRecord(people);
            if (people.isEmpty()) graphics.drawString(font,Component.translatable("life.relationship.filter.empty"),16,PEOPLE_TOP+8,0xFF606060,false);
            if (!person.isEmpty()) {
                drawPortrait(graphics,person,215,39,32);
                graphics.drawString(font,font.plainSubstrByWidth(personName(person).getString(),310),258,40,person.getBoolean("Alive")?0xFF17325E:0xFFB43434,false);
                graphics.drawString(font,personStatus(person),258,53,0xFF404040,false);
                graphics.drawString(font,realm(person.getInt("Realm")),258,66,0xFF404040,false);
            }
            drawScrollbar(graphics,scrollbar(true));
        } else SystemGuiTextures.drawInsetPanel(graphics,6,29,w-12,h-65);
        List<FormattedCharSequence> lines=journalLines();
        int visible=peopleView?19:23,startY=peopleView?88:40,startX=peopleView?215:17;
        scroll=Mth.clamp(scroll,0,Math.max(0,lines.size()-visible));
        scissorCanvas(graphics,startX,startY,w-27,h-39);
        for(int row=0;row<visible&&row+scroll<lines.size();row++)graphics.drawString(font,lines.get(row+scroll),startX,startY+row*11,0xFF202020,false);
        graphics.disableScissor();
        drawScrollbar(graphics,scrollbar(false));
    }
    private void drawPortrait(GuiGraphics graphics,CompoundTag person,int x,int y,int size) {
        var sprite=LifeSimulationSprites.person(person);
        int pixels=sprite.pixels();
        graphics.blit(sprite.texture(),x,y,size,size,0,0,pixels,pixels,pixels,pixels);
    }

    private Scrollbar scrollbar(boolean people) {
        boolean peopleView=journalView==JournalView.PEOPLE;
        return people?new Scrollbar(180,59,10,222,PEOPLE_VISIBLE,peopleRecords().size(),peopleScroll,true)
                : new Scrollbar(canvasWidth()-19,peopleView?84:33,10,peopleView?215:266,peopleView?19:23,journalLines().size(),scroll,false);
    }
    private void drawScrollbar(GuiGraphics graphics,Scrollbar bar) {
        SystemGuiTextures.drawVerticalScrollbar(graphics,bar.x,bar.y,bar.width,bar.height,bar.offset,bar.visible,bar.total);
    }
    private void changeScroll(boolean people,int value) {
        Scrollbar bar=scrollbar(people);
        int next=Mth.clamp(value,0,Math.max(0,bar.total-bar.visible));
        if (people) {if (next!=peopleScroll) {peopleScroll=next;rebuildWidgets();}}
        else scroll=next;
    }
    @Override protected boolean clickContent(double x,double y,int button) {
        if (!journal||button!=0) return false;
        for (boolean people:journalView==JournalView.PEOPLE?List.of(true,false):List.of(false)) {
            Scrollbar bar=scrollbar(people);
            if (x<bar.x||x>=bar.x+bar.width||y<bar.y||y>=bar.y+bar.height) continue;
            if (y<bar.y+bar.width) changeScroll(people,bar.offset-1);
            else if (y>=bar.y+bar.height-bar.width) changeScroll(people,bar.offset+1);
            else {
                int[] thumb=scrollThumb(bar);
                draggingScroll=true;draggingPeople=people;
                scrollGrab=y>=thumb[0]&&y<thumb[0]+thumb[1]?(int)y-thumb[0]:thumb[1]/2;
                dragScrollbar(y,bar);
            }
            return true;
        }
        return false;
    }
    @Override protected boolean dragContent(double x,double y,int button) {
        if (!journal||!draggingScroll||button!=0) return false;
        dragScrollbar(y,scrollbar(draggingPeople));return true;
    }
    @Override protected void releaseContent(double x,double y,int button) { draggingScroll=false; }
    private int[] scrollThumb(Scrollbar bar) {
        int inner=Math.max(1,bar.height-bar.width*2-4),maximum=Math.max(0,bar.total-bar.visible);
        int thumb=maximum==0?inner:Math.max(9,inner*bar.visible/Math.max(bar.visible,bar.total));
        thumb=Math.min(inner,thumb);
        int top=bar.y+bar.width+2+(maximum==0?0:Math.round((inner-thumb)*Mth.clamp(bar.offset,0,maximum)/(float)maximum));
        return new int[]{top,thumb,inner-thumb};
    }
    private void dragScrollbar(double y,Scrollbar bar) {
        int[] thumb=scrollThumb(bar);int maximum=Math.max(0,bar.total-bar.visible);
        float fraction=thumb[2]<=0?0:Mth.clamp((float)(y-scrollGrab-bar.y-bar.width-2)/thumb[2],0,1);
        changeScroll(bar.people,Math.round(maximum*fraction));
    }
    private record Scrollbar(int x,int y,int width,int height,int visible,int total,int offset,boolean people) {}

    private enum JournalView {
        HISTORY("history"), ENDINGS("endings"), LEADS("leads"), PEOPLE("people");
        private final String key;
        JournalView(String key) { this.key=key; }
    }
}
