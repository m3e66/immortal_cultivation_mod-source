package com.example.immortal_cultivation_mod.guidance;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Checks actual runtime resources and Minecraft's component formatter, not just source JSON. */
@GameTestHolder("immortal_cultivation_guidance")
@PrefixGameTestTemplate(false)
public final class GuidanceTranslationGameTests {
    private static final Pattern ARGUMENT = Pattern.compile("%(?:[0-9]+\\$)?[sd]");
    private static boolean guidanceKey(String key) {
        return key.startsWith("guidance.immortal_cultivation_mod.")
                || key.startsWith("screen.immortal_cultivation_mod.dao_guidance")
                || key.startsWith("hud.immortal_cultivation_mod.dao_guidance.");
    }

    private static Map<String,String> load(String locale) throws Exception {
        var path="/assets/immortal_cultivation_mod/lang/"+locale+".json";
        var stream=GuidanceTranslationGameTests.class.getResourceAsStream(path);
        if(stream==null)throw new IllegalStateException("Missing packaged language: "+locale);
        var result=new HashMap<String,String>();
        try(var reader=new JsonReader(new InputStreamReader(stream,StandardCharsets.UTF_8))) {
            reader.beginObject();
            while(reader.hasNext()) {
                String key=reader.nextName();
                if(!guidanceKey(key)){reader.skipValue();continue;}
                if(reader.peek()!=JsonToken.STRING)throw new IllegalStateException("Non-text guidance: "+key);
                if(result.putIfAbsent(key,reader.nextString())!=null)
                    throw new IllegalStateException("Duplicate guidance key: "+key);
            }
            reader.endObject();
        }
        return result;
    }

    private static final class TestLanguage extends Language {
        private final Map<String,String> values;
        TestLanguage(Map<String,String> values){this.values=values;}
        @Override public String getOrDefault(String key,String fallback){return values.getOrDefault(key,fallback);}
        @Override public boolean has(String key){return values.containsKey(key);}
        @Override public boolean isDefaultRightToLeft(){return false;}
        @Override public FormattedCharSequence getVisualOrder(FormattedText text){
            return FormattedCharSequence.forward(text.getString(),Style.EMPTY);
        }
    }

    private static void require(GameTestHelper h,Map<String,String> values,String key,String locale) {
        h.assertTrue(values.containsKey(key)&&!values.get(key).isBlank(),"Missing "+locale+" translation: "+key);
    }

    @GameTest(template="fate_test")
    public static void everyChapterObjectiveAndNoticeRendersInBothLanguages(GameTestHelper h) throws Exception {
        var english=load("en_us");var chinese=load("zh_cn");
        h.assertTrue(english.keySet().equals(chinese.keySet()),"Both languages cover exactly the same guidance keys");
        Language original=Language.getInstance();
        try {
            for(String locale:new String[]{"en_us","zh_cn"}) {
                var values=locale.equals("en_us")?english:chinese;
                Language.inject(new TestLanguage(values));
                for(var chapter:DaoGuidanceContent.chapters()) {
                    require(h,values,DaoGuidanceContent.chapterKey(chapter.id(),"name"),locale);
                    require(h,values,DaoGuidanceContent.chapterKey(chapter.id(),"intro"),locale);
                    require(h,values,"guidance.immortal_cultivation_mod.unlock."+chapter.id(),locale);
                    for(var objective:chapter.objectives())for(String suffix:new String[]{"name","detail"})
                        require(h,values,DaoGuidanceContent.objectiveKey(chapter.id(),objective.id(),suffix),locale);
                    String unlock=Component.translatable("guidance.immortal_cultivation_mod.unlock_chapter",
                            Component.translatable(DaoGuidanceContent.chapterKey(chapter.id(),"name"))).getString();
                    h.assertTrue(!unlock.contains("guidance.immortal_cultivation_mod."),"Unlock renders chapter name: "+chapter.id());
                }
                for(var entry:values.entrySet()) {
                    String key=entry.getKey(),value=entry.getValue();
                    h.assertTrue(!value.isBlank()&&!value.equals(key)&&!value.contains("\uFFFD")&&!value.contains("TODO"),"Valid text: "+locale+" "+key);
                    h.assertTrue(ARGUMENT.matcher(english.get(key)).results().count()==ARGUMENT.matcher(chinese.get(key)).results().count(),"Matching placeholders: "+key);
                    Object[] arguments={"[key]","[value]","[count]","[target]"};
                    String rendered=Component.translatable(key,arguments).getString();
                    h.assertTrue(!rendered.equals(key)&&!ARGUMENT.matcher(rendered).find(),"Minecraft formats guidance text: "+locale+" "+key);
                }
            }
        } finally {Language.inject(original);}
        h.succeed();
    }
}
