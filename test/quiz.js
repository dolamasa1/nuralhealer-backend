/**
 * NeuralHealer Quiz Engine
 * Unified, non-overengineered logic for IPIP-120, IPIP-50, and PHQ-9
 */

/**
 * NeuralHealer Quiz Engine
 * Unified, non-overengineered logic for IPIP-120, IPIP-50, and PHQ-9
 */

const TR = {
    en: {
        title: "Assessment",
        questionOf: "Question",
        of: "of",
        complete: "complete",
        previous: "Previous",
        next: "Next",
        viewResults: "View Results",
        retake: "Retake Test",
        yourResults: "Your Results",
        yourProfile: "Your Personality Profile",
        loading: "Loading questions...",
        starting: "Starting session...",
        saving: "Saving response...",
        calculating: "Calculating results...",
        error: "Connection error. Retrying...",
        phq9Options: ["Not at all", "Several days", "More than half the days", "Nearly every day"],
        ipipOptions: ["Very Inaccurate", "Moderately Inaccurate", "Neither", "Moderately Accurate", "Very Accurate"]
    },
    ar: {
        title: "تقييم",
        questionOf: "سؤال",
        of: "من",
        complete: "مكتمل",
        previous: "السابق",
        next: "التالي",
        viewResults: "عرض النتائج",
        retake: "إعادة الاختبار",
        yourResults: "نتائجك",
        yourProfile: "ملفك الشخصي",
        loading: "جاري تحميل الأسئلة...",
        starting: "جاري بدء الجلسة...",
        saving: "جاري حفظ الإجابة...",
        calculating: "جاري حساب النتائج...",
        error: "خطأ في الاتصال. جاري المحاولة...",
        phq9Options: ["على الإطلاق", "لعدة أيام", "أكثر من نصف الأيام", "تقريباً كل يوم"],
        ipipOptions: ["غير دقيق جداً", "غير دقيق إلى حد ما", "محايد", "دقيق إلى حد ما", "دقيق جداً"]
    }
};

class QuizEngine {
    constructor(config) {
        this.config = config;
        this.lang = 'en';
        this.currentIdx = 0;
        this.questions = [];
        this.answers = {};
        this.sessionId = null;
        this.sessionKey = `quiz_session_${config.type}`;
        this.isDarkMode = document.documentElement.classList.contains('dark');
        this.isLoading = false;
    }

    async init() {
        this.setupToggles();
        this.showLoading(this.t().starting);

        // Try to recover session from localStorage
        this.sessionId = localStorage.getItem(this.sessionKey);

        try {
            if (!this.sessionId) {
                await this.startNewSession();
            }

            await this.loadQuestions();
            await this.loadProgress();

            // Find first unanswered or stay at 0
            this.currentIdx = this.questions.findIndex((_, idx) => !this.answers[this.questions[idx].id]);
            if (this.currentIdx === -1) this.currentIdx = 0;

            this.hideLoading();
            this.render();
        } catch (e) {
            console.error("Init failed", e);
            this.showError(this.t().error);
        }
    }

    t() { return TR[this.lang]; }

    async startNewSession() {
        const res = await fetch(`${this.config.baseUrl}/start`, { method: 'POST' });
        const data = await res.json();
        this.sessionId = data.sessionId;
        localStorage.setItem(this.sessionKey, this.sessionId);
    }

    async api(path, options = {}, isRetry = false) {
        const headers = {
            'Content-Type': 'application/json',
            'X-Quiz-Session': this.sessionId
        };
        if (this.config.type === 'ipip120') headers['X-Quiz-Session-120'] = this.sessionId;

        try {
            const res = await fetch(`${this.config.baseUrl}${path}`, { ...options, headers });

            if (res.status === 401 && !isRetry) {
                console.warn("Unauthorized. Retrying...");
                await this.startNewSession();
                return this.api(path, options, true);
            }

            if (!res.ok) {
                const errData = await res.json().catch(() => ({}));
                console.error(`API Error [${res.status}] ${path}:`, errData);
                throw errData; // Throw the whole object
            }
            return await res.json();
        } catch (e) {
            console.error("Fetch/API Error:", e);
            throw e;
        }
    }

    async loadQuestions() {
        this.questions = await this.api('/questions');
    }

    async loadProgress() {
        const data = await this.api('/responses');
        if (data?.responses) {
            data.responses.forEach(r => {
                this.answers[r.questionId] = r.score;
            });
        }
    }

    render() {
        const q = this.questions[this.currentIdx];
        if (!q) return;

        // Progress
        const count = Object.keys(this.answers).length;
        const total = this.questions.length;
        const pct = (count / total) * 100;

        document.getElementById('progressFill').style.width = `${pct}%`;
        document.getElementById('progressText').innerText =
            `${this.t().questionOf} ${this.currentIdx + 1} ${this.t().of} ${total} (${Math.round(pct)}% ${this.t().complete})`;

        if (this.lang === 'ar') {
            document.getElementById('questionText').innerText = q.arabic_text || q.arabicText || q.text || "No Question Text";
        } else {
            document.getElementById('questionText').innerText = q.text || q.text_en || "No Question Text";
        }

        // Options
        const optCont = document.getElementById('options');
        optCont.innerHTML = '';
        const options = this.config.type === 'phq9' ? this.t().phq9Options : this.t().ipipOptions;

        options.forEach((label, i) => {
            const val = this.config.type === 'phq9' ? i : i + 1;
            const div = document.createElement('div');
            div.className = `option ${this.answers[q.id] === val ? 'selected' : ''}`;
            div.innerHTML = `<span class="option-value">${val}</span><span>${label}</span>`;
            div.onclick = () => this.select(val);
            optCont.appendChild(div);
        });

        // Banner (PHQ-9)
        const banner = document.getElementById('timeframeBanner');
        if (banner) banner.classList.toggle('active', this.config.type === 'phq9');

        // Navigation
        document.getElementById('prevBtn').disabled = this.currentIdx === 0;
        document.getElementById('nextBtn').innerText = this.currentIdx === total - 1 ? this.t().viewResults : this.t().next;

        // Milestone logic (IPIP-50)
        if (this.config.type === 'ipip50') this.checkMilestone(pct);
    }

    async select(val) {
        if (this.isLoading) return;
        const q = this.questions[this.currentIdx];
        this.answers[q.id] = val;

        this.render(); // Instant UI update

        try {
            await this.api('/submit-question', {
                method: 'POST',
                body: JSON.stringify({ questionId: q.id, score: val })
            });

            // Auto advance
            setTimeout(() => {
                if (this.currentIdx < this.questions.length - 1) {
                    this.currentIdx++;
                    this.render();
                } else if (Object.keys(this.answers).length === this.questions.length) {
                    this.submit();
                }
            }, 300);
        } catch (e) {
            this.showError(this.t().error);
        }
    }

    async submit() {
        const total = this.questions.length;
        const answered = Object.keys(this.answers).length;

        if (answered < total) {
            const missingIds = [];
            for (let i = 0; i < total; i++) {
                const qId = this.questions[i].id;
                if (!this.answers[qId]) {
                    missingIds.push(i + 1); // Use humans-readable index (1-based)
                }
            }
            const list = missingIds.join(', ');
            console.warn(`Attempted submit with missing answers: ${answered}/${total}. Missing: ${list}`);

            const msg = this.lang === 'ar'
                ? `يرجى الإجابة على جميع الأسئلة الـ ${total} قبل الإرسال. الأسئلة المفقودة: ${list}`
                : `Please answer all ${total} questions before submitting. Missing: ${list}`;
            this.showError(msg);
            return;
        }

        this.showLoading(this.t().calculating);
        try {
            const data = await this.api(`/submit-quiz?language=${this.lang}`, { method: 'POST' });
            this.hideLoading();
            this.showResults(data.result);
        } catch (e) {
            this.hideLoading();
            if (e.missingQuestions && e.missingQuestions.length > 0) {
                const list = e.missingQuestions.join(', ');
                this.showError(`${this.lang === 'ar' ? 'يرجى الإجابة على الأسئلة التالية:' : 'Please check these questions:'} ${list}`);
            } else {
                this.showError(`Calculation failed: ${e.error || e.message || 'Unknown error'}`);
            }
        }
    }

    showResults(result) {
        document.querySelector('.test').classList.add('hidden');
        document.getElementById('results').classList.add('active');

        // Set results title
        const titleEl = document.getElementById('resultsTitle');
        if (titleEl) {
            titleEl.innerText = this.lang === 'ar' ? 'نتائج ملفك الشخصي' : 'Your Personality Results';
        }

        if (this.config.type === 'ipip120') {
            this.render120Results(result);
        } else if (this.config.type === 'phq9') {
            this.renderPhq9Results(result);
        } else {
            this.render50Results(result);
        }
    }

    // REMOVE THE OLD render50Results and render120Results METHODS AND KEEP THESE ONES:

    render50Results(result) {
        const cont = document.getElementById('resultsList');
        cont.innerHTML = '';

        // Standard Scientific Results Section
        const resultsSection = document.createElement('div');
        resultsSection.className = 'results-section';
        resultsSection.innerHTML = `
            <div class="section-header">
                <div class="section-title">${this.t().yourProfile}</div>
                <div class="scientific-badge">
                    <span>🔬</span>
                    <span>${this.lang === 'ar' ? 'الدكتور جون جونسون' : 'Dr. John Johnson'}</span>
                </div>
            </div>
        `;

        const narrative = document.createElement('div');
        narrative.className = 'narrative';

        result.scores.forEach(s => {
            // Calculate percentage (assuming max score is 50 for each trait)
            const maxScore = 50;
            const percentage = Math.round((s.score / maxScore) * 100);

            const insight = document.createElement('div');
            insight.className = 'trait-insight';
            insight.innerHTML = `
                <div class="trait-icon">${this.getIcon(s.trait)}</div>
                <div class="trait-content">
                    <div class="trait-header-row">
                        <div class="trait-name">${this.lang === 'ar' ? s.arabicTrait : s.trait}</div>
                        <div class="trait-percentage">${percentage}%</div>
                    </div>
                    <div class="trait-bar-container">
                        <div class="trait-bar" style="width: 0%" data-width="${percentage}"></div>
                    </div>
                    <div class="trait-description">${this.lang === 'ar' ? s.arabicDescription : s.description}</div>
                </div>
            `;
            narrative.appendChild(insight);
        });

        resultsSection.appendChild(narrative);
        cont.appendChild(resultsSection);

        // AI Chat Redirect Section
        this.addAIChatRedirect(cont);

        // Action Buttons
        this.addActionButtons(cont);
    }

    render120Results(result) {
        const cont = document.getElementById('resultsList');
        cont.innerHTML = '';

        // Standard Scientific Results Section
        const resultsSection = document.createElement('div');
        resultsSection.className = 'results-section';
        resultsSection.innerHTML = `
            <div class="section-header">
                <div class="section-title">${this.t().yourProfile}</div>
                <div class="scientific-badge">
                    <span>🔬</span>
                    <span>${this.lang === 'ar' ? 'الدكتور جون جونسون' : 'Dr. John Johnson'}</span>
                </div>
            </div>
        `;

        const narrative = document.createElement('div');
        narrative.className = 'narrative';

        result.scores.forEach(s => {
            // Calculate percentage (max score is 120 for each domain)
            const maxScore = 120;
            const percentage = Math.round((s.score / maxScore) * 100);

            const insight = document.createElement('div');
            insight.className = 'trait-insight';

            // Create facets HTML if they exist
            let facetsHTML = '';
            if (s.facets && s.facets.length > 0) {
                facetsHTML = `
                    <div class="facets-container" style="margin-top: 20px; padding-top: 20px; border-top: 1px dashed var(--border);">
                        ${s.facets.map(f => {
                    const facetPercentage = Math.round((f.score / 20) * 100);
                    return `
                                <div class="facet-item">
                                    <div class="facet-header">
                                        <span>${this.lang === 'ar' ? (f.arabicTrait || f.trait) : f.trait}</span>
                                        <span>${f.score} / 20</span>
                                    </div>
                                    <div class="facet-bar-container">
                                        <div class="facet-bar" style="width: 0%" data-width="${facetPercentage}"></div>
                                    </div>
                                    <div class="facet-description">${this.lang === 'ar' ? f.arabicDescription : f.description}</div>
                                </div>
                            `;
                }).join('')}
                    </div>
                `;
            }

            insight.innerHTML = `
                <div class="trait-icon">${this.getIcon(s.trait)}</div>
                <div class="trait-content">
                    <div class="trait-header-row">
                        <div class="trait-name">${this.lang === 'ar' ? s.arabicTrait : s.trait}</div>
                        <div class="trait-percentage">${percentage}%</div>
                    </div>
                    <div class="trait-bar-container">
                        <div class="trait-bar" style="width: 0%" data-width="${percentage}"></div>
                    </div>
                    <div class="trait-description">${this.lang === 'ar' ? s.arabicDescription : s.description}</div>
                    ${facetsHTML}
                </div>
            `;
            narrative.appendChild(insight);
        });

        resultsSection.appendChild(narrative);
        cont.appendChild(resultsSection);

        // AI Chat Redirect Section
        this.addAIChatRedirect(cont);

        // Action Buttons
        this.addActionButtons(cont);
    }

    renderPhq9Results(result) {
        const cont = document.getElementById('resultsList');
        const s = result.scores[0];

        const sevClass = s.score <= 4 ? 'sev-none' : s.score <= 9 ? 'sev-mild' : s.score <= 14 ? 'sev-moderate' : 'sev-high';

        cont.innerHTML = `
            <div class="results-header" style="text-align: center; margin-bottom: 30px;">
                <div style="font-size: 48px; font-weight: 800; color: var(--primary);">${s.score}</div>
                <div class="severity-label ${sevClass}">${this.lang === 'ar' ? s.arabicLevel : s.level}</div>
            </div>
            ${s.hasCriticalAlert ? `
                <div class="critical-alert">
                    <div class="critical-alert-header"><span>🆘</span> ${this.lang === 'ar' ? "تنبيه سلامة" : "Safety Alert"}</div>
                    <p>${this.lang === 'ar' ? s.alertMessageAr : s.alertMessageEn}</p>
                </div>
            ` : ''}
            <div class="narrative">
                <div class="narrative-title">${this.lang === 'ar' ? "التوصية" : "Recommendation"}</div>
                <p style="text-align: center; color: var(--text-secondary);">${this.lang === 'ar' ? s.arabicDescription : s.description}</p>
            </div>
        `;
    }

    // Helper methods
    addAIChatRedirect(container) {
        const aiSection = document.createElement('div');
        aiSection.className = 'ai-chat-redirect';
        aiSection.innerHTML = `
            <div class="redirect-content">
                <div class="redirect-icon">✨</div>
                <div class="redirect-text">
                    <div class="redirect-title">${this.lang === 'ar' ? 'افتح رؤى أعمق' : 'Unlock Deeper Insights'}</div>
                    <div class="redirect-subtitle">${this.lang === 'ar' ? 'ناقش ملفك الشخصي مع مساعدنا الذكي' : 'Discuss your personality profile with our AI consultant'}</div>
                </div>
                <button class="redirect-btn" onclick="quiz.openAIChat()">
                    <span>${this.lang === 'ar' ? 'ابدأ الاستشارة' : 'Begin Consultation'}</span>
                    <span class="arrow">${this.lang === 'ar' ? '←' : '→'}</span>
                </button>
            </div>
        `;
        container.appendChild(aiSection);
    }

    addActionButtons(container) {
        const actionButtons = document.createElement('div');
        actionButtons.className = 'action-buttons';
        actionButtons.innerHTML = `
            <button class="action-btn btn-download" onclick="quiz.downloadReport()">
                <span>📥</span>
                <span>${this.lang === 'ar' ? 'تحميل التقرير' : 'Download Report'}</span>
            </button>
            <button class="action-btn btn-retake" onclick="quiz.retake()">
                <span>🔄</span>
                <span>${this.t().retake}</span>
            </button>
        `;
        container.appendChild(actionButtons);
    }

    getIcon(trait) {
        const icons = {
            'Extraversion': '👥',
            'Agreeableness': '🤝',
            'Conscientiousness': '📋',
            'Neuroticism': '🧘',
            'Openness': '🎨',
            'Openness to Experience': '🎨',
            'Emotional Stability': '🧘'
        };
        return icons[trait] || '✨';
    }

    openAIChat() {
        const url = this.lang === 'ar'
            ? `/ar/chat?context=personality_${this.config.type}`
            : `/en/chat?context=personality_${this.config.type}`;

        if (this.config.type === 'ipip120' || this.config.type === 'ipip50') {
            const personalityData = {
                type: this.config.type,
                scores: this.questions.map(q => ({
                    question: q.text,
                    answer: this.answers[q.id]
                }))
            };

            sessionStorage.setItem('personalityData', JSON.stringify(personalityData));
            window.open(url, '_blank');
        }
    }

    downloadReport() {
        // Create a simple text report
        const report = `
PERSONALITY ASSESSMENT REPORT
${this.config.type.toUpperCase()} Results
Generated: ${new Date().toLocaleDateString()}
Language: ${this.lang === 'ar' ? 'Arabic' : 'English'}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

YOUR PERSONALITY PROFILE

${this.questions.map((q, i) => {
            const answer = this.answers[q.id];
            const answerText = this.config.type === 'phq9'
                ? this.t().phq9Options[answer]
                : this.t().ipipOptions[answer - 1];
            return `Q${i + 1}: ${q.text}\nA: ${answerText}\n`;
        }).join('\n')}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Assessment based on ${this.config.type.toUpperCase()}
NeuralHealer Personality Insights
        `.trim();

        // Create blob and download
        const blob = new Blob([report], { type: 'text/plain' });
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `personality-report-${this.config.type}-${Date.now()}.txt`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);

        // Visual feedback
        const btn = document.querySelector('.btn-download');
        if (btn) {
            const originalContent = btn.innerHTML;
            btn.innerHTML = `<span>✓</span><span>${this.lang === 'ar' ? 'تم التنزيل!' : 'Downloaded!'}</span>`;
            btn.style.background = 'linear-gradient(135deg, #27AE60, #229954)';
            setTimeout(() => {
                btn.innerHTML = originalContent;
                btn.style.background = '';
            }, 2000);
        }
    }

    retake() {
        if (confirm(this.lang === 'ar' ? "هل تريد إعادة الاختبار؟" : "Are you sure you want to retake the test?")) {
            localStorage.removeItem(this.sessionKey);
            window.location.reload();
        }
    }

    drawRadar(labels, data) {
        const ctx = document.getElementById('radarChart')?.getContext('2d');
        if (!ctx) return;

        if (window.myRadar) window.myRadar.destroy();
        window.myRadar = new Chart(ctx, {
            type: 'radar',
            data: {
                labels,
                datasets: [{
                    label: this.t().yourResults,
                    data,
                    backgroundColor: 'rgba(107, 70, 193, 0.2)',
                    borderColor: '#6B46C1',
                    pointBackgroundColor: '#553C9A',
                    fill: true
                }]
            },
            options: { scales: { r: { min: 24, max: 120, ticks: { display: false } } }, responsive: true, maintainAspectRatio: false }
        });
    }

    // Helpers
    setupToggles() {
        document.getElementById('themeToggle').onclick = () => {
            this.isDarkMode = !this.isDarkMode;
            document.documentElement.classList.toggle('dark', this.isDarkMode);
        };
        document.getElementById('langToggle').onclick = () => {
            this.lang = this.lang === 'en' ? 'ar' : 'en';
            document.documentElement.setAttribute('dir', this.lang === 'ar' ? 'rtl' : 'ltr');
            document.getElementById('langText').innerText = this.lang === 'en' ? 'العربية' : 'English';
            this.render();
            if (document.getElementById('results').classList.contains('active')) {
                this.submit(); // Refresh results for lang
            }
        };
        document.getElementById('prevBtn').onclick = () => {
            if (this.currentIdx > 0) {
                this.currentIdx--;
                this.render();
            }
        };
        document.getElementById('nextBtn').onclick = () => {
            if (this.currentIdx < this.questions.length - 1) {
                this.currentIdx++;
                this.render();
            } else {
                this.submit();
            }
        };
    }

    checkMilestone(pct) {
        const m = [20, 50, 80, 100];
        const val = m.find(v => Math.abs(pct - v) < 2);
        if (val && !this[`m${val}`]) {
            this[`m${val}`] = true;
            this.showCharacter(val);
        }
    }

    showCharacter(milestone) {
        const cont = document.createElement('div');
        cont.className = 'motivation-character-container';
        const msg = {
            20: { en: "Great start!", ar: "بداية رائعة!" },
            50: { en: "Halfway there!", ar: "وصلت للمنتصف!" },
            80: { en: "Almost done!", ar: "أوشكت على الانتهاء!" },
            100: { en: "Complete!", ar: "اكتمل الاختبار!" }
        }[milestone];

        cont.innerHTML = `
            <div class="speech-bubble">${this.lang === 'ar' ? msg.ar : msg.en}</div>
            <div class="character-body"></div>
        `;
        document.body.appendChild(cont);
        setTimeout(() => cont.classList.add('show'), 100);
        setTimeout(() => {
            cont.classList.remove('show');
            setTimeout(() => cont.remove(), 600);
        }, 3000);
    }

    showLoading(msg) {
        this.isLoading = true;
        let l = document.getElementById('loader');
        if (!l) {
            l = document.createElement('div');
            l.id = 'loader';
            l.style.cssText = "position:fixed;inset:0;background:rgba(0,0,0,0.8);display:flex;align-items:center;justify-content:center;color:white;z-index:1000;";
            document.body.appendChild(l);
        }
        l.innerText = msg;
    }

    hideLoading() {
        this.isLoading = false;
        document.getElementById('loader')?.remove();
    }

    showError(msg) {
        alert(msg);
    }
}
