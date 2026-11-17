package com.iqbal.reelblock;

import java.util.Random;

/** Quran verses and hadith shown on the block screen. {arabic, english, reference} */
final class Verses {

    // Shown when Reels / Shorts are blocked: about time and wasting life.
    static final String[][] TIME = {
            {"وَالْعَصْرِ ۝ إِنَّ الْإِنسَانَ لَفِي خُسْرٍ ۝ إِلَّا الَّذِينَ آمَنُوا وَعَمِلُوا الصَّالِحَاتِ",
                    "By time, indeed mankind is in loss, except those who believe and do righteous deeds.",
                    "Surah Al-Asr 103:1–3"},
            {"وَالَّذِينَ هُمْ عَنِ اللَّغْوِ مُعْرِضُونَ",
                    "(Successful are the believers) who turn away from what is vain and useless.",
                    "Surah Al-Mu'minun 23:3"},
            {"الَّذِي خَلَقَ الْمَوْتَ وَالْحَيَاةَ لِيَبْلُوَكُمْ أَيُّكُمْ أَحْسَنُ عَمَلًا",
                    "He created death and life to test you, which of you is best in deeds.",
                    "Surah Al-Mulk 67:2"},
            {"أَفَحَسِبْتُمْ أَنَّمَا خَلَقْنَاكُمْ عَبَثًا وَأَنَّكُمْ إِلَيْنَا لَا تُرْجَعُونَ",
                    "Did you think We created you without purpose, and that you would not be returned to Us?",
                    "Surah Al-Mu'minun 23:115"},
            {"نِعْمَتَانِ مَغْبُونٌ فِيهِمَا كَثِيرٌ مِنَ النَّاسِ: الصِّحَّةُ وَالْفَرَاغُ",
                    "There are two blessings in which many people are at a loss: health and free time.",
                    "Sahih al-Bukhari 6412"},
            {"مِنْ حُسْنِ إِسْلَامِ الْمَرْءِ تَرْكُهُ مَا لَا يَعْنِيهِ",
                    "Part of the excellence of a person's Islam is leaving what does not concern him.",
                    "Jami' at-Tirmidhi 2317"},
            {"",
                    "A person's feet will not move on the Day of Judgement until he is asked about his life, how he spent it, and his youth, how he used it.",
                    "Jami' at-Tirmidhi 2417"},
    };

    // Shown when adult content is blocked: about lowering the gaze.
    static final String[][] GAZE = {
            {"قُل لِّلْمُؤْمِنِينَ يَغُضُّوا مِنْ أَبْصَارِهِمْ وَيَحْفَظُوا فُرُوجَهُمْ ۚ ذَٰلِكَ أَزْكَىٰ لَهُمْ",
                    "Tell the believing men to lower their gaze and guard their chastity. That is purer for them.",
                    "Surah An-Nur 24:30"},
            {"وَلَا تَقْرَبُوا الزِّنَىٰ ۖ إِنَّهُ كَانَ فَاحِشَةً وَسَاءَ سَبِيلًا",
                    "Do not even come near unlawful intimacy. It is indecent and an evil way.",
                    "Surah Al-Isra 17:32"},
            {"إِنَّ السَّمْعَ وَالْبَصَرَ وَالْفُؤَادَ كُلُّ أُولَٰئِكَ كَانَ عَنْهُ مَسْئُولًا",
                    "Indeed, the hearing, the sight and the heart, all of these will be questioned.",
                    "Surah Al-Isra 17:36"},
            {"يَعْلَمُ خَائِنَةَ الْأَعْيُنِ وَمَا تُخْفِي الصُّدُورُ",
                    "He knows the stealing glances of the eyes and what the hearts conceal.",
                    "Surah Ghafir 40:19"},
            {"أَلَمْ يَعْلَم بِأَنَّ اللَّهَ يَرَىٰ",
                    "Does he not know that Allah sees?",
                    "Surah Al-Alaq 96:14"},
            {"يَا عَلِيُّ، لَا تُتْبِعِ النَّظْرَةَ النَّظْرَةَ، فَإِنَّ لَكَ الْأُولَى وَلَيْسَتْ لَكَ الْآخِرَةُ",
                    "O Ali, do not follow one glance with another. The first is forgiven, but not the second.",
                    "Sunan Abi Dawud 2149"},
            {"",
                    "O young people, whoever can marry should marry, for it lowers the gaze and guards chastity. Whoever cannot, should fast, for it is a shield.",
                    "Sahih al-Bukhari 5066"},
    };

    private static final Random R = new Random();

    static String[] pick(boolean adult) {
        String[][] list = adult ? GAZE : TIME;
        return list[R.nextInt(list.length)];
    }

    private Verses() { }
}
