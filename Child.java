import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.*;

public class Child extends Utility {
    private static final Set<String> POSITIVE_KEYWORDS = Set.of(
            "otp", "code", "pin", "password", "verification", "auth", "login", "security",
            "your", "use", "enter", "is", "expires", "valid", "confirm", "access");

    private static final Set<String> NEGATIVE_KEYWORDS = Set.of(
            "rs", "inr", "amount", "balance", "credited", "debited", "phone", "mobile",
            "call", "year", "born", "date", "customer", "care", "help", "support", "xxxx");

    // Single optimized regex for all high-confidence patterns
    private static final Pattern HIGH_CONFIDENCE_PATTERN = Pattern.compile(
            "(?i)\\b(?:" + "(?:otp|code|pin|password|verification\\s*code)\\s*(?:is|:|-)\\s*|" +
                    "your\\s+(?:otp|verification\\s*code|login\\s*code)\\s*(?:is|:|-)\\s*|" +
                    "(?:use|enter)\\s+(?:otp|code)\\s*|" +
                    "^\\s*|" +
                    "[\\[\\(\"']|" +
                    "xxxx\\d{4}\\s+" +
                    ")(\\d{4,8})\\b");

    @Override
    public String getMasterDataFilePath() {
        return "master_data.csv";
    }

    @Override
    public String extractOtp(String message) {
        if (message == null || message.trim().isEmpty()) {
            return "nan";
        }
        List<ScoredCandidate> candidates = new ArrayList<>();
        message = message.toLowerCase();
        // Regex check
        Matcher highConfidenceMatcher = HIGH_CONFIDENCE_PATTERN.matcher(message);
        if (highConfidenceMatcher.find()) {
            String candidate = highConfidenceMatcher.group(1);
            if (isValidCandidate(candidate)) {
                return candidate;
            }
        }
        // Tokenize and calculate unified Position+Contextual score
        String[] tokens = message.split("\\s+");
        for (int i = 0; i < tokens.length; i++) {
            String cleanToken = tokens[i].replaceAll("[^\\d]", "");
            if (!isValidCandidate(cleanToken)) {
                continue;
            }
            int score = calculateUnifiedScore(tokens, i, cleanToken, message, findCharPosition(message, tokens, i));
            if (score > 0) {
                candidates.add(new ScoredCandidate(cleanToken, score));
            }
        }
        return candidates.stream()
                .max((a, b) -> Integer.compare(a.score, b.score))
                .map(c -> c.otp)
                .orElse("nan");
    }

    private int calculateUnifiedScore(String[] tokens, int tokenIndex, String otp, String lowerMessage,
            int charPosition) {
        int score = 1;
        // Context scoring (predecessor/successor)
        score = analyzeContext(tokens, tokenIndex) + analyzePosition(tokenIndex, charPosition)
                + analyzeGlobalContext(lowerMessage, charPosition, otp.length()); // Removed taking length of common OTP
                                                                                  // into consideration

        return score;
    }

    private int analyzeContext(String[] tokens, int index) {
        int contextScore = 0;
        // Check predecessor
        if (index > 0) {
            String prev = tokens[index - 1].toLowerCase().replaceAll("[^a-z:]", "");
            if (POSITIVE_KEYWORDS.contains(prev)) {
                contextScore += 10;
            }
            if (NEGATIVE_KEYWORDS.contains(prev)) {
                return -1;
            }
        }
        // Check successor
        if (index < tokens.length - 1) {
            String next = tokens[index + 1].toLowerCase().replaceAll("[^a-z]", "");
            if (POSITIVE_KEYWORDS.contains(next)) {
                contextScore += 8;
            }
            if (NEGATIVE_KEYWORDS.contains(next)) {
                return -1;
            }
        }
        return contextScore;
    }

    private int analyzePosition(int tokenIndex, int charPosition) {
        int positionScore = 0;
        if (tokenIndex <= 2)
            positionScore += 3;
        if (charPosition < 20)
            positionScore += 2;
        else if (charPosition < 50) // Checked for 20 first and then for 50, this reduced overhead
            positionScore += 1;
        return positionScore;
    }

    private int analyzeGlobalContext(String lowerMessage, int position, int length) {
        // Quick context window check
        int start = Math.max(0, position - 15);
        int end = Math.min(lowerMessage.length(), position + length + 15);
        String context = lowerMessage.substring(start, end);
        // Positive context
        if (containsAny(context, "otp", "code", "verification", "password", "pin", "auth", "enter")) {
            return 4;
        }
        // Negative context
        if (containsAny(context, "amount", "balance", "rs", "₹", "phone", "year", "customer")) {
            return -1;
        }
        return 0;
    }

    private boolean isValidCandidate(String token) {
        if (token == null || token.isEmpty())
            return false;
        if (token.length() < 4 || token.length() > 8)
            return false;
        return true;
    }

    private int findCharPosition(String message, String[] tokens, int tokenIndex) {
        if (tokenIndex >= tokens.length)
            return 0;
        int position = 0;
        for (int i = 0; i < tokenIndex; i++) {
            position = message.indexOf(tokens[i], position) + tokens[i].length();
        }
        return message.indexOf(tokens[tokenIndex], position);
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword))
                return true;
        }
        return false;
    }

    private static class ScoredCandidate {
        final String otp;
        final int score;

        ScoredCandidate(String otp, int score) {
            this.otp = otp;
            this.score = score;
        }
    }
}