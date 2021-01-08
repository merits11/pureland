package merits.funskills.pureland.model;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.collect.ImmutableSet.of;
import static merits.funskills.pureland.model.PlayList.AmitabhaChanting;
import static merits.funskills.pureland.model.PlayList.AmitabhaChantingChinKung;
import static merits.funskills.pureland.model.PlayList.AmitabhaSutraEnglish;
import static merits.funskills.pureland.model.PlayList.BabySongsChinese;
import static merits.funskills.pureland.model.PlayList.BabySongsEnglish;
import static merits.funskills.pureland.model.PlayList.BabyStoriesChinese;
import static merits.funskills.pureland.model.PlayList.BabyStoriesEnglish;
import static merits.funskills.pureland.model.PlayList.DizangSutra;
import static merits.funskills.pureland.model.PlayList.EarthStore;
import static merits.funskills.pureland.model.PlayList.Eight;
import static merits.funskills.pureland.model.PlayList.Eleven;
import static merits.funskills.pureland.model.PlayList.EmperorLiangRepentance;
import static merits.funskills.pureland.model.PlayList.EmperorLiangRepentanceChant;
import static merits.funskills.pureland.model.PlayList.EnglishFaGuJiangTang;
import static merits.funskills.pureland.model.PlayList.EnglishHeartSutra;
import static merits.funskills.pureland.model.PlayList.EnglishLotusSutra;
import static merits.funskills.pureland.model.PlayList.EnglishMedicineBuddha;
import static merits.funskills.pureland.model.PlayList.EnglishPlatformSutra;
import static merits.funskills.pureland.model.PlayList.EnglishPureLand;
import static merits.funskills.pureland.model.PlayList.EnglishSutraChantings;
import static merits.funskills.pureland.model.PlayList.FaGuJiangTang;
import static merits.funskills.pureland.model.PlayList.FavoriteSutras;
import static merits.funskills.pureland.model.PlayList.Fifteen;
import static merits.funskills.pureland.model.PlayList.Five;
import static merits.funskills.pureland.model.PlayList.Fourteen;
import static merits.funskills.pureland.model.PlayList.GreatCompassionPrayer;
import static merits.funskills.pureland.model.PlayList.GreatCompassionSutra;
import static merits.funskills.pureland.model.PlayList.HeartSutra;
import static merits.funskills.pureland.model.PlayList.HuayanSutra;
import static merits.funskills.pureland.model.PlayList.InfiniteLifeSutra;
import static merits.funskills.pureland.model.PlayList.InfiniteLifeSutraChinKung;
import static merits.funskills.pureland.model.PlayList.InfiniteLifeSutraEnglish;
import static merits.funskills.pureland.model.PlayList.LengYan;
import static merits.funskills.pureland.model.PlayList.Nine;
import static merits.funskills.pureland.model.PlayList.One;
import static merits.funskills.pureland.model.PlayList.OneHundredFour;
import static merits.funskills.pureland.model.PlayList.OneHundredThree;
import static merits.funskills.pureland.model.PlayList.PersonalList;
import static merits.funskills.pureland.model.PlayList.PuMenPin;
import static merits.funskills.pureland.model.PlayList.PureLandVows;
import static merits.funskills.pureland.model.PlayList.Repentance;
import static merits.funskills.pureland.model.PlayList.ShengYen;
import static merits.funskills.pureland.model.PlayList.Six;
import static merits.funskills.pureland.model.PlayList.SutraChantings;
import static merits.funskills.pureland.model.PlayList.TechTalks;
import static merits.funskills.pureland.model.PlayList.Ten;
import static merits.funskills.pureland.model.PlayList.ThichNhatHanh;
import static merits.funskills.pureland.model.PlayList.ThousandYearsBodhiRoad;
import static merits.funskills.pureland.model.PlayList.ThriceYearning;
import static merits.funskills.pureland.model.PlayList.Twelve;
import static merits.funskills.pureland.model.PlayList.Twenty;
import static merits.funskills.pureland.model.PlayList.TwentySix;
import static merits.funskills.pureland.model.PlayList.Two;
import static merits.funskills.pureland.model.PlayList.VimalakirtiSutra;
import static merits.funskills.pureland.model.PlayList.VisualizationSutra;
import static merits.funskills.pureland.model.PlayList.VisualizationSutraEnglish;
import static merits.funskills.pureland.model.PlayList.VowsOfSamantabhadra;
import static merits.funskills.pureland.model.PlayList.WordOfBuddha;
import static merits.funskills.pureland.model.PlayList.XingYuanPin;
import static merits.funskills.pureland.model.PlayList.YinGuang;

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
            entry("WordOfBuddha", of(WordOfBuddha)),
            entry("GreatCompassionRepentance", of(GreatCompassionPrayer)),
            entry("GreatCompassionSutra", of(GreatCompassionSutra)),
            entry("ThriceYearning", of(ThriceYearning)),
            entry("ThousandYearsBodhiRoad", of(ThousandYearsBodhiRoad)),
            entry("FaGuJiangTang", of(FaGuJiangTang, EnglishFaGuJiangTang)),
            entry("EmperorLiangRepentance", of(EmperorLiangRepentance, EmperorLiangRepentanceChant)),
            entry("EditorFavorite", of(FavoriteSutras)),

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
            entry("PersonalList", of(PersonalList)),
            entry("BabyStories", of(BabyStoriesChinese, BabyStoriesEnglish)),
            entry("BabySongs", of(BabySongsChinese, BabySongsEnglish)),
            entry("TechTalks", of(TechTalks))
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
