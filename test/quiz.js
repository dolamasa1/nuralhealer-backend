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
        yourProfile: "Your Profile",
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
        yourProfile: "ملفك",
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
        this.config = config; // { type: 'ipip120' | 'ipip50' | 'phq9', baseUrl: string, sessionHeader: string }
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
            this.currentIdx = this.questions.findIndex((_, idx) => !this.answers[this.questions[idx].id]) || 0;
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
        // Compatibility for IPIP-120 header if needed
        if (this.config.type === 'ipip120') headers['X-Quiz-Session-120'] = this.sessionId;

        try {
            const res = await fetch(`${this.config.baseUrl}${path}`, { ...options, headers });

            if (res.status === 401 && !isRetry) {
                console.warn("Unauthorized. This endpoint requires an active session. Retrying...");
                await this.startNewSession();
                return this.api(path, options, true);
            }

            if (!res.ok) {
                const errData = await res.json().catch(() => ({}));
                const errMsg = errData.error || errData.message || "API Fail";
                console.error(`API Error [${res.status}] ${path}:`, errMsg);
                throw new Error(errMsg);
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
            const val = this.config.type === 'phq9' ? i : i + 1; // PHQ-9 is 0-3, IPIP is 1-5
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
            console.warn(`Attempted submit with missing answers: ${answered}/${total}`);
            this.showError(`Please answer all ${total} questions before submitting.`);
            return;
        }

        this.showLoading(this.t().calculating);
        try {
            const data = await this.api(`/submit-quiz?language=${this.lang}`, { method: 'POST' });
            this.hideLoading();
            this.showResults(data.result);
        } catch (e) {
            this.hideLoading();
            this.showError(`Calculation failed: ${e.message}`);
        }
    }

    showResults(result) {
        document.querySelector('.test').classList.add('hidden');
        document.getElementById('results').classList.add('active');

        if (this.config.type === 'ipip120') {
            this.render120Results(result);
        } else if (this.config.type === 'phq9') {
            this.renderPhq9Results(result);
        } else {
            this.render50Results(result);
        }
    }

    render120Results(result) {
        const cont = document.getElementById('resultsList');
        cont.innerHTML = `<div class="radar-container"><canvas id="radarChart"></canvas></div>`;

        const labels = [], scores = [];
        result.scores.forEach(s => {
            labels.push(this.lang === 'ar' ? s.arabicTrait : s.trait);
            scores.push(s.score);

            const card = document.createElement('div');
            card.className = 'domain-card';
            const pct = (s.score / 120) * 100;
            card.innerHTML = `
                <div class="domain-header">
                    <span class="domain-name">${this.lang === 'ar' ? s.arabicTrait : s.trait}</span>
                    <span class="domain-score">${s.score}</span>
                </div>
                <div class="domain-bar"><div class="domain-bar-fill" style="width:${pct}%"></div></div>
                <div class="facets-container">
                    ${s.facets.map(f => `
                        <div class="facet-item">
                            <div class="facet-header">
                                <span>${this.lang === 'ar' ? (f.arabicTrait || f.trait) : f.trait}</span>
                                <span>${f.score} / 20</span>
                            </div>
                            <div class="facet-bar"><div class="facet-bar-fill" style="width:${(f.score / 20) * 100}%"></div></div>
                            <p class="facet-description">${this.lang === 'ar' ? f.arabicDescription : f.description}</p>
                        </div>
                    `).join('')}
                </div>
            `;
            card.onclick = () => card.classList.toggle('expanded');
            cont.appendChild(card);
        });

        this.drawRadar(labels, scores);
    }

    render50Results(result) {
        const cont = document.getElementById('resultsList');
        cont.innerHTML = `
            <div class="narrative">
                <div class="narrative-title">${this.t().yourProfile}</div>
                <div class="narrative-content">
                    ${result.scores.map(s => `
                        <div class="trait-insight">
                            <span class="trait-icon">${this.getIcon(s.trait)}</span>
                            <span class="trait-text"><strong>${this.lang === 'ar' ? s.arabicTrait : s.trait}:</strong> ${this.lang === 'ar' ? s.arabicDescription : s.description}</span>
                        </div>
                    `).join('')}
                </div>
            </div>
        `;
    }

    renderPhq9Results(result) {
        const cont = document.getElementById('resultsList');
        const s = result.scores[0]; // PHQ-9 has one main score

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

    getIcon(trait) {
        const icons = { 'Extraversion': '👥', 'Agreeableness': '🤝', 'Conscientiousness': '📋', 'Neuroticism': '🧘', 'Openness': '🎨' };
        return icons[trait] || '✨';
    }

    drawRadar(labels, data) {
        const ctx = document.getElementById('radarChart').getContext('2d');
        if (window.myRadar) window.myRadar.destroy();
        window.myRadar = new Chart(ctx, {
            type: 'radar',
            data: {
                labels,
                datasets: [{
                    label: this.t().yourResults,
                    data,
                    backgroundColor: 'rgba(155, 89, 182, 0.2)',
                    borderColor: '#9B59B6',
                    pointBackgroundColor: '#8E44AD',
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
            if (document.getElementById('results').classList.contains('active')) this.submit(); // Refresh results for lang
        };
        document.getElementById('prevBtn').onclick = () => { if (this.currentIdx > 0) { this.currentIdx--; this.render(); } };
        document.getElementById('nextBtn').onclick = () => {
            if (this.currentIdx < this.questions.length - 1) { this.currentIdx++; this.render(); }
            else this.submit();
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
        setTimeout(() => { cont.classList.remove('show'); setTimeout(() => cont.remove(), 600); }, 3000);
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
    hideLoading() { this.isLoading = false; document.getElementById('loader')?.remove(); }
    showError(msg) { alert(msg); }

    retake() {
        if (confirm(this.lang === 'ar' ? "هل تريد إعادة الاختبار؟" : "Retake the test?")) {
            localStorage.removeItem(this.sessionKey);
            window.location.reload();
        }
    }
}
