package merits.funskills.pureland.utils;

public class NumberWordConverter {
    public static final String[] units = {
            "", "one", "two", "three", "four", "five", "six", "seven",
            "eight", "nine", "ten", "eleven", "twelve", "thirteen", "fourteen",
            "fifteen", "sixteen", "seventeen", "eighteen", "nineteen"
    };

    public static final String[] tens = {
            "",        // 0
            "",        // 1
            "twenty",  // 2
            "thirty",  // 3
            "forty",   // 4
            "fifty",   // 5
            "sixty",   // 6
            "seventy", // 7
            "eighty",  // 8
            "ninety"   // 9
    };

    public String convert(final int n, final String delimiter) {
        if (n < 0) {
            return "minus " + convert(-n, delimiter);
        }

        if (n < 20) {
            return units[n];
        }

        if (n < 100) {
            return tens[n / 10] + ((n % 10 != 0) ? delimiter : "") + units[n % 10];
        }

        if (n < 1000) {
            return units[n / 100] + delimiter + "hundred" + ((n % 100 != 0) ? delimiter : "") + convert(n % 100, delimiter);
        }

        if (n < 1000000) {
            return convert(n / 1000, delimiter) + delimiter + "thousand" + ((n % 1000 != 0) ? delimiter : "") + convert(n % 1000, delimiter);
        }

        throw new IllegalArgumentException(n + " is too large!");

        /*
        if (n < 1000000000) {
            return convert(n / 1000000, delimiter) + delimiter + "million" + ((n % 1000000 != 0) ? delimiter : "") + convert(n % 1000000, delimiter);
        }

        return convert(n / 1000000000, delimiter) + delimiter + "billion" + ((n % 1000000000 != 0) ? delimiter : "") + convert(n % 1000000000, delimiter);
        */
    }

}
