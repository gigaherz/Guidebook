package dev.gigaherz.guidebook.guidebook.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record Length(int value, String unit)
{
    private static final Pattern PATTERN = Pattern.compile("^(?<num>\\d+)(?<unit>px|%|)$");

    public float getValue(int parentValue/*, float fontSize*/) // TODO: make context object that can handle more units
    {
        return switch (unit)
        {
            case "px" -> value;
            //case "em" -> value * fontSize;
            case "%" -> value * parentValue / 100f;
            default -> throw new IllegalArgumentException("Unknown length unit " + unit);
        };
    }

    public static Length parse(String s)
    {
        Matcher matcher = PATTERN.matcher(s);
        if (!matcher.matches()) throw new IllegalArgumentException("Invalid length: " + s);
        int num = Integer.parseInt(matcher.group("num"));
        String unit = matcher.group("unit");
        if (unit.isEmpty()) unit = "px";
        return new Length(num, unit);
    }
}
