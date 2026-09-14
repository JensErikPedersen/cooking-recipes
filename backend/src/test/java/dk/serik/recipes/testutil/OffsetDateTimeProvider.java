package dk.serik.recipes.testutil;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class OffsetDateTimeProvider {

    public final static ZoneId COPENHAGEN_ZONE_ID = ZoneId.of("Europe/Copenhagen");
    public final static String isoFullFormat = "yyyy-MM-dd'T'HH:mm:ss";
//    public static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-ddTHH:mm", Locale.forLanguageTag("da-DK"));


    public static OffsetDateTime provideIsoFullFormat(String timestamp) {
        DateTimeFormatter parser = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        LocalDateTime dt = java.time.LocalDateTime.parse(timestamp, parser);
        ZonedDateTime zdt = ZonedDateTime.of(dt, COPENHAGEN_ZONE_ID);
        return OffsetDateTime.from(zdt);
//        OffsetDateTime offsetDateTime = ZonedDateTime.parse(timestamp, formatter).toOffsetDateTime();
//        return offsetDateTime;
    }

    public static OffsetDateTime provide(String timestamp, DateTimeFormat format) {
        DateTimeFormatter parser = DateTimeFormatter.ofPattern(format.getStringFormat());
        LocalDateTime dt = java.time.LocalDateTime.parse(timestamp, parser);
        ZonedDateTime zdt = ZonedDateTime.of(dt, COPENHAGEN_ZONE_ID);
        return OffsetDateTime.from(zdt);
    }
}
