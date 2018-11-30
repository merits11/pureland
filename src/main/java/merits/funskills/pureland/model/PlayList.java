package merits.funskills.pureland.model;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Joiner;

import lombok.Getter;

import static merits.funskills.pureland.model.Tag.Chanting;
import static merits.funskills.pureland.model.Tag.Chinese;
import static merits.funskills.pureland.model.Tag.Combine;
import static merits.funskills.pureland.model.Tag.DharmaTalk;
import static merits.funskills.pureland.model.Tag.English;
import static merits.funskills.pureland.model.Tag.Music;
import static merits.funskills.pureland.model.Tag.Pop;
import static merits.funskills.pureland.model.Tag.Private;
import static merits.funskills.pureland.model.Tag.SortByTimeAndKeyAsc;
import static merits.funskills.pureland.model.Tag.SortByTimeDesc;
import static merits.funskills.pureland.model.Tag.Sutra;
import static merits.funskills.pureland.model.Tag.TAG;
import static merits.funskills.pureland.model.Tag.Virtual;

public enum PlayList {

    One(1, "Guan Yin bodhisattva", TAG(Chinese, Chanting, English)),
    Two(2, "Platform sutra", TAG(Sutra, Chinese)),
    AmitabhaChanting(3, "Chanting of Amitabha", TAG(Chanting, Chinese, English)),
    SutraChantings(4, "", TAG(Chanting, Chinese)),
    Five(5, "Dharma music", TAG(Music, Chinese, English)),
    Six(6, "Diamond sutra", TAG(Sutra, Chinese)),
    HeartSutra(7, "Heart Sutra", TAG(Sutra, Chinese)),
    Eight(8, "Lotus sutra", TAG(Sutra, Chinese)),
    Nine(9, "Great dharma drum", TAG(DharmaTalk, Chinese)),

    Ten(10, "Sutra of Amitabha", TAG(Sutra, Chinese)),
    Eleven(11, "Last teachings of Buddha", TAG(Sutra, Chinese)),
    Twelve(12, "Master Empty Cloud", TAG(Chinese, DharmaTalk)),
    AHan(13, "Ah Han Sutra", TAG(Chinese, Sutra)),
    Fourteen(14, "Wei Mo Jie sutra", TAG(Chinese, Sutra)),
    Fifteen(15, "Medicine Buddha sutra", TAG(Chinese, Sutra)),
    GreatCompassionPrayer(16, "Great Compassion Repentance", TAG(Chinese, Chanting)),
    DizangSutra(17, "Sutra of Di Zang bodhisattva", TAG(Chinese, Sutra)),
    HuayanSutra(18, "", TAG(Chinese, Sutra)),
    YinGuang(19, "Master Yin Guang", TAG(Chinese, DharmaTalk)),

    Twenty(20, "Theravada sutras", TAG(Sutra, English)),
    EnglishPlatformSutra(21, "Platform sutra", TAG(English, Sutra)),
    EnglishLotusSutra(22, "Lotus sutra", TAG(English, Sutra)),
    EarthStore(23, "Sutra of Earth Store bodhisattva", TAG(Sutra, English)),
    VimalakirtiSutra(24, "Vimalakirti sutra", TAG(English, Sutra)),
    EnglishPureLand(25, "Pure land sutras", TAG(English, Sutra)),
    TwentySix(26, "Diamond Sutra", TAG(Sutra, English)),
    EnglishBuddhistSongs(27, "English Songs", TAG(Music, English)),
    EnglishMedicineBuddha(28, "Medicine Buddha sutra", TAG(Sutra, English)),
    EnglishHeartSutra(29, "Heart Sutra", TAG(Sutra, English)),

    MiscellaneousSutras(31, "", TAG(Chinese, Sutra)),
    BuddhistSongs(32, "", TAG(Chinese, Music, English)),
    LengYan(33, "Leng yen sutra", TAG(Chinese, Sutra)),
    InfiniteLifeSutra(34, "Infinite life sutra", TAG(Chinese, Sutra)),
    VisualizationSutra(35, "Sutra on visualization of Amitabha", TAG(Chinese, Sutra)),
    InfiniteLifeSutraChinKung(36, "Infinite life Sutra, Master Chin Kung", TAG(Chinese, Sutra)),
    AmitabhaChantingChinKung(37, "Chanting of Amitabha, Master Chin Kung", TAG(Chinese)),
    XingYuanPin(38, "Ten Great Vows", TAG(Chinese, Sutra)),
    PuMenPin(39, "Pu Men Pin of Lotus Sutra", TAG(Chinese, Sutra)),

    //English sutra
    AmitabhaSutraEnglish(41, "", TAG(English, Sutra)),
    InfiniteLifeSutraEnglish(42, "", TAG(English, Sutra)),
    VisualizationSutraEnglish(43, "", TAG(English, Sutra)),
    VowsOfSamantabhadra(44, "", TAG(English, Sutra)),

    //English dharma talk/chant
    ThichNhatHanh(50, "Thich Nhat Hanh", TAG(DharmaTalk, English)),
    WordOfBuddha(51, "Word of the Buddha", TAG(DharmaTalk, English)),
    EnglishSutraChantings(52, "", TAG(Chanting, English)),

    //Chinese Dharma talks
    ShengYen(60, "Lectures by Master Sheng Yen", TAG(Chinese, DharmaTalk)),
    //InfiniteSutraTalksByJingKong(61, "", TAG(Chinese, DharmaTalk)),
    ThousandYearsBodhiRoad(61, "Thousand Years of Bodhi Road", TAG(Chinese, DharmaTalk)),
    FaGuJiangTang(62, "Dharma Drum Mountain lectures ", TAG(Chinese, DharmaTalk)),
    EnglishFaGuJiangTang(63, "Dharma Drum Mountain lectures, Chinese and English", TAG(English, Chinese, DharmaTalk)),

    //Chinese Chanting
    PureLandVows(80, "", TAG(Chinese, Chanting)),
    Repentance(81, "88 Buddhas Repentance", TAG(Chinese, Chanting)),
    ThriceYearning(82, "Thrice Yearning Ceremony", TAG(Chinese, Chanting)),
    MorningService(83, "Morning service", TAG(Chinese, Chanting, English)),
    EveningService(84, "Evening service", TAG(Chinese, Chanting, English)),
    EmperorLiangRepentanceChant(85, "Emperor Liang Repentance Chant",TAG(Chinese, Chanting)),

    //Chinese sutra
    InfiniteLifePlain(90, "Infinite Life Sutra In Plain Chinese", TAG(Chinese, Sutra)),
    GreatCompassionSutra(91, "Great Compassion Mantra Sutra", TAG(Chinese, Sutra)),
    EmperorLiangRepentance(92, "Emperor Liang Repentance",TAG(Chinese, Sutra)),

    //private list
    OneHundredThree(103, "Classical music", TAG(Private, Music, Chinese, Pop)),

    OneHundredFour(104, "Pop music", TAG(Private, Music, Chinese, Pop, SortByTimeDesc)),

    PersonalList(105, "", TAG(Private, DharmaTalk, Chinese)),

    MingHai(106, "", TAG(Chinese, DharmaTalk, Private, SortByTimeAndKeyAsc)),

    CompassionRepel(108, "", TAG(Private, Chanting, Chinese, SortByTimeDesc)),

    AllMusic(200, "", TAG(Combine, Chinese, Music, Virtual, SortByTimeDesc));

    @Getter
    private final int listNumber;

    private final String text;

    @Getter
    private final List<Tag> tags;

    PlayList(final int num, final String text, List<Tag> tags) {
        this.listNumber = num;
        this.tags = tags;
        this.text = text;
    }

    public String getText() {
        if (StringUtils.isEmpty(text)) {
            return Joiner.on(" ").join(
                PlayListUtils.splitByCamelCase(this.toString())
            );
        } else {
            return text;
        }
    }

    public boolean isAccessible() {
        return !isTagged(Private);
    }

    public boolean isAccessible(final List<Tag> inputTags) {
        if (inputTags == null) {
            return isAccessible();
        }
        return isAccessible() || inputTags.contains(Private);
    }

    public boolean isShuffle() {
        return isTagged(Tag.Music);
    }

    public boolean isTagged(final Tag tag) {
        return tags.stream().filter(t -> tag.equals(t)).findAny().isPresent();
    }

    public boolean isTagged(final List<Tag> tags) {
        for (Tag tag : tags) {
            if (!isTagged(tag)) {
                return false;
            }
        }
        return true;
    }

}
