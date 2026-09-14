package dk.serik.recipes.testutil;

public enum DateTimeFormat {
    ISO_FULL_FORMAT("yyyy-MM-dd'T'HH:mm:ss");

    private String stringFormat;

    private DateTimeFormat(String stringFormat) {
        this.stringFormat = stringFormat;
    }

    public String getStringFormat() {
        return stringFormat;
    }
}
