# Contributing to NeuralHealer

Welcome! We are building a secure healthcare platform. Follow these standards to keep the codebase clean and safe.

---

## 🛠️ Development Standards

### 1. The 3-Plane Rule
Every new feature must be classified:
- **Control Plane**: If it's about rules or state (e.g., Billing, Verification).
- **Data Plane**: If it's about history (e.g., Reports, Archives).
- **Real-Time Plane**: If it's about live UX (e.g., Video, Live Status).

### 2. Security First
- Never use `localStorage` for tokens. Use HTTPOnly Cookies.
- Always add `@Transactional` to state-changing service methods.
- Check permissions (`canAccessEngagement`) before any resource access.

---

## 🧪 Testing

Run all tests before submitting a PR:
```bash
mvn test
```

---

## 📝 Commit Messages
We follow conventional commits:
- `feat: ...` for new features.
- `fix: ...` for bug fixes.
- `docs: ...` for documentation changes.
- `refactor: ...` for code changes that neither fix a bug nor add a feature.
