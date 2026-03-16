package ru.kors.finalproject.config;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class DemoIdentitySupport {

    private static final Pattern NON_ASCII_ALNUM = Pattern.compile("[^a-z0-9]");

    private static final List<String> STUDENT_FIRST_NAMES = List.of(
            "Aruzhan", "Aigerim", "Dana", "Aisha", "Madina", "Adiya", "Kamila", "Asel", "Zhanel", "Tomiris",
            "Nursultan", "Aslan", "Alikhan", "Dias", "Bekzat", "Yerkebulan", "Arman", "Sanzhar", "Miras", "Daulet",
            "Amina", "Zere", "Diyar", "Nurali", "Erzhan", "Aidana", "Malika", "Ayan", "Dinara", "Ruslan"
    );

    private static final List<String> STUDENT_LAST_NAMES = List.of(
            "Mustafayev", "Nurgaliyev", "Suleimenov", "Tulegenov", "Kassymov", "Omarov", "Abdikarimov",
            "Zhaksylykov", "Serikbayev", "Baimukhanov", "Tursynbekov", "Kenzhebekov", "Myrzabekov", "Saparov",
            "Iskakov", "Sadykov", "Yesenov", "Mukhanbetov", "Zhaparov", "Beketov", "Akhmetov", "Zhunisov",
            "Kaliev", "Dosmagambetov", "Abenov", "Koishybayev", "Amanzholov", "Rakhatov", "Ormanov", "Seitov",
            "Tanirbergenov", "Altynbekov", "Bekturov", "Bolatov", "Kairatov", "Nurlanov", "Yessembekov",
            "Aitbayev", "Karatayev", "Orazbayev", "Sarsenov", "Temirbekov", "Darmenov", "Zholdasov", "Kuatov",
            "Aidarov", "Beknazarov", "Nurbekov", "Ermekov", "Maratov", "Askarov", "Kanatov", "Azamatov",
            "Rustemov", "Baurzhanov", "Yermekov", "Toktarov", "Sagyndykov", "Shokanov", "Darkhanov", "Ulanov",
            "Adilov", "Zhanibekov", "Damirov", "Eldarov", "Muratov", "Abayev", "Sakenov", "Talgatov",
            "Alimzhanov", "Nurzhanov", "Ayanov", "Diasov", "Rysbekov", "Samatov", "Yedigeev", "Baqytzhanov",
            "Maksatov", "Arystanov", "Kenzhaliev", "Bazarbayev", "Nurpeisov", "Tolegenov", "Imanbayev",
            "Kudaibergenov", "Akhanov", "Moldabekov", "Zhumabayev", "Bektasov", "Karimov", "Rakhimzhanov",
            "Satpayev", "Seitzhanov", "Kamalov", "Zhaxybekov", "Nurtaev", "Abylaev", "Kenzhegulov", "Medeuov",
            "Baigenzhin", "Nogerbekov", "Yesbolov", "Aldabergenov", "Yskakov", "Sabitov", "Auelbekov"
    );

    private DemoIdentitySupport() {
    }

    public static String generateStudentName(int index) {
        int normalizedIndex = Math.max(index, 1) - 1;
        String firstName = STUDENT_FIRST_NAMES.get(normalizedIndex % STUDENT_FIRST_NAMES.size());
        String lastName = STUDENT_LAST_NAMES.get(normalizedIndex % STUDENT_LAST_NAMES.size());
        return firstName + " " + lastName;
    }

    public static String studentEmailFromFullName(String fullName) {
        NameParts parts = splitName(fullName);
        return sanitize(parts.firstName().substring(0, 1)) + "_" + sanitize(parts.lastName()) + "@kbtu.kz";
    }

    public static String teacherEmailFromFullName(String fullName) {
        NameParts parts = splitName(stripTeacherTitle(fullName));
        return sanitize(parts.firstName().substring(0, 1)) + "." + sanitize(parts.lastName()) + "@kbtu.kz";
    }

    private static String stripTeacherTitle(String fullName) {
        return fullName
                .replaceFirst("(?i)^professor\\s+", "")
                .replaceFirst("(?i)^dr\\.\\s+", "")
                .trim();
    }

    private static NameParts splitName(String fullName) {
        String normalized = fullName == null ? "" : fullName.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Name cannot be blank");
        }
        String[] parts = normalized.split("\\s+");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Expected first name and last name: " + fullName);
        }
        return new NameParts(parts[0], parts[parts.length - 1]);
    }

    private static String sanitize(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);
        return NON_ASCII_ALNUM.matcher(normalized).replaceAll("");
    }

    private record NameParts(String firstName, String lastName) {
    }
}
