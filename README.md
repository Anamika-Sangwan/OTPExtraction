Approach Overview

Regex Layer (High Confidence): Quickly detects common OTP patterns like "The OTP is 123456".

Tokenization + Validation: Splits the message into potential candidates, filtering by digit length and numeric validity.

Scoring Engine:

Context Score: Uses qualified/disqualified keywords to boost or eliminate candidates.

Position Score: Rewards tokens that appear early (OTP is usually near the start).

Global Context Score: Scans nearby tokens for supportive or disqualifying cues.

Candidate Selection: Picks the highest-scoring token as the OTP.

Optimization: Single regex pass and tokenization upfront to minimize complexity.

Result: Achieved 97.87% accuracy on the OneBanc dataset.
