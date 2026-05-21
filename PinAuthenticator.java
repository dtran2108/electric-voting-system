import java.util.HashMap;
import java.util.Map;

/**
 * PIN 碼認證實作類別
 * 實作 AuthInterface，並使用 Map 來管理使用者的認證憑證。
 */
public class PinAuthenticator implements AuthInterface {
    
    // 使用 Map 來儲存 voterId (Key) 與對應的 PIN 碼 (Value)
    private Map<String, String> pins;

    /**
     * 建構子：初始化 pin-map
     */
    public PinAuthenticator() {
        this.pins = new HashMap<>();
    }

    /**
     * 實作安全的使用者驗證邏輯
     */
    @Override
    public boolean authenticate(String voterId, String credential) {
        // 1. 安全性檢查：防止傳入 null 導致系統崩潰 (NullPointerException)
        if (voterId == null || credential == null) {
            return false;
        }
        
        // 2. 取得該投票者註冊的 PIN 碼
        String storedPin = pins.get(voterId);
        
        // 3. 檢查儲存的 PIN 碼是否存在，且與輸入的憑證是否完全相符
        return storedPin != null && storedPin.equals(credential);
    }

    /**
     * 輔助方法：供系統註冊或匯入使用者的 PIN 碼
     * (因為 Map 必須有資料才能驗證，這是實作上必要的擴充，提供給其他模組呼叫)
     * * @param voterId 使用者編號
     * @param pin 設定的 PIN 碼
     */
    public void registerCredential(String voterId, String pin) {
        // 確保不存入無效的空資料
        if (voterId != null && pin != null) {
            pins.put(voterId, pin);
        }
    }
    
}