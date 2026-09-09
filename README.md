Guess Market - Exercise 2 (JavaFX)
=================================

Java 25 | LMSR + Order Book | multi-user accounts

Submitted by: 325556835, 213726094
Repository: https://github.com/Shady-ab/GuessMarket-EX2

How to run
----------
1. JDK 25 must be on PATH (`java -version` should show 25).
2. JavaFX 25 Windows jars must exist in `lib\` (`javafx.base.jar`, `javafx.graphics.jar`, `javafx.controls.jar`).
   If they are missing, run `download-javafx.bat`.
3. From this folder run:

   build.bat
   run.bat

The checker can also run `dist\run.bat` after copying `dist\*.jar` and `dist\lib\`.

Load XML with the file chooser (Exercise-2 schema only). Sample files are in `samples\`.

Main classes
------------
- GuessMarketEngine / GuessMarketEngineImpl: passive engine. XML load, users, events, LMSR, order book.
- MarketEvent: one binary event, either LMSR or order-book.
- User: cash, holdings, market-maker assignment, blocked-if-negative.
- OptionOrderBook / BookOrder: price-time priority book, matching and mint.
- GuessMarketApp: JavaFX UI (the only module with main / System.out-free engine).

Assumptions written for the checker
-----------------------------------
- XML tags accept both the assignment-text names (`commission`, `GM-market-maker`, `initial`) and the diagram typos (`comision`, `GM-mareket-maker`, `inital`).
- Every event must have exactly one market maker. Duplicate MM assignment is rejected.
- User names are unique and case-sensitive.
- Events start as Not started. Only the MM can open/close.
- Opening LMSR pays `b * ln(2)` from the MM to the event account.
- Opening an order-book event pays `initial` from the MM; the MM receives `initial/d` complete pairs of shares and may later sell them.
- Order prices must satisfy `0.01 <= price <= d - 0.01`.
- Matching uses price-time priority. The trade price is the resting order price.
- Mint (if allow-mint=true) happens after same-option matching, when an incoming BUY plus a resting BUY on the other option sum to at least `d`. The resting order pays its full price; the incoming order pays `d - resting`.
- On-purchase commission is paid by the buyer to the MM account.
- On-close commission is taken from winners and paid to the MM. The MM is treated as a regular holder here,
  so commission on the MM's own winning shares is charged and immediately paid back to the MM. This is
  cash-neutral, but it does count toward the "commission collected" figure shown in the UI.
- After close, leftover event-account cash is returned to the MM.
- If a debit makes cash negative, the action is applied and that user is then blocked.
- UI is English, left-to-right, 1-based option numbers, two decimal digits.
- Screen resize uses grow + scroll; the window is resizable.

Bonuses
-------
None implemented in this version.
