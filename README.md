# Gold Demo Trader — Android MVP

A simple Android demo trading app for XAU/USD with simulated prices, virtual balance, BUY/SELL orders, optional stop-loss and take-profit, open positions, and local history.

## Build APK
The GitHub Actions workflow builds a debug APK and uploads it as an artifact. Download the artifact ZIP from the completed workflow run, extract it on your Android phone, then install the APK.

## Limitations
- Prices are randomly simulated, not live market quotes.
- No customer accounts, online backend, cloud synchronization, or persistent trade history yet.
- The chart is a basic native line chart, not TradingView.
- No real trading, deposits, withdrawals, or real money.
- Illustrative P/L convention: 1 lot = 100 troy ounces; actual broker specifications may differ.

Minimum Android version: Android 8.0 (API 26). Application ID: `com.golddemotrader.app`.