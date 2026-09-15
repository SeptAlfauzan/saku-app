# Saku

Saku is a personal finance tracker for Android and iOS that helps you stay on top of where your money goes — automatically. Instead of manually recording every purchase, Saku listens to your banking and e-wallet notifications, turns them into transactions, categorizes them, and shows you a clear monthly picture of your income and spending.

## Features

### Automatic transaction tracking

Saku reads payment notifications from your banking and e-wallet apps and converts them into structured transactions — amount, merchant, and whether it's money in or money out. Supported out of the box:

- BCA
- GoPay (Gojek)
- OVO
- DANA

No integration is a perfect fit for your app list? Saku also includes a generic parser that understands common payment notification patterns, so other apps often work too. You can enable or disable tracking per app from Settings, and fine-tune the keywords the parser looks for — so it learns the wording your apps actually use.

### Auto-categorization

Each transaction is automatically classified into a category (Food, Transport, Shopping, Bills, Entertainment, Health, Education, Insurance, Salary, Freelance, Cashback, Interest, and more), so your spending is organized without any extra work.

### Smart review queue

Not every notification is crystal clear. Saku scores every parsed transaction by confidence — full details like amount, type, merchant, and category score high. Transactions above the confidence threshold are confirmed automatically; anything uncertain lands in a review queue where you can confirm, edit, or mark it within seconds. Duplicate notifications are detected and skipped, so the same transaction never gets counted twice.

### Receipt scanning

Take a photo of a receipt and Saku reads its value, then prefills a new transaction for you to save. No typing amounts by hand.

### Manual entry

Track cash payments and anything notifications miss. Add income, expense, or transfer transactions by hand, and edit any transaction at any time — including fixing category, merchant, or amount.

### Dashboard

One glance gives you the essentials:

- Income, expenses, and balance for the current month
- Your recent transactions
- A daily spending (burn rate) view
- Outstanding items waiting in the review queue

### Data you control

Your transactions are yours. Export your data to CSV whenever you need it — filter by date range (all time, this month, this year), transaction type, or category. Import CSV files back in, too: you get a preview first, and if anything looks wrong, undo and try again.

## Supported languages

Payment notification parsing understands Indonesian-language transaction messages (Indonesian payment apps), and the UI is built around Indonesian financial terminology.

## Development

Saku is a Kotlin Multiplatform project sharing business logic and UI between Android and iOS.

### Project structure

- [`/shared`](./shared/src) — shared Kotlin code: data layer (database, repositories, remote OCR), notification parsing engine, domain logic, and Compose UI
- [`/androidApp`](./androidApp) — Android application entry point
- [`/iosApp`](./iosApp) — iOS application entry point

### Running the apps

- Android: `./gradlew :androidApp:assembleDebug`
- iOS: open the [`/iosApp`](./iosApp) directory in Xcode and run it from there

### Running tests

- Android tests: `./gradlew :shared:testAndroidHostTest`
- iOS tests: `./gradlew :shared:iosSimulatorArm64Test`

### Writing GitHub release notes

Each release note includes a short summary in both Indonesian and English, prefixed with `[ID]` and `[EN]`. Write the Indonesian section first, then the English translation. Keep both to a few sentences and use plain language that users can understand.

Template:

```
[ID]
Ringkasan perubahan utama untuk pengguna dalam Bahasa Indonesia.

[EN]
Summary of the main changes for users in English.
```

Example:

```
[ID]
Perbaikan bug login dan peningkatan performa kamera.

[EN]
Fixed login bug and improved camera performance.
```

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html).