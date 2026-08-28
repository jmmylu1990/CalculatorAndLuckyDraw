# 計算機與電商抽獎系統

本專案是一個基於 Spring Boot 與 Thymeleaf 的練習網頁應用，包含「計算機（支援 Undo/Redo）」與「電商抽獎轉盤（支援高併發安全）」兩個核心功能。

---

## 功能模組說明

### 1. 復原 (Undo) 與重做 (Redo) 計算機
* **命令模式（Command Pattern）**：將每個運算（加減乘除）封裝成獨立的 Command 物件。執行時自動記錄運算前的數值（`prevValue`），以便在 Undo/Redo 歷史中來回切換。
* **歷史紀錄**：記錄每一次的運算過程，並將運算步驟呈現在畫面上。
* **優化操作體驗**：當點選「復原」時，系統會自動在畫面上亮起該次被撤銷的運算子，使用者可以直接輸入新數值並點擊 `=`，便能直接修正上一步的計算。

### 2. 電商轉盤抽獎與高併發防護
* **獎品與機率配置**：
  - iPhone 15 Pro：數量 3，機率 1%
  - iPad Air：數量 10，機率 5%
  - AirPods Pro：數量 20，機率 14%
  - 銘謝惠顧：機率 80%
* **併發安全防超抽**：為了防止 100 人同時抽獎時發生超賣（即剩餘庫存變為負數），我們使用 `ReentrantLock` 鎖定核心抽獎區塊。
  * **為什麼使用 `ReentrantLock`？**
    * **明確的鎖範圍控制**：與 `synchronized` 關鍵字相比，`ReentrantLock` 允許我們在更靈活的區塊進行手動加鎖與解鎖（`lock()` 與 `unlock()`），並能搭配 `try/finally` 確保異常發生時鎖一定會被釋放。
    * **防止死鎖與重入性**：它支援同一個執行緒重複進入同一個鎖控制的區塊（可重入），避免在多重抽獎或內部相互調用時產生死鎖。
    * **單機高效防護**：由於獎品庫存存放在 JVM 記憶體中，使用 JVM 內建的鎖（ReentrantLock）是單機環境下效能最高、最輕量的併發安全防護手段，不需額外架設外部 Redis 等服務即可保證數據一致性。
* **高併發模擬測試**：
  * 後端使用 `ExecutorService` 建立擁有 100 個執行緒的執行緒池。
  * 配合 `CountDownLatch`（計數器門閂）讓 100 個抽獎請求在一瞬間「同時」對後端服務發起呼叫，模擬真實高流量併發情境，藉此驗證 `ReentrantLock` 的執行緒安全效果。
* **Session 防重複提交**：使用 HTTP Session 鎖定機制。當使用者的抽獎請求正在處理時，若使用者快速重複點擊按鈕，後續請求將被直接攔截並拒絕，防止短時間內重複扣減。
* **多次抽獎**：支援單次、連續 5 次、連續 10 次抽獎。

---

### [進階擴展] 如果改用 Redis 進行分散式設計

當系統從「單機」擴展到「多實例（微服務）集群」時，JVM 內建的 `ReentrantLock` 與本地記憶體庫存將不再適用（各節點庫存無法即時同步，且本地鎖無法跨機器防護）。此時應將架構重構為 **Redis 核心控制**：

#### 1. 分散式庫存管理
* **作法**：將獎品的庫存數量移出 JVM 記憶體，直接存在 Redis 的鍵值（如 `lottery:prize:prize_a` -> `3`）中。
* **無鎖原子扣減 (推薦)**：抽獎時不需使用顯式鎖，直接利用 Redis 單執行緒運作與原子操作的特性：
  * **Lua 腳本**：使用 Lua 腳本在 Redis 端一口氣執行「檢查庫存是否大於 0」與「庫存扣減（`DECR`）」，因 Lua 腳本在 Redis 中執行具備排他性，能絕對防止並行超賣，且效能極高。
  * **Redis `DECR`**：也可直接進行 `DECR` 扣減，若扣減後值為負數，則立即加回 (`INCR`) 並判定沒中獎。

#### 2. Redis 分散式鎖 (Distributed Lock)
若業務邏輯較複雜，需要在多步操作間維持排他性，可引入 Redis 分散式鎖保護抽獎 critical section：
* **加鎖**：利用 `SET key token NX PX 5000`（在 Spring 使用 `setIfAbsent`）進行互斥加鎖，並設定租期過期時間（如 5 秒）防止執行緒中途崩潰導致死鎖。若鎖被佔用則進行自旋等待。
* **安全解鎖**：釋放鎖時必須傳入唯一的 `token`，並使用 **Lua 腳本** 進行原子比對後刪除：
  ```lua
  if redis.call('get', KEYS[1]) == ARGV[1] then 
      return redis.call('del', KEYS[1]) 
  else 
      return 0 
  end
  ```
  這能防止因前一個執行緒執行過久、鎖已過期自動釋放後，該執行緒不小心把「後續其他執行緒新取得的鎖」誤刪的嚴重 Bug。
* **成熟套件**：實務上亦可直接引入 `Redisson` 套件，利用其 `RLock`（內建 Watchdog 自動續租機制）實現如同 `java.util.concurrent.locks.Lock` 的阻塞與解鎖操作。

#### 3. 多環境連線適配
為確保本地、Docker 容器與雲端平台（如 Railway）的無縫部署，連線資訊應採用環境變數動態注入：
```properties
# 自動偵測雲端平台變數（如 Railway 的 REDISHOST 密碼認證）或退回本地 localhost
spring.data.redis.host=${REDISHOST:${SPRING_REDIS_HOST:${REDIS_HOST:localhost}}}
spring.data.redis.port=${REDISPORT:${SPRING_REDIS_PORT:${REDIS_PORT:6379}}}
spring.data.redis.password=${REDISPASSWORD:${SPRING_REDIS_PASSWORD:${REDIS_PASSWORD:}}}
```

---

## 開發環境
- Java 21
- Maven 3.9.6
- Spring Boot (Thymeleaf 整合)

---

## 如何執行

### 1. 啟動服務
於專案根目錄下執行以下指令：
```bash
./mvnw spring-boot:run
```

### 2. 存取網頁
- 計算機首頁：`http://localhost:8080`
- 抽獎轉盤頁面：`http://localhost:8080/lottery`

### 3. 執行單元測試
專案內含高併發抽獎與計算邏輯的單元測試。可執行：
```bash
./mvnw test
```
