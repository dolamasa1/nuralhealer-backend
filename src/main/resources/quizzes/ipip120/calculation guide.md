

### IPIP-NEO-120 Technical Calculation Guide (Updated)

This guide provides the scoring logic and item mapping for the 120-item version of the IPIP-NEO.

#### 1. Scoring Logic & Keying

* **Scale:** 1 (Very Inaccurate) to 5 (Very Accurate).
* **Positive (+) Keyed:** Use the raw response (1=1, 5=5).
* **Negative (-) Keyed:** Reverse the score using `6 - Response` (e.g., 1 becomes 5).
* **Missing Data:** Assign a "3" (Neutral) if an item is skipped.

#### 2. Domain & Facet Mapping (The Item ID Table)

Each Domain (Score Range: 24–120) consists of 6 Facets. Each Facet (Score Range: 4–20) is calculated by summing its **4 specific Item IDs**.

| Domain (Domain ID) | Facet Name | Facet ID | Item IDs |
| --- | --- | --- | --- |
| **Neuroticism (N)** | Anxiety | N1 | 1, 31, 61, 91 |
|  | Anger | N2 | 6, 36, 66, 96 |
|  | Depression | N3 | 11, 41, 71, 101 |
|  | Self-Consciousness | N4 | 16, 46, 76, 106 |
|  | Immoderation | N5 | 21, 51, 81, 111 |
|  | Vulnerability | N6 | 26, 56, 86, 116 |
| **Extraversion (E)** | Friendliness | E1 | 2, 32, 62, 92 |
|  | Gregariousness | E2 | 7, 37, 67, 97 |
|  | Assertiveness | E3 | 12, 42, 72, 102 |
|  | Activity Level | E4 | 17, 47, 77, 107 |
|  | Excitement-Seeking | E5 | 22, 52, 82, 112 |
|  | Cheerfulness | E6 | 27, 57, 87, 117 |
| **Openness (O)** | Imagination | O1 | 3, 33, 63, 93 |
|  | Artistic Interests | O2 | 8, 38, 68, 98 |
|  | Emotionality | O3 | 13, 43, 73, 103 |
|  | Adventurousness | O4 | 18, 48, 78, 108 |
|  | Intellect | O5 | 23, 53, 83, 113 |
|  | Liberalism | O6 | 28, 58, 88, 118 |
| **Agreeableness (A)** | Trust | A1 | 4, 34, 64, 94 |
|  | Morality | A2 | 9, 39, 69, 109 |
|  | Altruism | A3 | 14, 44, 74, 104 |
|  | Cooperation | A4 | 19, 49, 79, 109 |
|  | Modesty | A5 | 24, 54, 84, 114 |
|  | Sympathy | A6 | 29, 59, 89, 119 |
| **Conscientiousness (C)** | Self-Efficacy | C1 | 5, 35, 65, 95 |
|  | Orderliness | C2 | 10, 40, 70, 100 |
|  | Dutifulness | C3 | 15, 45, 75, 105 |
|  | Achievement-Striving | C4 | 20, 50, 80, 110 |
|  | Self-Discipline | C5 | 25, 55, 85, 115 |
|  | Cautiousness | C6 | 30, 60, 90, 120 |

#### 3. Interpretation (Arabic Support)

To integrate your Arabic resources into the calculation results:

* **Low Score (24–55):** "Your score is low..." / "درجتك منخفضة...".
* **Average Score (56–88):** "Your score is average..." / "درجتك متوسطة...".
* **High Score (89–120):** "Your score is high..." / "درجتك مرتفعة...".

#### 4. Key Differences for Implementation

1. **Item 9 and 109:** Note that in your `questions.json`, Item 9 and 109 both relate to Morality/Agreeableness and are both **negative (-)** keyed. Ensure your code handles the negative reversal before adding them to the facet sum.
2. **Factor 4:** Reminder—in this version, a high score in **Neuroticism** means high anxiety/distress, not stability.
