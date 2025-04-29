package com.ammonium.adminshop.money;

import com.ammonium.adminshop.setup.Config;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;

import javax.annotation.Nullable;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Locale;

public class MoneyFormat {
    public static final double FORMAT_START = 1000000;
    public static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat();

    public enum FormatType {
        FULL,
        SHORT,
        RAW
    }

    private MoneyFormat() {
    }

    // Format value based on config
    public static String cfgformat(long value) {
//        AdminShop.LOGGER.debug("Formatting {} with config", value);
        boolean displayFormat = (Config.displayFormat.get() != null) ? Config.displayFormat.get() : true;
        FormatType formattype = displayFormat ? FormatType.SHORT : FormatType.FULL;
        return format(value, formattype, FormatType.RAW);
    }

    // Format is always X irrespective of shift
    public static String forcedFormat(long value, FormatType forced) {
        return format(value, forced, forced);
    }

    // If noShift not specified -> noShift = RAW
    public static String format(long value, FormatType noShift) {
        return format(value, noShift, FormatType.RAW);
    }

    public static String format(long value, FormatType noShift, FormatType onShift) {
//        AdminShop.LOGGER.debug("Formatting {} with noShift: {} and onShift: {}", value, noShift, onShift);
        int decimalOffset = Integer.parseInt(I18n.get("gui.money_decimal_offset"));

        double realValue = value;
        if (decimalOffset > 0 && !Config.ignoreDecimalOffset.get()) {
            realValue /= Math.pow(10, decimalOffset);

            // Round to the specified number of decimal places
            double scale = Math.pow(10, decimalOffset);
            realValue = Math.round(realValue * scale) / scale;
        }

        NumberName name = NumberName.findName(realValue);
        String moneyStr;
        if (name == null || Math.abs(realValue) < FORMAT_START) {
            DECIMAL_FORMAT.setMinimumFractionDigits(decimalOffset);
            DECIMAL_FORMAT.setMaximumFractionDigits(decimalOffset);
            moneyStr = DECIMAL_FORMAT.format(realValue);
        } else {
            moneyStr = Screen.hasShiftDown() ?
                    doFormat(realValue, name, onShift, decimalOffset) :
                    doFormat(realValue, name, noShift, decimalOffset);
        }

        return I18n.get("gui.money_format", moneyStr);
    }

    public static String doFormat(long value, NumberName name, FormatType formattype) {
        if (formattype == FormatType.SHORT) {
            return getShort(value) + name.getName(true);
        }
        else if (formattype == FormatType.FULL) {
            return getShort(value) + String.format(" %s", name.getName(false));
        }
        else {
            return DECIMAL_FORMAT.format(value);
        }
    }

    public static String doFormat(double value, NumberName name, FormatType formattype, int decimalOffset) {
        if (formattype == FormatType.SHORT) {
            return getShort(value, decimalOffset) + name.getName(true);
        }
        else if (formattype == FormatType.FULL) {
            return getShort(value, decimalOffset) + String.format(" %s", name.getName(false));
        }
        else {
            DECIMAL_FORMAT.setMinimumFractionDigits(decimalOffset);
            DECIMAL_FORMAT.setMaximumFractionDigits(decimalOffset);
            return DECIMAL_FORMAT.format(value);
        }
    }

    public static String getShort(long value) {
//        AdminShop.LOGGER.debug("Getting short for long {}", value);
        boolean isNegative = value < 0;
        String str = String.valueOf(Math.abs(value));
        int len = str.length();

        if (len < 4) {
            return isNegative ? "-" + str : str;
        }

        String sig = str.substring(0, len % 3 == 0 ? 3 : len % 3);
        String dec = str.substring(sig.length(), Math.min(sig.length() + 2, len));

        String result = String.format("%s.%s", sig, dec);
        return isNegative ? "-" + result : result;
    }

    public static String getShort(double value, int decimalOffset) {
//        AdminShop.LOGGER.debug("Getting short for double {}", value);
        boolean isNegative = value < 0;
//        String str = String.valueOf(Math.abs(value));
        // Use DecimalFormat to format the number without scientific notation
        DECIMAL_FORMAT.setMinimumFractionDigits(decimalOffset);
        DECIMAL_FORMAT.setMaximumFractionDigits(decimalOffset);
        String str = DECIMAL_FORMAT.format(Math.abs(value));
        int len = str.length();

        if (len <= Math.max(5, 2+decimalOffset)) {
//            AdminShop.LOGGER.debug("Length is less than 5, returning {}", str);
            return isNegative ? "-" + str : str;
        }

//        AdminShop.LOGGER.debug("Length is greater than 5, returning {}", getShort((long) value));
        return getShort((long) value);
    }



    public enum NumberName {
        MILLION(1e6, "M"),
        BILLION(1e9, "B"),
        TRILLION(1e12, "T"),
        QUADRILLION(1e15, "Qa"),
        QUINTILLION(1e18, "Qi"),
        SEXTILLION(1e21, "Sx"),
        SEPTILLION(1e24, "Sp"),
        OCTILLION(1e27, "O"),
        NONILLION(1e30, "N"),
        DECILLION(1e33, "D"),
        UNDECILLION(1e36, "U"),
        DUODECILLION(1e39, "Du"),
        TREDECILLION(1e42, "Tr"),
        QUATTUORDECILLION(1e45, "Qt"),
        QUINDECILLION(1e48, "Qd"),
        SEXDECILLION(1e51, "Sd"),
        SEPTENDECILLION( 1e54, "St"),
        OCTODECILLION(1e57, "Oc"),
        NOVEMDECILLION(1e60, "No");

        public static final NumberName[] VALUES = values();

        private final double value;
        private final String shortName;
        NumberName(double val, String shortName) {
            this.value  = val;
            this.shortName = shortName;
        }

        public String getName() {
            return getName(false);
        }

        public String getName(boolean getShort) {
            return getShort ? shortName : (name().charAt(0) + name().toLowerCase(Locale.US).substring(1)).trim();
        }

        public double getValue() {return value;}

        static @Nullable NumberName findName(long value) {
            return Arrays.stream(VALUES).filter(v -> Math.abs(value) >= v.getValue()).reduce((first, second) -> second).orElse(null);
        }

        static @Nullable NumberName findName(double value) {
            return Arrays.stream(VALUES).filter(v -> Math.abs(value) >= v.getValue()).reduce((first, second) -> second).orElse(null);
        }

    }
}