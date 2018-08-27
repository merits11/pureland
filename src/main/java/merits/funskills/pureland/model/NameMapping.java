package merits.funskills.pureland.model;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.collect.ImmutableSet.of;
import static merits.funskills.pureland.model.PlayList.*;

public class NameMapping {

    private static Map<String, Set<PlayList>> mappings = Collections.unmodifiableMap(Stream.of(
        entry("JinKong", of(InfiniteLifeSutraChinKung, AmitabhaChantingChinKung)),
        entry("InfiniteLife", of(InfiniteLifeSutra, InfiniteLifeSutraEnglish)),
        entry("Amituofo", of(AmitabhaSutraEnglish, AmitabhaChanting)),
        entry("EarthStore", of(DizangSutra, EarthStore)),
        entry("GuanYin", of(One)),
        entry("SutraChanting", of(SutraChantings, EnglishSutraChantings)),
        entry("LotusSutra", of(EnglishLotusSutra, Eight)),
        entry("DiamondSutra", of(Six, TwentySix)),
        entry("EmptyCloud", of(Twelve)),
        entry("PlatformSutra", of(EnglishPlatformSutra, Two)),
        entry("ShengYan", of(Nine, ShengYen)),
        entry("WeiMoJie", of(Fourteen, VimalakirtiSutra)),
        entry("HeartSutra", of(EnglishHeartSutra, HeartSutra)),
        entry("MedicineBuddhaSutra", of(EnglishMedicineBuddha, Fifteen)),

        entry("PuMenPin", of(PuMenPin)),
        entry("XingYuanPin", of(XingYuanPin, VowsOfSamantabhadra)),
        entry("AHan", of(Twenty)),

        entry("DharmaMusic", of(Five)),

        entry("ThichNhatHanh", of(ThichNhatHanh)),

        entry("HuaYan", of(VowsOfSamantabhadra, HuayanSutra)),
        entry("YiJiaoJing", of(Eleven)),
        entry("LengYan", of(LengYan)),
        entry("ChinesePopMusic", of(OneHundredThree, OneHundredFour)),

        entry("PureLand", of(EnglishPureLand, Ten, InfiniteLifeSutra, VisualizationSutra)),
        entry("VisualizationSutra", of(VisualizationSutra, VisualizationSutraEnglish)),
        entry("YinGuang", of(YinGuang)),
        entry("AmitabhaSutra", of(Ten, AmitabhaSutraEnglish)),
        entry("FaYuanWen", of(PureLandVows)),
        entry("RepentancePrayer", of(Repentance)),
        entry("PersonalList", of(PersonalList))
    ).collect(entriesToMap()));

    private static <K, V> Map.Entry<K, V> entry(K key, V value) {
        return new AbstractMap.SimpleEntry<>(key, value);
    }

    private static <K, U> Collector<Map.Entry<K, U>, ?, Map<K, U>> entriesToMap() {
        return Collectors.toMap((e) -> e.getKey(), (e) -> e.getValue());
    }

    public static Set<PlayList> getPlayLists(final String name) {
        return mappings.get(name);
    }

    public static Set<String> getNames() {
        return mappings.keySet();
    }
}
